package com.akshatdjain.ultron.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UltronLogger {
    private const val MAX_LINES = 500

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    private var logFile: File? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        logFile = File(context.getExternalFilesDir(null) ?: context.filesDir, "ultron_ble.log")
    }

    fun d(tag: String, message: String) = log("D", tag, message)
    fun e(tag: String, message: String) = log("E", tag, message)

    private fun log(level: String, tag: String, message: String) {
        if (level == "E") Log.e(tag, message) else Log.d(tag, message)
        val line = "${timeFormat.format(Date())} $level/$tag: $message"
        _lines.update { (it + line).takeLast(MAX_LINES) }
        logFile?.let { file -> runCatching { file.appendText(line + "\n") } }
    }

    fun clear() {
        _lines.value = emptyList()
        logFile?.let { runCatching { it.writeText("") } }
    }
}
