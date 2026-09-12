package com.safeer.mobile.browser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/**
 * 🎵 Predvajanje v ozadju: ko uporabnik med predvajanjem (YouTube, YouTube Music, radio ...) zapusti brskalnik,
 * ta storitev v ospredju drži proces pri življenju (Android in One UI sicer aplikacijo brez take storitve po
 * nekaj sekundah zamrzneta in zvok utihne), pokaže obvestilo z gumbom za premor/nadaljevanje ter drži CPU in
 * Wi-Fi budna, dokler zvok teče. Zvočnega fokusa NE zahteva: drži ga WebView (Chromium) sam in bi ob izgubi
 * predvajanje ustavil. Ustavi se, ko se brskalnik vrne v ospredje ali predvajanje
 * neha. Stran sama ostane v WebViewu (nič se ne pretaka mimo brskalnika).
 */
class MediaPlaybackService : Service() {
    companion object {
        private const val CHANNEL_ID = "safeer_media_playback"
        private const val NOTIFICATION_ID = 4101
        const val ACTION_START = "com.safeer.mobile.browser.action.MEDIA_START"
        const val ACTION_TOGGLE = "com.safeer.mobile.browser.action.MEDIA_TOGGLE"
        const val ACTION_STOP = "com.safeer.mobile.browser.action.MEDIA_STOP"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_PLAYING = "playing"
        private const val MAX_WAKE_MS = 6L * 3600L * 1000L

        /** Set by MainActivity: pauses or resumes the media element of the tab that plays. */
        @Volatile var toggleHandler: (() -> Unit)? = null
        /** Set by MainActivity: pauses the media element (the Stop button of the notification). */
        @Volatile var pauseHandler: (() -> Unit)? = null
        @Volatile var running = false
            private set

        fun start(context: Context, title: String, playing: Boolean = true) {
            val intent = Intent(context, MediaPlaybackService::class.java).setAction(ACTION_START)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_PLAYING, playing)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.w("SafeerMedia", "Storitve za predvajanje v ozadju ni mogoče zagnati: ${e.message}")
            }
        }

        fun stop(context: Context) {
            if (!running) return
            try { context.stopService(Intent(context, MediaPlaybackService::class.java)) } catch (_: Exception) {}
        }
    }

    private var session: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var title: String = ""
    private var playing = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { pauseHandler?.invoke(); stopSelf(); return START_NOT_STICKY }
            ACTION_TOGGLE -> { toggleHandler?.invoke(); return START_STICKY }
        }
        title = intent?.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: I18n.t(this, "media_background_title", "Safeer")
        playing = intent?.getBooleanExtra(EXTRA_PLAYING, true) ?: true
        ensureChannel()
        ensureSession()
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.w("SafeerMedia", "startForeground ni uspel: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        if (playing) acquireLocks() else releaseLocks()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        releaseLocks()
        try { session?.isActive = false; session?.release() } catch (_: Exception) {}
        session = null
        super.onDestroy()
    }

    // -- notification -------------------------------------------------------------------------------
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, I18n.t(this, "media_background_channel", "Background playback"),
            NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        channel.setSound(null, null)
        manager.createNotificationChannel(channel)
    }

    private fun pending(action: String, code: Int): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(this, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        builder.setSmallIcon(R.drawable.ic_media_notification)
            .setContentTitle(title)
            .setContentText(I18n.t(this, if (playing) "media_background_playing" else "media_background_paused", if (playing) "Playing in the background" else "Paused"))
            .setContentIntent(open)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
        val toggleLabel = I18n.t(this, if (playing) "media_background_pause" else "media_background_resume", if (playing) "Pause" else "Resume")
        val toggleIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        builder.addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, toggleIcon), toggleLabel, pending(ACTION_TOGGLE, 2)).build())
        builder.addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            I18n.t(this, "media_background_stop", "Stop"), pending(ACTION_STOP, 3)).build())
        session?.let { builder.style = Notification.MediaStyle().setMediaSession(it.sessionToken).setShowActionsInCompactView(0) }
        return builder.build()
    }

    // -- media session, audio focus, locks -------------------------------------------------------------
    private fun ensureSession() {
        val current = session ?: MediaSession(this, "SafeerBrowser").also { created ->
            created.setCallback(object : MediaSession.Callback() {
                override fun onPlay() { toggleHandler?.invoke() }
                override fun onPause() { toggleHandler?.invoke() }
                override fun onStop() { pauseHandler?.invoke(); stopSelf() }
            })
            session = created
        }
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        current.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP)
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, if (playing) 1f else 0f).build())
        current.setMetadata(android.media.MediaMetadata.Builder().putString(android.media.MediaMetadata.METADATA_KEY_TITLE, title).build())
        current.isActive = true
    }

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val power = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Safeer:mediaPlayback").apply { setReferenceCounted(false) }
            }
            wakeLock?.acquire(MAX_WAKE_MS)
            if (wifiLock == null) {
                val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Safeer:mediaPlayback").apply { setReferenceCounted(false) }
            }
            wifiLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
        try { if (wifiLock?.isHeld == true) wifiLock?.release() } catch (_: Exception) {}
    }
}
