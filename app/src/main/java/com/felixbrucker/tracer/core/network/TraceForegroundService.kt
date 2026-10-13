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
        traceJob?.cancel()

        when (traceType) {
            TraceType.PING -> {
                traceJob = traceRepository.runPingStream(target)
                    .onEach { pingResult ->
                        traceRepository.updatePingResult(pingResult)
                        val pingList = traceRepository.pingResults.value
                        val sent = pingList.size
                        val recv = pingList.count { it.isSuccess }
                        val lost = sent - recv
                        val lossPct = if (sent == 0) 0.0 else (lost.toDouble() / sent.toDouble()) * 100.0
                        val statusText = if (pingResult.isSuccess) {
                            "Seq ${pingResult.sequence}: ${pingResult.rttMs?.let { "%.1f".format(it) }} ms | Recv: $recv/$sent (Loss: ${"%.0f".format(lossPct)}%)"
                        } else {
                            "Seq ${pingResult.sequence}: Timed out | Recv: $recv/$sent (Loss: ${"%.0f".format(lossPct)}%)"
                        }
                        updateOngoingNotification("Ping $target", statusText)
                    }
                    .onCompletion {
                        val pingList = traceRepository.pingResults.value
                        val sent = pingList.size
                        val recv = pingList.count { it.isSuccess }
                        val lost = sent - recv
                        val lossPct = if (sent == 0) 0.0 else (lost.toDouble() / sent.toDouble()) * 100.0
                        val rtts = pingList.mapNotNull { it.rttMs }
                        val avgRtt = if (rtts.isNotEmpty()) rtts.average() else 0.0
                        val minRtt = rtts.minOrNull() ?: 0.0
                        val maxRtt = rtts.maxOrNull() ?: 0.0

                        val finishedSummary = "Sent: $sent, Recv: $recv, Lost: $lost (${"%.0f".format(lossPct)}% loss) | RTT Min/Avg/Max: ${"%.1f".format(minRtt)}/${"%.1f".format(avgRtt)}/${"%.1f".format(maxRtt)} ms"
                        postSummaryNotification("Ping Summary for $target", finishedSummary)
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    .catch { e ->
                        postSummaryNotification("Ping Summary for $target", e.message ?: "Failed")
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    .launchIn(serviceScope)
            }
            TraceType.TRACEROUTE -> {
                traceJob = traceRepository.runTracerouteStream(target)
                    .onEach { hop ->
                        traceRepository.updateTracerouteHop(hop)
                        val status = "Hop ${hop.hopNumber}: ${hop.ipAddress} (${if (hop.isTimeout) "Timeout" else "${hop.rttMs?.let { "%.1f".format(it) }} ms"})"
                        updateOngoingNotification("Traceroute $target", status)
                    }
                    .onCompletion {
                        val hops = traceRepository.tracerouteHops.value
                        val reached = hops.lastOrNull()?.isFinalDestination == true
                        val rtts = hops.mapNotNull { it.rttMs }
                        val avgRtt = if (rtts.isNotEmpty()) rtts.average() else 0.0
                        val destHop = hops.lastOrNull()

                        val finishedSummary = if (reached && destHop != null) {
                            "Destination reached in ${hops.size} hops | Final RTT: ${"%.1f".format(destHop.rttMs ?: 0.0)} ms | Path Avg RTT: ${"%.1f".format(avgRtt)} ms"
                        } else {
                            "Stopped after ${hops.size} hops | Path Avg RTT: ${"%.1f".format(avgRtt)} ms"
                        }
                        postSummaryNotification("Traceroute Summary for $target", finishedSummary)
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    .catch { e ->
                        postSummaryNotification("Traceroute Summary for $target", e.message ?: "Failed")
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    .launchIn(serviceScope)
            }
            TraceType.MTR -> {
                traceJob = traceRepository.runWinMtrStream(target)
                    .onEach { stats ->
                        traceRepository.updateWinMtrStats(stats)
                        val destHop = stats.lastOrNull { it.sent > 0 }
                        val status = if (destHop != null) {
                            "Hops: ${stats.size} | Loss: ${"%.0f".format(destHop.lossPercentage)}% | Last RTT: ${"%.1f".format(destHop.lastRttMs)} ms"
                        } else {
                            "Tracing path to $target..."
                        }
                        updateOngoingNotification("MTR Live $target", status)
                    }
                    .onCompletion {
                        val stats = traceRepository.winMtrStats.value
                        val destHop = stats.lastOrNull { it.sent > 0 }
                        val summaryText = if (destHop != null) {
                            "Hops: ${stats.size} | Probes: ${destHop.sent} | Final Loss: ${"%.0f".format(destHop.lossPercentage)}% | Avg RTT: ${"%.1f".format(destHop.avgRttMs)} ms | Best/Worst: ${"%.0f".format(destHop.bestRttMs)}/${"%.0f".format(destHop.worstRttMs)} ms"
                        } else {
                            "Session ended after tracing ${stats.size} hops"
                        }
                        postSummaryNotification("MTR Summary for $target", summaryText)
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    .catch { e ->
                        postSummaryNotification("MTR Summary for $target", e.message ?: "Failed")
                        traceRepository.clearActiveTrace()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    .launchIn(serviceScope)
            }
        }
    }

    private fun updateOngoingNotification(title: String, content: String) {
        val notification = buildOngoingNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val uniqueSummaryId = System.currentTimeMillis().toInt()
        manager.notify(uniqueSummaryId, notification)
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
        const val ONGOING_NOTIFICATION_ID = 1001
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
