package com.nbp.unique_legends.util

object TimeUtil {
    fun daysToMillis(days: Int): Long {
        return days.coerceAtLeast(0).toLong() * 24L * 60L * 60L * 1000L
    }

    fun minutesToTicks(minutes: Int): Long {
        return minutes.coerceAtLeast(1).toLong() * 60L * 20L
    }
}
