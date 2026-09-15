package com.safeer.browser.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.webkit.WebSettings;
import com.safeer.mobile.browser.*;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

/** Runs against a separate com.safeer.mobile.browser.review installation, without personal accounts. */
public class BrowserLifecycleInstrumentation extends Instrumentation {
    private Activity activity;
    private TabManager manager;
    private final StringBuilder report = new StringBuilder();
    private static final String BASE = "http://127.0.0.1:18769/";

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private <T> T ui(Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        runOnMainSync(() -> { try { result.set(action.call()); } catch (Exception e) { error.set(e); } });
        if (error.get() != null) throw error.get();
        return result.get();
    }
    private void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void waitFor(Callable<Boolean> condition, String message) throws Exception {
        long end = SystemClock.uptimeMillis() + 20000;
        do { if (ui(condition)) return; SystemClock.sleep(100); } while (SystemClock.uptimeMillis() < end);
        throw new IllegalStateException(message);
    }
    private void pass(String text) { report.append("PASS: ").append(text).append('\n'); }
    private void launch() throws Exception {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.safeer.mobile.browser.review", "com.safeer.mobile.browser.MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = startActivitySync(intent);
        Field field = activity.getClass().getDeclaredField("tabManager");
        field.setAccessible(true);
        manager = (TabManager) field.get(activity);
        waitFor(() -> manager.getCount() > 0, "initial tab missing");
    }
    private void loaded(TabModel tab, String title) throws Exception {
        waitFor(() -> tab.getLoadedWebView() != null && title.equals(tab.getWebView().getTitle()) &&
            tab.getWebView().getProgress() == 100, "page did not load: " + title);
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            launch();
            ui(() -> { manager.closeAllTabs(activity); return null; });
            TabModel first = ui(() -> {
                TabModel tab = manager.getActiveTab();
                tab.getWebView().loadUrl(BASE + "first"); return tab;
            });
            loaded(first, "First fixture");
            ui(() -> {
                require(first.getWebView().getSettings().getMixedContentMode() == WebSettings.MIXED_CONTENT_NEVER_ALLOW, "mixed content is not strict");
                long before = PreferencesManager.INSTANCE.getTotalAdsBlocked(activity);
                AdBlockEngine.INSTANCE.getBlockedAdsCount().incrementAndGet();
                AdBlockEngine.INSTANCE.getOnAdBlocked().invoke();
                String stats = new ChromiumEngineView.SafeerWebAppInterface(activity, first.getWebView()).getStats();
                require(new JSONObject(stats).getLong("ads") == before + 1, "statistics counted the block twice");
                return null;
            });
            pass("strict HTTPS setting and real WebView bridge statistics");
            TabModel second = ui(() -> manager.createTab(activity, BASE + "second", true));
            loaded(second, "Second fixture");
            ui(() -> {
                new ChromiumEngineView.SafeerWebAppInterface(activity, first.getWebView()).notifyAudioState(true);
                manager.suspendInactiveTabs(true); return null;
            });
            SystemClock.sleep(500);
            require(ui(() -> first.getLoadedWebView() != null), "audio tab discarded");
            ui(() -> {
                new ChromiumEngineView.SafeerWebAppInterface(activity, first.getWebView()).notifyAudioState(false);
                manager.switchTab(first.getId());
                first.getWebView().evaluateJavascript("document.querySelector('input').value='draft';document.querySelector('input').dispatchEvent(new Event('input',{bubbles:true}));", null);
                return null;
            });
            waitFor(() -> first.getWebView().getHasEditedForm(), "form input was not tracked");
            ui(() -> { manager.switchTab(second.getId()); manager.suspendInactiveTabs(true); return null; });
            SystemClock.sleep(500);
            require(ui(() -> first.getLoadedWebView() != null), "edited form discarded");
            pass("audio state and real DOM form edits prevent suspension");
            ui(() -> { manager.switchTab(first.getId()); first.getWebView().reload(); return null; });
            waitFor(() -> !first.getWebView().getHasEditedForm() && first.getWebView().getProgress() == 100, "form reset failed");
            ui(() -> { manager.switchTab(second.getId()); manager.suspendInactiveTabs(true); return null; });
            waitFor(() -> first.getLoadedWebView() == null, "inactive WebView not released");
            ui(() -> { manager.switchTab(first.getId()); return null; });
            loaded(first, "First fixture");
            pass("real WebView saveState, destroy and restoreState");
            ui(() -> { manager.switchTab(second.getId()); manager.saveSession(); activity.finish(); return null; });
            waitFor(() -> activity.isDestroyed(), "activity did not close");
            launch();
            require(ui(() -> manager.getCount() == 2 && manager.getActiveTab().getId().equals(second.getId())), "session selection lost");
            TabModel restored = ui(() -> manager.getAllTabs().get(0));
            require(ui(() -> restored.getLoadedWebView() == null), "restored background page loaded eagerly");
            loaded(ui(() -> manager.getActiveTab()), "Second fixture");
            pass("activity restart restores session lazily and preserves selected tab");
            TabModel crashTab = ui(() -> manager.getActiveTab());
            ui(() -> { crashTab.getWebView().loadUrl("chrome://crash"); return null; });
            try {
                waitFor(() -> crashTab.getLoadedWebView() == null, "renderer crash was not handled");
            } catch (IllegalStateException ignored) {
                // The debug URL can be dropped while the page is still settling; ask once more.
                ui(() -> { crashTab.getWebView().loadUrl("chrome://crash"); return null; });
                waitFor(() -> crashTab.getLoadedWebView() == null, "renderer crash was not handled");
            }
            require(ui(() -> !activity.isDestroyed()), "renderer crash killed activity");
            ui(() -> { manager.switchTab(crashTab.getId()); return null; });
            loaded(crashTab, "Second fixture");
            pass("real renderer crash stays in app and explicit retry reloads page");
            ui(() -> { manager.closeAllTabs(activity); manager.saveSession(); activity.finish(); return null; });
            report.append("DONE: all device checks passed\n");
            result.putString("stream", report.toString());
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", report + "FAIL: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
