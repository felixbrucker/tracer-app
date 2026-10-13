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
import kotlinx.coroutines.flow.onCompletion
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
        val initialNotification = buildNotification("Starting $traceType to $target", "Initializing...", isFinished = false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
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
                        val pingList = traceRepository.pingResults.value
                        val sent = pingList.size
                        val recv = pingList.count { it.isSuccess }
                        val statusText = if (pingResult.isSuccess) {
                            "Online • Seq ${pingResult.sequence}: ${pingResult.rttMs?.let { "%.1f".format(it) }} ms (Recv $recv/$sent)"
                        } else {
                            "Unreachable • Seq ${pingResult.sequence}: Timed out (Recv $recv/$sent)"
                        }
                        updateNotification("Ping $target", statusText, isFinished = false)
                    }
                    .onCompletion {
                        val pingList = traceRepository.pingResults.value
                        val sent = pingList.size
                        val recv = pingList.count { it.isSuccess }
                        val lost = sent - recv
                        val lossPct = if (sent == 0) 0.0 else (lost.toDouble() / sent.toDouble()) * 100.0
                        val avgRtt = pingList.mapNotNull { it.rttMs }.let { if (it.isNotEmpty()) it.average() else 0.0 }
                        val isOnline = pingList.lastOrNull()?.isSuccess == true

                        val finishedSummary = "Target ${if (isOnline) "Online" else "Offline"} • Sent: $sent, Recv: $recv, Loss: ${"%.0f".format(lossPct)}%, Avg RTT: ${"%.1f".format(avgRtt)} ms"
                        updateNotification("Ping Finished for $target", finishedSummary, isFinished = true)
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                    .catch { e ->
                        updateNotification("Ping $target Error", e.message ?: "Failed", isFinished = true)
                        traceRepository.clearActiveTrace()
                    }
                    .launchIn(serviceScope)
            }
            TraceType.TRACEROUTE -> {
                traceJob = traceRepository.runTracerouteStream(target)
                    .onEach { hop ->
                        traceRepository.updateTracerouteHop(hop)
                        val status = "Hop ${hop.hopNumber}: ${hop.ipAddress} (${if (hop.isTimeout) "Timeout" else "${hop.rttMs?.let { "%.1f".format(it) }} ms"})"
                        updateNotification("Traceroute $target", status, isFinished = false)
                    }
                    .onCompletion {
                        val hops = traceRepository.tracerouteHops.value
                        val reached = hops.lastOrNull()?.isFinalDestination == true
                        val finishedSummary = if (reached) {
                            "Traceroute Completed to $target in ${hops.size} hops (${hops.lastOrNull()?.ipAddress})"
                        } else {
                            "Traceroute Stopped after ${hops.size} hops"
                        }
                        updateNotification("Traceroute Finished", finishedSummary, isFinished = true)
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                    .catch { e ->
                        updateNotification("Traceroute $target Error", e.message ?: "Failed", isFinished = true)
                        traceRepository.clearActiveTrace()
                    }
                    .launchIn(serviceScope)
            }
            TraceType.MTR -> {
                traceJob = traceRepository.runWinMtrStream(target)
                    .onEach { stats ->
                        traceRepository.updateWinMtrStats(stats)
                        val destHop = stats.lastOrNull { it.sent > 0 }
                        val isOnline = destHop != null && destHop.received > 0
                        val status = if (destHop != null) {
                            "Target ${if (isOnline) "Online" else "Unreachable"} • Hops: ${stats.size} | Loss: ${"%.0f".format(destHop.lossPercentage)}% | Avg: ${"%.1f".format(destHop.avgRttMs)} ms"
                        } else {
                            "Tracing path to $target..."
                        }
                        updateNotification("MTR Live $target", status, isFinished = false)
                    }
                    .onCompletion {
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                    .catch { e ->
                        updateNotification("MTR $target Error", e.message ?: "Failed", isFinished = true)
                        traceRepository.clearActiveTrace()
                    }
                    .launchIn(serviceScope)
            }
        }
    }

    private fun updateNotification(title: String, content: String, isFinished: Boolean) {
        val notification = buildNotification(title, content, isFinished)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String, isFinished: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)

        if (!isFinished) {
            val stopIntent = Intent(this, TraceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
        } else {
            builder.setOngoing(false)
                .setAutoCancel(true)
        }

        return builder.build()
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
