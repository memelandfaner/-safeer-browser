#!/usr/bin/env python3
"""Build/install a separate review app and instrumentation APK on an explicitly selected test device.
Requires a completed build-ci build, JDK 17+, and the local Android toolchain.
Usage: JAVA_HOME=/path/to/jdk python3 tests/run_android_lifecycle.py --device SERIAL
The installed browser and its data are untouched. Temporary review/test apps are removed afterward.
"""
import argparse
import http.server
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
REVIEW = 'com.safeer.mobile.browser.review'
TEST = 'com.safeer.browser.tests'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--device', required=True)
    parser.add_argument('--tools', type=Path, default=ROOT.parent / 'streamN-TV2/android_tv/.tools')
    parser.add_argument('--keep', action='store_true', help='Keep isolated test installations for inspection')
    args = parser.parse_args()
    java_home = Path(os.environ['JAVA_HOME'])
    java, javac = java_home / 'bin/java', java_home / 'bin/javac'
    tools = args.tools.resolve()
    android_jar = tools / 'android.jar'
    build = ROOT / 'build-ci'
    if not list((build / 'dex').glob('*.dex')):
        raise SystemExit('Complete tools/gradnja-ci.sh before running device tests')

    def run(command, **kwargs):
        return subprocess.run([str(x) for x in command], check=True, text=True, capture_output=True, **kwargs)

    def adb(*command):
        return run(['adb', '-s', args.device, *command])

    # Refuse to overwrite anyone's existing installation of these test package IDs.
    for package in (REVIEW, TEST):
        if ('package:' + package) in adb('shell', 'pm', 'list', 'packages', package).stdout.splitlines():
            raise SystemExit(f'Test package already exists: {package}; remove it explicitly before rerunning')

    class Fixture(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            title = 'First fixture' if self.path.startswith('/first') else 'Second fixture'
            body = f'<html><head><title>{title}</title></head><body><h1>{title}</h1><input placeholder="Test draft"></body></html>'.encode()
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        def log_message(self, *args):
            pass

    server = http.server.ThreadingHTTPServer(('127.0.0.1', 18769), Fixture)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    installed = []
    try:
        with tempfile.TemporaryDirectory(prefix='safeer-device-review-') as temp:
            temp = Path(temp)
            manifest = ET.parse(ROOT / 'AndroidManifest.xml')
            manifest.getroot().set('package', REVIEW)
            application = manifest.getroot().find('application')
            application.set(ANDROID + 'label', 'Safeer preizkus')
            application.set(ANDROID + 'debuggable', 'true')
            application.find('provider').set(ANDROID + 'authorities', REVIEW + '.fileprovider')
            review_manifest = temp / 'AndroidManifest.xml'
            manifest.write(review_manifest, encoding='utf-8', xml_declaration=True)
            review_apk = temp / 'review.apk'
            run([tools / 'aapt2', 'link', '-I', android_jar, '--manifest', review_manifest,
                 '--rename-manifest-package', REVIEW, '-A', ROOT / 'assets', '-o', review_apk, build / 'compiled_res.zip'])
            with zipfile.ZipFile(review_apk, 'a') as out:
                for dex in (build / 'dex').glob('*.dex'):
                    out.write(dex, dex.name)

            test_manifest = temp / 'TestManifest.xml'
            test_manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{TEST}">
<uses-sdk android:minSdkVersion="28" android:targetSdkVersion="36"/>
<application android:label="Safeer lifecycle tests"/>
<instrumentation android:name="{TEST}.BrowserLifecycleInstrumentation" android:targetPackage="{REVIEW}"/>
</manifest>''')
            classes = temp / 'classes'
            classes.mkdir()
            classpath = os.pathsep.join(map(str, [android_jar, build / 'classes', tools / 'kotlinc/lib/kotlin-stdlib.jar']))
            run([javac, '-source', '8', '-target', '8', '-cp', classpath, '-d', classes,
                 ROOT / 'tests/android/BrowserLifecycleInstrumentation.java'])
            dexdir = temp / 'dex'
            dexdir.mkdir()
            run([java, '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--min-api', '28',
                 '--lib', android_jar, '--output', dexdir, *classes.rglob('*.class')])
            test_apk = temp / 'tests.apk'
            run([tools / 'aapt2', 'link', '-I', android_jar, '--manifest', test_manifest, '-o', test_apk])
            with zipfile.ZipFile(test_apk, 'a') as out:
                out.write(dexdir / 'classes.dex', 'classes.dex')
            signed = temp / 'signed'
            for apk in (review_apk, test_apk):
                run([java, '-jar', tools / 'uber-apk-signer.jar', '--apks', apk, '--out', signed])
            adb('reverse', 'tcp:18769', 'tcp:18769')
            for name, package in (('review', REVIEW), ('tests', TEST)):
                # uber-apk-signer: '<ime>-aligned-debugSigned.apk' (velika S)
                apk = next(p for p in sorted(signed.glob(name + '*.apk'))
                           if 'signed' in p.name.lower())
                adb('install', '-r', str(apk))
                installed.append(package)
            adb('shell', 'pm', 'grant', REVIEW, 'android.permission.POST_NOTIFICATIONS')
            result = adb('shell', 'am', 'instrument', '-w', TEST + '/.BrowserLifecycleInstrumentation')
            print(result.stdout, flush=True)
            log = ROOT / 'build-ci/android-lifecycle-results.txt'
            log.write_text(result.stdout + result.stderr)
            # This device prints only the report stream, without INSTRUMENTATION_CODE.
            if 'FAIL:' in result.stdout or 'DONE:' not in result.stdout:
                raise SystemExit('Device lifecycle tests failed; see ' + str(log))
    except subprocess.CalledProcessError as exc:
        print(exc.stdout or '')
        print(exc.stderr or '')
        raise
    finally:
        if not args.keep:
            for package in reversed(installed):
                try: adb('uninstall', package)
                except subprocess.CalledProcessError: pass
        try: adb('reverse', '--remove', 'tcp:18769')
        except subprocess.CalledProcessError: pass
        server.shutdown()
        server.server_close()


if __name__ == '__main__':
    main()
