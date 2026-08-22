package com.prsnl.core.log

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val CRASH_FILE_NAME = "latest_crash.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(context, throwable, "Uncaught Exception on Thread [${thread.name}]")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash log", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logNonFatal(context: Context, throwable: Throwable, tag: String = "NonFatal") {
        try {
            saveCrashLog(context, throwable, "Non-Fatal Error [$tag]")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log non-fatal exception", e)
        }
    }

    private fun saveCrashLog(context: Context, throwable: Throwable, title: String) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logContent = """
            ========================================
            PRSNL CRASH LOG - $timeStamp
            $title
            Exception: ${throwable.javaClass.name}
            Message: ${throwable.message ?: "No message"}
            ========================================
            
            STACK TRACE:
            $stackTrace
        """.trimIndent()

        val logFile = File(context.filesDir, CRASH_FILE_NAME)
        logFile.writeText(logContent)
        Log.e(TAG, logContent)
    }

    fun getLatestCrashLog(context: Context): String? {
        val logFile = File(context.filesDir, CRASH_FILE_NAME)
        return if (logFile.exists() && logFile.length() > 0) {
            logFile.readText()
        } else {
            null
        }
    }

    fun clearCrashLog(context: Context) {
        val logFile = File(context.filesDir, CRASH_FILE_NAME)
        if (logFile.exists()) {
            logFile.delete()
        }
    }
}
