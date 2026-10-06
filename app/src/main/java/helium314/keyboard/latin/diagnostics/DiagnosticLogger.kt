package helium314.keyboard.latin.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object DiagnosticLogger {
    private const val MAX_LOGS = 500
    private val logs = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun log(event: String) {
        val timestamp = dateFormat.format(Date())
        logs.offer("[$timestamp] $event")
        while (logs.size > MAX_LOGS) {
            logs.poll()
        }
    }

    fun getLogs(): List<String> {
        return logs.toList()
    }

    fun clear() {
        logs.clear()
    }
}
