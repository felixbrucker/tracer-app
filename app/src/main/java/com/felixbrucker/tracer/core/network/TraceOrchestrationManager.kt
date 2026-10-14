package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.data.TraceRepository
import com.felixbrucker.tracer.core.model.TraceType
import com.felixbrucker.tracer.core.util.TraceSummaryCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraceOrchestrationManager @Inject constructor(
    private val traceRepository: TraceRepository
) {
    private var traceJob: Job? = null

    fun startTracing(
        scope: CoroutineScope,
        target: String,
        traceType: TraceType,
        isAppInBackground: () -> Boolean,
        onUpdateNotification: (title: String, content: String) -> Unit,
        onPostSummaryNotification: (title: String, content: String) -> Unit,
        onFinish: () -> Unit
    ) {
        traceJob?.cancel()

        traceJob = scope.launch {
            when (traceType) {
                TraceType.PING -> {
                    traceRepository.runPingStream(target)
                        .onEach { pingResult ->
                            traceRepository.updatePingResult(pingResult)
                            val pingList = traceRepository.pingResults.value
                            val statusText = TraceSummaryCalculator.calculatePingStatus(pingResult, pingList)
                            onUpdateNotification("Ping $target", statusText)
                        }
                        .onCompletion {
                            if (isAppInBackground()) {
                                val pingList = traceRepository.pingResults.value
                                val finishedSummary = TraceSummaryCalculator.calculatePingFinalSummary(pingList)
                                onPostSummaryNotification("Ping Summary for $target", finishedSummary)
                            }
                            traceRepository.clearActiveTrace()
                            onFinish()
                        }
                        .catch { e ->
                            if (isAppInBackground()) {
                                onPostSummaryNotification("Ping Summary for $target", e.message ?: "Failed")
                            }
                            traceRepository.clearActiveTrace()
                            onFinish()
                        }
                        .launchIn(this)
                }
                TraceType.TRACEROUTE -> {
                    traceRepository.runTracerouteStream(target)
                        .onEach { hop ->
                            traceRepository.updateTracerouteHop(hop)
                            val status = TraceSummaryCalculator.calculateTracerouteStatus(hop)
                            onUpdateNotification("Traceroute $target", status)
                        }
                        .onCompletion {
                            if (isAppInBackground()) {
                                val hops = traceRepository.tracerouteHops.value
                                val finishedSummary = TraceSummaryCalculator.calculateTracerouteFinalSummary(hops)
                                onPostSummaryNotification("Traceroute Summary for $target", finishedSummary)
                            }
                            traceRepository.clearActiveTrace()
                            onFinish()
                        }
                        .catch { e ->
                            if (isAppInBackground()) {
                                onPostSummaryNotification("Traceroute Summary for $target", e.message ?: "Failed")
                            }
                            traceRepository.clearActiveTrace()
                            onFinish()
                        }
                        .launchIn(this)
                }
                TraceType.MTR -> {
                    traceRepository.runMtrStream(target)
                        .onEach { stats ->
                            traceRepository.updateMtrStats(stats)
                            val status = TraceSummaryCalculator.calculateMtrStatus(stats)
                            onUpdateNotification("MTR Live $target", status)
                        }
                        .onCompletion {
                            if (isAppInBackground()) {
                                val stats = traceRepository.mtrStats.value
                                val summaryText = TraceSummaryCalculator.calculateMtrFinalSummary(stats)
                                onPostSummaryNotification("MTR Summary for $target", summaryText)
                            }
                            traceRepository.clearActiveTrace()
                            onFinish()
                        }
                        .catch { e ->
                            if (isAppInBackground()) {
                                onPostSummaryNotification("MTR Summary for $target", e.message ?: "Failed")
                            }
                            traceRepository.clearActiveTrace()
                            onFinish()
                        }
                        .launchIn(this)
                }
            }
        }
    }

    fun stopTracing() {
        traceJob?.cancel()
        traceRepository.clearActiveTrace()
    }
}
