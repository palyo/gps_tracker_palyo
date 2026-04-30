package com.post.call.info.utils

import java.text.SimpleDateFormat
import java.util.Locale

object Formatter {

    @Throws(Exception::class)
    fun extractTime(dateString: String): String =
        SimpleDateFormat("hh:mm", Locale.US)
            .format(SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).parse(dateString)!!)

    fun getTimeDiff(start: Long, end: Long): String {
        val diff = maxOf(0L, end - start)
        val seconds = (diff / 1000) % 60
        val minutes = (diff / (1000 * 60)) % 60
        val hours = diff / (1000 * 60 * 60)
        return if (hours > 0) "${format(hours)}:${format(minutes)}:${format(seconds)}"
        else "${format(minutes)}:${format(seconds)}"
    }

    private fun format(value: Long): String = if (value < 10) "0$value" else "$value"
}
