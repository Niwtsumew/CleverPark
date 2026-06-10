package ua.parking.app.domain

class BookingTimer(private val clock: () -> Long = System::currentTimeMillis) {

    data class ElapsedTime(
        val totalSeconds: Long,
        val hours: Long,
        val minutes: Long,
        val seconds: Long
    ) {
        fun format(): String = when {
            hours > 0   -> "%d год %02d хв %02d с".format(hours, minutes, seconds)
            minutes > 0 -> "%d хв %02d с".format(minutes, seconds)
            else        -> "%d с".format(seconds)
        }
    }

    fun getElapsed(bookedAt: Long): ElapsedTime? {
        val now = clock()
        if (bookedAt > now) return null
        val totalSeconds = (now - bookedAt) / 1_000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return ElapsedTime(totalSeconds, hours, minutes, seconds)
    }

    fun isOverLimit(bookedAt: Long, limitSeconds: Long): Boolean {
        val elapsed = getElapsed(bookedAt) ?: return false
        return elapsed.totalSeconds > limitSeconds
    }
}
