"""Account-free Android WebView regression fixture.
Run: python3 tests/android_login_fixture.py
Connect only the intended phone: adb -s PHONE reverse tcp:18765 tcp:18765
Open http://127.0.0.1:18765/ in Safeer and tap Direct, Delayed, POST.
Expected: three callbacks, intact query, POST proof=fixture%2Bbody, cookie=true,
login/frame preserved, adHidden=true, automaticBlocked=true; popups close.
No personal credentials are required. Stop the server and remove adb reverse after testing.
"""
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
import json
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*a): pass
 def do_GET(self): self.serve('GET','')
 def do_POST(self): self.serve('POST',self.rfile.read(int(self.headers.get('Content-Length',0))).decode())
 def serve(self,method,body):
  if self.path.startswith('/report'):
   print('REPORT',body,flush=True);self.send_response(204);self.end_headers();return
  self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.end_headers()
  if self.path.startswith('/callback'):
   result={'method':method,'body':body,'query':self.path.partition('?')[2],'cookie':'fixture=yes' in self.headers.get('Cookie','')}
   print('CALLBACK',json.dumps(result),flush=True)
   html='<script>if(window.opener){window.opener.postMessage('+json.dumps(result)+',location.origin);window.close();}else{document.write("FAIL missing opener")}</script>'
  else:
   html='''<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><title>Safeer login test</title>
   <style>body{font:18px sans-serif;padding:12px}button{font:18px sans-serif;padding:18px;margin:8px 0;display:block}iframe{height:45px;width:90%}</style>
   <h2>Safeer login test</h2><button onclick="window.open('/callback?state=fixture&utm_source=test&case=direct','direct')">Direct popup</button>
   <button onclick="let p=window.open('about:blank','delayed');setTimeout(()=>{if(p)p.location='/callback?state=fixture&utm_source=test&case=delayed'},100)">Delayed popup</button>
   <form method="POST" target="post" action="/callback?state=fixture&utm_source=test&case=post"><input type="hidden" name="proof" value="fixture+body"><button>POST popup</button></form>
   <div class="tp-modal" id="login"><form><label>Preserved login input<input placeholder="Fixture only"></label></form></div>
   <div style="position:relative;z-index:10001" id="challenge"><iframe srcdoc="<p>Verification frame OK</p>"></iframe></div>
   <div data-component="ad-slot" id="advert">Test ad space</div><pre id="results" style="white-space:pre-wrap;font-size:14px"></pre>
   <script>document.cookie='fixture=yes; path=/';var results=[];function report(){let ad=document.getElementById('advert');let r={results,login:!!document.getElementById('login'),frame:!!document.querySelector('#challenge iframe'),adHidden:!ad||ad.getBoundingClientRect().height===0,automaticBlocked:window.automaticBlocked};document.getElementById('results').textContent=JSON.stringify(r,null,1);fetch('/report',{method:'POST',body:JSON.stringify(r)})};addEventListener('message',e=>{if(e.origin===location.origin){results.push(e.data);report()}});setTimeout(()=>{window.automaticBlocked=open('/callback?case=unwanted','unwanted')===null;report()},5000);</script>'''
  self.wfile.write(html.encode())
print('Fixture ready on 18765',flush=True)
ThreadingHTTPServer(('127.0.0.1',18765),Handler).serve_forever()
