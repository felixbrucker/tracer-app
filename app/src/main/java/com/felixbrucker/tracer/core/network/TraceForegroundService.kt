package com.felixbrucker.tracer.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.felixbrucker.tracer.MainActivity
import com.felixbrucker.tracer.MainApplication
import com.felixbrucker.tracer.R
import com.felixbrucker.tracer.core.model.TraceType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class TraceForegroundService : Service() {

    @Inject
    lateinit var traceOrchestrationManager: TraceOrchestrationManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopTracingService()
            return START_NOT_STICKY
        }

        val target = intent?.getStringExtra(EXTRA_TARGET) ?: return START_NOT_STICKY
        val typeName = intent.getStringExtra(EXTRA_TYPE) ?: TraceType.PING.name
        val traceType = TraceType.valueOf(typeName)

        createNotificationChannel()
        val initialNotification = buildOngoingNotification("Starting $traceType to $target", "Initializing...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, initialNotification)
        }

        startTracing(target, traceType)

        return START_STICKY
    }

    private fun startTracing(target: String, traceType: TraceType) {
        traceOrchestrationManager.startTracing(
            scope = serviceScope,
            target = target,
            traceType = traceType,
            isAppInBackground = { MainApplication.isAppInBackground },
            onUpdateNotification = { title, content -> updateOngoingNotification(title, content) },
            onPostSummaryNotification = { title, content -> postSummaryNotification(title, content) },
            onFinish = {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        )
    }

    private fun stopTracingService() {
        traceOrchestrationManager.stopTracing()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateOngoingNotification(title: String, content: String) {
        val notification = buildOngoingNotification(title, content)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun buildOngoingNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TraceForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_notification_bolt)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun postSummaryNotification(title: String, content: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_notification_checkmark)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val uniqueSummaryId = System.currentTimeMillis().toInt()
        manager.notify(uniqueSummaryId, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trace Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "tracer_service_channel"
        const val ONGOING_NOTIFICATION_ID = 1001
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_TYPE = "extra_type"
        const val ACTION_STOP = "action_stop"

        fun start(context: Context, target: String, traceType: TraceType) {
            val intent = Intent(context, TraceForegroundService::class.java).apply {
                putExtra(EXTRA_TARGET, target)
                putExtra(EXTRA_TYPE, traceType.name)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TraceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
