package aanibrothers.tracker.io.afterCall

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

object Formatter {
    @Throws(ParseException::class)
    fun extractTime(dateString: String): String {
        val output = SimpleDateFormat("hh:mm", Locale.US)
        val input = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
        val date = input.parse(dateString) ?: throw ParseException("Unable to parse date", 0)
        return output.format(date)
    }

    fun getTimeDiff(start: Long, end: Long): String {
        val diff = max(0L, end - start)
        val seconds = diff / 1000L % 60L
        val minutes = diff / 60000L % 60L
        val hours = diff / 3600000L
        return if (hours > 0L) {
            "${format(hours)}:${format(minutes)}:${format(seconds)}"
        } else {
            "${format(minutes)}:${format(seconds)}"
        }
    }

    private fun format(value: Long): String {
        return if (value < 10L) {
            "0$value"
        } else {
            value.toString()
        }
    }
}
