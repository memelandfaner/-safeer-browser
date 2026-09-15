package com.safeer.mobile.browser.cast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.safeer.mobile.browser.R

/**
 * Safeer Link na telefonu: gostitelj tece v storitvi, ne v dejavnosti brskalnika.
 *
 * Doslej je znal biti sredisce samo televizor, zato brez prizganega televizorja ni bilo
 * mogoce poslati nicesar nikamor. Zdaj lahko gosti tudi telefon - takrat se nanj povezejo
 * televizor in racunalniki.
 *
 * Razlika od televizorja je namerna: televizor je naprava, ki stoji v dnevni sobi in naj bo
 * dosegljiva tudi po ponovnem vklopu, zato se tam Link zazene ob zagonu naprave. Telefon je
 * v zepu in ima baterijo, zato tu ni zagona ob vklopu: Hub tece, dokler ga uporabnik ne
 * ugasne v Safeer Linku, obvestilo pa ves cas pove, da tece.
 */
class HubStoritev : Service() {

    companion object {
        private const val TAG = "SafeerHubStoritev"
        private const val KANAL = "safeer_link_hub"
        private const val OBVESTILO = 4142

        const val AKCIJA_ZACNI = "com.safeer.mobile.browser.cast.HUB_ZACNI"
        const val AKCIJA_KONCAJ = "com.safeer.mobile.browser.cast.HUB_KONCAJ"

        /** Uporabnik je Safeer Link prizgal: Hub zazenemo takoj, storitev ga drzi pri zivljenju. */
        fun vklopi(context: Context): Boolean {
            val app = context.applicationContext
            val uspelo = HubKrmilnik.zazeni(app, zapomni = true)
            if (uspelo) zazeniStoritev(app, AKCIJA_ZACNI)
            return uspelo
        }

        /** Uporabnik je Safeer Link ugasnil. */
        fun izklopi(context: Context) {
            val app = context.applicationContext
            HubKrmilnik.ustavi(app, zapomni = true)
            zazeniStoritev(app, AKCIJA_KONCAJ)
        }

        /** Ob zagonu brskalnika: ce je uporabnik Link pustil prizgan, naj spet tece. */
        fun zagotovi(context: Context) {
            val app = context.applicationContext
            if (!HubKrmilnik.jeZazelen(app)) return
            zazeniStoritev(app, AKCIJA_ZACNI)
        }

        private fun zazeniStoritev(app: Context, akcija: String) {
            val namera = Intent(app, HubStoritev::class.java).apply { action = akcija }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(namera)
                else app.startService(namera)
            } catch (e: Throwable) {
                // Android zavrne zagon storitve iz ozadja; Hub takrat tece, dokler zivi brskalnik.
                Log.w(TAG, "Storitve ni bilo mogoce zagnati: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AKCIJA_KONCAJ) {
            HubKrmilnik.ustavi(applicationContext, zapomni = false)
            ustaviOspredje()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!HubKrmilnik.jeZazelen(applicationContext)) {
            ustaviOspredje()
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            pripraviKanal()
            startForeground(OBVESTILO, obvestilo())
        } catch (e: Throwable) {
            Log.w(TAG, "Obvestila ni bilo mogoce prikazati: ${e.message}")
        }
        if (!HubKrmilnik.tece() && !HubKrmilnik.zazeni(applicationContext, zapomni = false)) {
            Log.w(TAG, "Huba ni bilo mogoce zagnati; storitev koncujem.")
            ustaviOspredje()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        HubKrmilnik.ustavi(applicationContext, zapomni = false)
        super.onDestroy()
    }

    private fun ustaviOspredje() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
            else @Suppress("DEPRECATION") stopForeground(true)
        } catch (_: Throwable) {
        }
    }

    private fun pripraviKanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val upravitelj = getSystemService(NotificationManager::class.java) ?: return
        if (upravitelj.getNotificationChannel(KANAL) != null) return
        val kanal = NotificationChannel(
            KANAL,
            getString(R.string.hub_obvestilo_naslov),
            NotificationManager.IMPORTANCE_LOW
        )
        kanal.description = getString(R.string.hub_obvestilo_besedilo)
        kanal.setShowBadge(false)
        upravitelj.createNotificationChannel(kanal)
    }

    private fun obvestilo(): Notification {
        val gradnik = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, KANAL)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return gradnik
            .setContentTitle(getString(R.string.hub_obvestilo_naslov))
            .setContentText(getString(R.string.hub_obvestilo_besedilo))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
    }
}
