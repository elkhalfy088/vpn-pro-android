package com.vpnpro.utils

object FormatUtils {
    fun formatBytes(bytes: Long): String = when {
        bytes < 1024L          -> "${bytes} B"
        bytes < 1024L * 1024L  -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024L * 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
