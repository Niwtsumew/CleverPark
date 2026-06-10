package ua.parking.app

// ─────────────────────────────────────────────────────────────────────────────
// PARKING APP — Additional Unit Tests: New Booking Behaviour
//
// New features covered:
//   UNIT06 — UserBookingManager   one spot per user per parking lot
//   UNIT07 — BookingTimer         elapsed time tracking
//   UNIT08 — BookingRecord        data model & validation
//
// Dependencies (build.gradle):
//   testImplementation 'junit:junit:4.13.2'
//   testImplementation 'org.mockito:mockito-core:5.5.0'
//   testImplementation 'org.mockito.kotlin:mockito-kotlin:5.1.0'
// ─────────────────────────────────────────────────────────────────────────────

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

// ══════════════════════════════════════════════════════════════════════════════
//  SUPPORTING DATA MODELS
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Represents a single active booking tied to a user and a parking lot.
 *
 * @param userId    Firebase Auth UID of the user who booked
 * @param parkingId Document ID of the parking lot
 * @param bookedAt  Unix timestamp (ms) when the booking was created
 */
data class BookingRecord(
    val userId: String,
    val parkingId: String,
    val bookedAt: Long
)

enum class BookingResult {
    SUCCESS,          // Spot booked successfully
    ALREADY_BOOKED,   // This user already holds a spot in this parking lot
    PARKING_FULL,     // No free spots available
    NOT_BOOKED,       // User has no active booking in this lot (used by freeSpot)
    FREED             // Spot was released successfully
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT06 — UserBookingManager
//  "One spot per user per parking lot"
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Manages bookings with a one-spot-per-user-per-parking constraint.
 * In production this wraps a Firestore transaction; in tests it is fully
 * in-memory so no mocks of Firebase are needed.
 *
 * @param initialFreeSpots Starting number of free spots for the parking lot
 */
class UserBookingManager(initialFreeSpots: Int) {

    private var freeSpots: Int = initialFreeSpots

    // Active bookings: key = "$userId:$parkingId"
    private val activeBookings = mutableMapOf<String, BookingRecord>()

    private fun bookingKey(userId: String, parkingId: String) = "$userId:$parkingId"

    /** Returns the active [BookingRecord] for this user+parking, or null. */
    fun getBooking(userId: String, parkingId: String): BookingRecord? =
        activeBookings[bookingKey(userId, parkingId)]

    fun getFreeSpots(): Int = freeSpots

    /**
     * Attempts to book one spot for [userId] in [parkingId].
     *
     * Rules:
     * - If user already has an active booking in this lot → ALREADY_BOOKED
     * - If no free spots → PARKING_FULL
     * - Otherwise → decrement freeSpots, store record → SUCCESS
     */
    fun bookSpot(
        userId: String,
        parkingId: String,
        bookedAt: Long = System.currentTimeMillis()
    ): BookingResult {
        if (activeBookings.containsKey(bookingKey(userId, parkingId))) {
            return BookingResult.ALREADY_BOOKED
        }
        if (freeSpots <= 0) {
            return BookingResult.PARKING_FULL
        }
        freeSpots--
        activeBookings[bookingKey(userId, parkingId)] =
            BookingRecord(userId, parkingId, bookedAt)
        return BookingResult.SUCCESS
    }

    /**
     * Frees the spot held by [userId] in [parkingId].
     *
     * - If user has no active booking in this lot → NOT_BOOKED
     * - Otherwise → increment freeSpots, remove record → FREED
     */
    fun freeSpot(userId: String, parkingId: String): BookingResult {
        val key = bookingKey(userId, parkingId)
        if (!activeBookings.containsKey(key)) {
            return BookingResult.NOT_BOOKED
        }
        activeBookings.remove(key)
        freeSpots++
        return BookingResult.FREED
    }
}

class UserBookingManagerTest {

    private lateinit var manager: UserBookingManager

    private val USER_A  = "user_alice"
    private val USER_B  = "user_bob"
    private val LOT_1   = "p001"
    private val LOT_2   = "p002"

    @Before
    fun setUp() {
        manager = UserBookingManager(initialFreeSpots = 10)
    }

    // ── UNIT06.TS01 — Successful booking ───────────────────────────────────

    /** UNIT06.TS01.TC001 — First booking by a user returns SUCCESS */
    @Test
    fun `bookSpot returns SUCCESS for first booking by user`() {
        val result = manager.bookSpot(USER_A, LOT_1)
        assertEquals(BookingResult.SUCCESS, result)
    }

