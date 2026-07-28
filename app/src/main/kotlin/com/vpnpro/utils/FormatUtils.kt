package com.vpnpro.utils

object FormatUtils {

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024L           -> "${bytes} B"
            bytes < 1024L * 1024    -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else                    -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatBytesPerSec(bytesPerSec: Long): String = "${formatBytes(bytesPerSec)}/s"
}
