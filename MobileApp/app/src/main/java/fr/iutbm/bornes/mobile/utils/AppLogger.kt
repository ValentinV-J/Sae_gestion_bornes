package fr.iutbm.bornes.mobile.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralise les logs: Logcat + buffer memoire pour affichage a l'ecran.
 */
object AppLogger {
    private const val MAX_LINES = 150
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val lines = ArrayDeque<String>()
    private val listeners = LinkedHashSet<(String) -> Unit>()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        append("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append("I", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        append("W", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
            append("E", tag, "$message | ${throwable.javaClass.simpleName}: ${throwable.message}")
        } else {
            Log.e(tag, message)
            append("E", tag, message)
        }
    }

    fun v(tag: String, message: String) {
        Log.v(tag, message)
        append("V", tag, message)
    }

    fun observe(onUpdate: (String) -> Unit): () -> Unit {
        listeners.add(onUpdate)
        onUpdate(currentDump())
        return {
            listeners.remove(onUpdate)
        }
    }

    private fun append(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val line = "$timestamp $level/$tag: $message"

        lines.addLast(line)
        while (lines.size > MAX_LINES) {
            lines.removeFirst()
        }

        notifyListeners()
    }

    private fun notifyListeners() {
        val snapshot = currentDump()
        mainHandler.post {
            listeners.forEach { it(snapshot) }
        }
    }

    private fun currentDump(): String {
        return if (lines.isEmpty()) "" else lines.joinToString(separator = "\n")
    }
}