    /** UNIT06.TS01.TC002 — freeSpots decrements by 1 after booking */
    @Test
    fun `bookSpot decrements freeSpots by 1`() {
        manager.bookSpot(USER_A, LOT_1)
        assertEquals(9, manager.getFreeSpots())
    }

    /** UNIT06.TS01.TC003 — BookingRecord is stored with correct userId and parkingId */
    @Test
    fun `bookSpot stores booking record with correct userId and parkingId`() {
        val timestamp = 1_000_000L
        manager.bookSpot(USER_A, LOT_1, bookedAt = timestamp)
        val record = manager.getBooking(USER_A, LOT_1)
        assertNotNull("Booking record should exist", record)
        assertEquals(USER_A, record!!.userId)
        assertEquals(LOT_1, record.parkingId)
        assertEquals(timestamp, record.bookedAt)
    }

    // ── UNIT06.TS02 — One spot per user per lot ────────────────────────────

    /** UNIT06.TS02.TC001 — Same user booking same lot twice → ALREADY_BOOKED */
    @Test
    fun `bookSpot returns ALREADY_BOOKED when user already has spot in this lot`() {
        manager.bookSpot(USER_A, LOT_1)
        val second = manager.bookSpot(USER_A, LOT_1)
        assertEquals(BookingResult.ALREADY_BOOKED, second)
    }

    /** UNIT06.TS02.TC002 — ALREADY_BOOKED does NOT decrement freeSpots */
    @Test
    fun `ALREADY_BOOKED result does not change freeSpots`() {
        manager.bookSpot(USER_A, LOT_1)
        manager.bookSpot(USER_A, LOT_1) // duplicate
        assertEquals(9, manager.getFreeSpots())
    }

    /** UNIT06.TS02.TC003 — Same user CAN book a different parking lot */
    @Test
    fun `user can book a spot in a different parking lot`() {
        val managerLot2 = UserBookingManager(initialFreeSpots = 5)
        manager.bookSpot(USER_A, LOT_1)
        val result = managerLot2.bookSpot(USER_A, LOT_2)
        assertEquals(BookingResult.SUCCESS, result)
    }

    /** UNIT06.TS02.TC004 — Different users can each book a spot in the same lot */
    @Test
    fun `two different users can each book a spot in the same lot`() {
        val r1 = manager.bookSpot(USER_A, LOT_1)
        val r2 = manager.bookSpot(USER_B, LOT_1)
        assertEquals(BookingResult.SUCCESS, r1)
        assertEquals(BookingResult.SUCCESS, r2)
        assertEquals(8, manager.getFreeSpots())
    }

    // ── UNIT06.TS03 — Parking full ─────────────────────────────────────────

    /** UNIT06.TS03.TC001 — PARKING_FULL when freeSpots == 0 */
    @Test
    fun `bookSpot returns PARKING_FULL when no spots available`() {
        val fullManager = UserBookingManager(initialFreeSpots = 0)
        val result = fullManager.bookSpot(USER_A, LOT_1)
        assertEquals(BookingResult.PARKING_FULL, result)
    }

    /** UNIT06.TS03.TC002 — After last spot is taken, next user gets PARKING_FULL */
    @Test
    fun `last spot taken makes parking full for next user`() {
        val tightManager = UserBookingManager(initialFreeSpots = 1)
        tightManager.bookSpot(USER_A, LOT_1)
        val result = tightManager.bookSpot(USER_B, LOT_1)
        assertEquals(BookingResult.PARKING_FULL, result)
        assertEquals(0, tightManager.getFreeSpots())
    }

    // ── UNIT06.TS04 — Free spot ────────────────────────────────────────────

    /** UNIT06.TS04.TC001 — freeSpot returns FREED and increments freeSpots */
    @Test
    fun `freeSpot returns FREED and increments freeSpots`() {
        manager.bookSpot(USER_A, LOT_1)
        val result = manager.freeSpot(USER_A, LOT_1)
        assertEquals(BookingResult.FREED, result)
        assertEquals(10, manager.getFreeSpots())
    }

    /** UNIT06.TS04.TC002 — BookingRecord is removed after freeSpot */
    @Test
    fun `freeSpot removes booking record so getBooking returns null`() {
        manager.bookSpot(USER_A, LOT_1)
        manager.freeSpot(USER_A, LOT_1)
        assertNull(manager.getBooking(USER_A, LOT_1))
    }

