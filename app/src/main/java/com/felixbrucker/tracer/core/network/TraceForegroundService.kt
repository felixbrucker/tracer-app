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
import com.felixbrucker.tracer.core.data.TraceRepository
import com.felixbrucker.tracer.core.model.TraceType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class TraceForegroundService : Service() {

    @Inject
    lateinit var traceRepository: TraceRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var traceJob: Job? = null

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
        val initialNotification = buildNotification("Starting $traceType to $target", "Initializing...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startTracing(target, traceType)

        return START_STICKY
    }

    private fun startTracing(target: String, traceType: TraceType) {
        traceJob?.cancel()

        when (traceType) {
            TraceType.PING -> {
                traceJob = traceRepository.runPingStream(target)
                    .onEach { pingResult ->
                        traceRepository.updatePingResult(pingResult)
                        val status = if (pingResult.isSuccess) {
                            "Seq ${pingResult.sequence}: ${pingResult.rttMs} ms"
                        } else {
                            "Seq ${pingResult.sequence}: Request timed out"
                        }
                        updateNotification("Ping $target", status)
                    }
                    .catch { e ->
                        updateNotification("Ping $target Error", e.message ?: "Failed")
                    }
                    .launchIn(serviceScope)
            }
            TraceType.TRACEROUTE -> {
                traceJob = traceRepository.runTracerouteStream(target)
                    .onEach { hop ->
                        traceRepository.updateTracerouteHop(hop)
                        val status = "Hop ${hop.hopNumber}: ${hop.ipAddress} (${hop.rttMs ?: "*"} ms)"
                        updateNotification("Traceroute $target", status)
                    }
                    .catch { e ->
                        updateNotification("Traceroute $target Error", e.message ?: "Failed")
                    }
                    .launchIn(serviceScope)
            }
            TraceType.WINMTR -> {
                traceJob = traceRepository.runWinMtrStream(target)
                    .onEach { stats ->
                        traceRepository.updateWinMtrStats(stats)
                        val lastHop = stats.lastOrNull { it.sent > 0 }
                        val status = if (lastHop != null) {
                            "Hops: ${stats.size} | Hop ${lastHop.hopNumber} (${lastHop.ipAddress}) Loss: ${"%.1f".format(lastHop.lossPercentage)}%"
                        } else {
                            "Tracing path to $target..."
                        }
                        updateNotification("WinMTR Live $target", status)
                    }
                    .catch { e ->
                        updateNotification("WinMTR $target Error", e.message ?: "Failed")
                    }
                    .launchIn(serviceScope)
            }
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trace Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopTracingService() {
        traceJob?.cancel()
        traceRepository.clearActiveTrace()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "tracer_service_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_TYPE = "extra_type"
        const val ACTION_STOP = "action_stop"

        fun start(context: Context, target: String, traceType: TraceType) {
            val intent = Intent(context, TraceForegroundService::class.java).apply {
                putExtra(EXTRA_TARGET, target)
                putExtra(EXTRA_TYPE, traceType.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TraceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
