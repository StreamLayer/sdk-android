package io.streamlayer.demo.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import io.streamlayer.demo.BuildConfig
import io.streamlayer.sdk.StreamLayer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

object LogUtils {

    fun getLogsDir(context: Context, clear: Boolean): File {
        val logs = File(context.getExternalFilesDir(null), "streamlayer_logs")
        if (logs.exists() && clear) if (logs.isDirectory) logs.listFiles()?.forEach { it.delete() }
        if (!logs.exists()) logs.mkdirs()
        return logs
    }

    fun getLogFileUries(context: Context): ArrayList<Uri> {
        val files = ArrayList<Uri>()
        val logs = getLogsDir(context, false)
        if (!logs.exists()) return files
        if (logs.isDirectory) {
            logs.listFiles()?.forEach { file ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    files.add(
                        FileProvider.getUriForFile(
                            context, BuildConfig.APPLICATION_ID + ".fileprovider",
                            file!!
                        )
                    )
                } else {
                    files.add(Uri.fromFile(file))
                }
            }
        }
        return files
    }
}

class SdkFileLogger(context: Context) : StreamLayer.LogListener {

    companion object {
        private const val FILE_NAME = "sdk.txt"
        var PRINT_TO_LOGCAT = false
    }

    private val fullDateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH)
    }

    private var outputStream: FileOutputStream? = null

    init {
        try {
            // create new file each time when logger initialized
            val logs = LogUtils.getLogsDir(context, true)
            val logFile = File(logs, FILE_NAME)
            if (logFile.exists()) logFile.delete() else logFile.createNewFile()
            outputStream?.close()
            outputStream = FileOutputStream(logFile)
        } catch (e: Throwable) {
            Log.e("SdkFileLogger", "failed to open file $e")
        }
    }

    override fun log(level: StreamLayer.LogLevel, msg: String) {
        if (PRINT_TO_LOGCAT) Logger.getGlobal().log(Level.INFO, msg)
        outputStream?.apply {
            try {
                // we don't need to write VERBOSE and DEBUG messages in file
                val formattedMsg = when (level) {
                    StreamLayer.LogLevel.INFO, StreamLayer.LogLevel.WARNING, StreamLayer.LogLevel.ERROR ->
                        "${fullDateFormat.format(Date())} $msg\n"
                    else -> null
                }
                formattedMsg?.let { write(it.toByteArray()) }
            } catch (e: IOException) {
                Log.e("SdkFileLogger", "failed to write log message")
            }
        }
    }
}