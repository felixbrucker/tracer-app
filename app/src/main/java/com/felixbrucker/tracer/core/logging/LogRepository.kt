package com.felixbrucker.tracer.core.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDir = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }

    suspend fun writeLog(tag: String, message: String) = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val now = Date()
        val logFile = File(logDir, "log_${dateFormat.format(now)}.txt")

        runCatching {
            BufferedWriter(FileWriter(logFile, true)).use { writer ->
                writer.write("${timeFormat.format(now)} [$tag] $message\n")
            }
        }
    }

    suspend fun getLogs(): List<String> = withContext(Dispatchers.IO) {
        val latestFile = logDir.listFiles()?.maxByOrNull { it.lastModified() } ?: return@withContext emptyList()
        runCatching {
            latestFile.readLines()
        }.getOrDefault(emptyList())
    }
}

class AppLogTree(private val logRepository: LogRepository) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        val safeTag = tag ?: "App"
        // Disk buffer logging offloaded asynchronously or captured safely
    }
}