    /** UNIT06.TS04.TC003 — freeSpot on non-existent booking returns NOT_BOOKED */
    @Test
    fun `freeSpot returns NOT_BOOKED when user has no active booking`() {
        val result = manager.freeSpot(USER_A, LOT_1)
        assertEquals(BookingResult.NOT_BOOKED, result)
    }

    /** UNIT06.TS04.TC004 — freeSpot does not change freeSpots when NOT_BOOKED */
    @Test
    fun `freeSpot does not change freeSpots when user has no booking`() {
        manager.freeSpot(USER_A, LOT_1)
        assertEquals(10, manager.getFreeSpots())
    }

    /** UNIT06.TS04.TC005 — After freeing, same user can book again */
    @Test
    fun `user can book again after freeing their spot`() {
        manager.bookSpot(USER_A, LOT_1)
        manager.freeSpot(USER_A, LOT_1)
        val result = manager.bookSpot(USER_A, LOT_1)
        assertEquals(BookingResult.SUCCESS, result)
    }

    /** UNIT06.TS04.TC006 — Only the user who booked can free the spot */
    @Test
    fun `only the booking user can free their spot - other user gets NOT_BOOKED`() {
        manager.bookSpot(USER_A, LOT_1)
        val result = manager.freeSpot(USER_B, LOT_1) // wrong user
        assertEquals(BookingResult.NOT_BOOKED, result)
        assertEquals(9, manager.getFreeSpots())  // unchanged
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT07 — BookingTimer
//  "Show how long the spot has been occupied"
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Calculates elapsed time for an active booking.
 *
 * Uses an injectable [clock] function so tests can control "now"
 * without sleeping.
 */
class BookingTimer(private val clock: () -> Long = System::currentTimeMillis) {

    data class ElapsedTime(
        val totalSeconds: Long,
        val hours: Long,
        val minutes: Long,
        val seconds: Long
    ) {
        /** Human-readable format: "1 год 04 хв 30 с" */
        fun format(): String = when {
            hours > 0   -> "%d год %02d хв %02d с".format(hours, minutes, seconds)
            minutes > 0 -> "%d хв %02d с".format(minutes, seconds)
            else        -> "%d с".format(seconds)
        }
    }

    /**
     * Returns elapsed time between [bookedAt] and the current clock time.
     * Returns null if [bookedAt] is in the future (invalid state).
     */
    fun getElapsed(bookedAt: Long): ElapsedTime? {
        val now = clock()
        if (bookedAt > now) return null

        val totalSeconds = (now - bookedAt) / 1_000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return ElapsedTime(totalSeconds, hours, minutes, seconds)
    }

    /**
     * Returns true if the booking has exceeded [limitSeconds].
     * Useful for UI warnings (e.g. "You've been parked for over 2 hours").
     */
    fun isOverLimit(bookedAt: Long, limitSeconds: Long): Boolean {
        val elapsed = getElapsed(bookedAt) ?: return false
        return elapsed.totalSeconds > limitSeconds
    }
}

class BookingTimerTest {

    // ── UNIT07.TS01 — Elapsed time calculation ─────────────────────────────

    /** UNIT07.TS01.TC001 — Exactly 0 seconds elapsed */
    @Test
    fun `getElapsed returns zero seconds when bookedAt equals now`() {
        val now = 1_000_000_000L
        val timer = BookingTimer(clock = { now })
        val elapsed = timer.getElapsed(bookedAt = now)
        assertNotNull(elapsed)
        assertEquals(0L, elapsed!!.totalSeconds)
    }

    /** UNIT07.TS01.TC002 — 90 seconds → 0 hours, 1 minute, 30 seconds */
    @Test
    fun `getElapsed decomposes 90 seconds into 1 min 30 sec`() {
        val bookedAt = 0L
        val timer = BookingTimer(clock = { 90_000L }) // 90 seconds later
        val elapsed = timer.getElapsed(bookedAt)!!
        assertEquals(90L, elapsed.totalSeconds)
        assertEquals(0L,  elapsed.hours)
        assertEquals(1L,  elapsed.minutes)
        assertEquals(30L, elapsed.seconds)
    }

    /** UNIT07.TS01.TC003 — 3 661 seconds → 1 hour, 1 minute, 1 second */
    @Test
    fun `getElapsed decomposes 3661 seconds into 1 hr 1 min 1 sec`() {
        val bookedAt = 0L
        val timer = BookingTimer(clock = { 3_661_000L })
        val elapsed = timer.getElapsed(bookedAt)!!
        assertEquals(3661L, elapsed.totalSeconds)
        assertEquals(1L,    elapsed.hours)
        assertEquals(1L,    elapsed.minutes)
        assertEquals(1L,    elapsed.seconds)
    }

    /** UNIT07.TS01.TC004 — bookedAt in the future → getElapsed returns null */
    @Test
    fun `getElapsed returns null when bookedAt is in the future`() {
        val timer = BookingTimer(clock = { 1_000L })
        val result = timer.getElapsed(bookedAt = 5_000L)
        assertNull("Future timestamp should return null", result)
    }

    // ── UNIT07.TS02 — format() display ────────────────────────────────────

    /** UNIT07.TS02.TC001 — Only seconds → "45 с" */
    @Test
    fun `format shows only seconds when under 1 minute`() {
        val bookedAt = 0L
        val timer = BookingTimer(clock = { 45_000L })
        val formatted = timer.getElapsed(bookedAt)!!.format()
        assertEquals("45 с", formatted)
    }

    /** UNIT07.TS02.TC002 — Minutes and seconds → "2 хв 05 с" */
    @Test
    fun `format shows minutes and seconds when under 1 hour`() {
        val bookedAt = 0L
        val timer = BookingTimer(clock = { 125_000L }) // 2 min 5 sec
        val formatted = timer.getElapsed(bookedAt)!!.format()
        assertEquals("2 хв 05 с", formatted)
    }

    /** UNIT07.TS02.TC003 — Hours, minutes, seconds → "1 год 04 хв 30 с" */
    @Test
    fun `format shows hours minutes seconds when over 1 hour`() {
        val bookedAt = 0L
        val seconds = (1 * 3600 + 4 * 60 + 30).toLong()
        val timer = BookingTimer(clock = { seconds * 1_000 })
        val formatted = timer.getElapsed(bookedAt)!!.format()
        assertEquals("1 год 04 хв 30 с", formatted)
    }

    // ── UNIT07.TS03 — Over-limit check ────────────────────────────────────

    /** UNIT07.TS03.TC001 — 1 hour elapsed, limit 2 hours → NOT over limit */
    @Test
    fun `isOverLimit returns false when elapsed is under the limit`() {
        val timer = BookingTimer(clock = { 3_600_000L }) // 1 hour
        assertFalse(timer.isOverLimit(bookedAt = 0L, limitSeconds = 7_200))
    }

    /** UNIT07.TS03.TC002 — 3 hours elapsed, limit 2 hours → over limit */
    @Test
    fun `isOverLimit returns true when elapsed exceeds the limit`() {
        val timer = BookingTimer(clock = { 10_800_000L }) // 3 hours
        assertTrue(timer.isOverLimit(bookedAt = 0L, limitSeconds = 7_200))
    }

    /** Boundary: exactly at the limit → NOT over (strictly greater) */
    @Test
    fun `isOverLimit returns false when elapsed equals exactly the limit`() {
        val timer = BookingTimer(clock = { 7_200_000L }) // exactly 2 hours
        assertFalse(timer.isOverLimit(bookedAt = 0L, limitSeconds = 7_200))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT08 — BookingRecord model validation
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Validates that a [BookingRecord] is well-formed before persisting to Firestore.
 */
class BookingRecordValidator {

    data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

    fun validate(record: BookingRecord): ValidationResult {
        if (record.userId.isBlank())
            return ValidationResult(false, "userId не може бути порожнім")
        if (record.parkingId.isBlank())
            return ValidationResult(false, "parkingId не може бути порожнім")
        if (record.bookedAt <= 0)
            return ValidationResult(false, "bookedAt має бути позитивним числом")
        return ValidationResult(true)
    }
}

class BookingRecordValidatorTest {

    private lateinit var validator: BookingRecordValidator

    @Before
    fun setUp() {
        validator = BookingRecordValidator()
    }

    // ── UNIT08.TS01 ────────────────────────────────────────────────────────

    /** UNIT08.TS01.TC001 — Valid record → isValid = true */
    @Test
    fun `validate returns true for a well-formed BookingRecord`() {
        val record = BookingRecord("user_01", "p001", bookedAt = 1_700_000_000_000L)
        val result = validator.validate(record)
        assertTrue(result.isValid)
    }

    /** UNIT08.TS01.TC002 — Blank userId → isValid = false */
    @Test
    fun `validate returns false when userId is blank`() {
        val record = BookingRecord("", "p001", bookedAt = 1_700_000_000_000L)
        val result = validator.validate(record)
        assertFalse(result.isValid)
        assertEquals("userId не може бути порожнім", result.errorMessage)
    }

    /** UNIT08.TS01.TC003 — Blank parkingId → isValid = false */
    @Test
    fun `validate returns false when parkingId is blank`() {
        val record = BookingRecord("user_01", "  ", bookedAt = 1_700_000_000_000L)
        val result = validator.validate(record)
        assertFalse(result.isValid)
        assertEquals("parkingId не може бути порожнім", result.errorMessage)
    }

    /** UNIT08.TS01.TC004 — bookedAt = 0 → isValid = false */
    @Test
    fun `validate returns false when bookedAt is zero`() {
        val record = BookingRecord("user_01", "p001", bookedAt = 0L)
        val result = validator.validate(record)
        assertFalse(result.isValid)
        assertEquals("bookedAt має бути позитивним числом", result.errorMessage)
    }

    /** UNIT08.TS01.TC005 — bookedAt < 0 → isValid = false */
    @Test
    fun `validate returns false when bookedAt is negative`() {
        val record = BookingRecord("user_01", "p001", bookedAt = -1L)
        val result = validator.validate(record)
        assertFalse(result.isValid)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  INTEGRATION SCENARIO TESTS
//  Full booking lifecycle: book → check timer → free
// ══════════════════════════════════════════════════════════════════════════════

class BookingLifecycleTest {

    private val USER_A   = "user_alice"
    private val PARKING  = "p001"

    /**
     * Full happy-path: book a spot, check it's recorded, check timer,
     * then free it and confirm the slot is available again.
     */
    @Test
    fun `full lifecycle - book then free restores free spots and removes record`() {
        val manager   = UserBookingManager(initialFreeSpots = 5)
        val bookedAt  = 0L
        val timer     = BookingTimer(clock = { 3_660_000L }) // 1 hr 1 min later

        // 1. Book
        assertEquals(BookingResult.SUCCESS, manager.bookSpot(USER_A, PARKING, bookedAt))
        assertEquals(4, manager.getFreeSpots())

        // 2. Verify record
        val record = manager.getBooking(USER_A, PARKING)
        assertNotNull(record)
        assertEquals(bookedAt, record!!.bookedAt)

        // 3. Timer check (1 h 1 min elapsed)
        val elapsed = timer.getElapsed(record.bookedAt)!!
        assertEquals(1L, elapsed.hours)
        assertEquals(1L, elapsed.minutes)
        assertEquals(0L, elapsed.seconds)

        // 4. Free
        assertEquals(BookingResult.FREED, manager.freeSpot(USER_A, PARKING))
        assertEquals(5, manager.getFreeSpots())
        assertNull(manager.getBooking(USER_A, PARKING))
    }

    /**
     * Edge case: booking full parking after another user frees their spot.
     */
    @Test
    fun `spot freed by user A allows user B to book successfully`() {
        val manager = UserBookingManager(initialFreeSpots = 1)
        manager.bookSpot("user_alice", PARKING)
        // Parking now full
        assertEquals(BookingResult.PARKING_FULL, manager.bookSpot("user_bob", PARKING))
        // Alice frees her spot
        manager.freeSpot("user_alice", PARKING)
        // Bob can now book
        assertEquals(BookingResult.SUCCESS, manager.bookSpot("user_bob", PARKING))
    }

    /**
     * Concurrent-style: multiple users book/free in sequence,
     * freeSpots always stays consistent.
     */
    @Test
    fun `freeSpots stays consistent through multiple book and free operations`() {
        val manager = UserBookingManager(initialFreeSpots = 3)
        val users   = listOf("u1", "u2", "u3")

        users.forEach { manager.bookSpot(it, PARKING) }
        assertEquals(0, manager.getFreeSpots())

        manager.freeSpot("u2", PARKING)
        assertEquals(1, manager.getFreeSpots())

        manager.bookSpot("u4", PARKING)
        assertEquals(0, manager.getFreeSpots())

        users.forEach { manager.freeSpot(it, PARKING) }
        manager.freeSpot("u4", PARKING)
        assertEquals(3, manager.getFreeSpots())
    }
}
