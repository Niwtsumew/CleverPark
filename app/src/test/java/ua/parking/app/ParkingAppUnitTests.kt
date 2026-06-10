package ua.parking.app

// ─────────────────────────────────────────────────────────────────────────────
// PARKING APP — Unit Tests
// Котляров Б.І., 643п, Тестування та верифікація ПЗ
//
// Modules covered:
//   UNIT01 — AuthValidator      (FR01)
//   UNIT02 — ParkingSearchFilter (FR02)
//   UNIT03 — ParkingModel        (FR03)
//   UNIT04 — BookingManager      (FR04)
//   UNIT05 — FavoritesManager    (FR05)
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
//  DATA MODELS (referenced by tests)
// ══════════════════════════════════════════════════════════════════════════════

data class ParkingModel(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String,
    val schedule: String,
    val totalSpots: Int,
    val occupiedSpots: Int
) {
    /**
     * FR03: Calculates free spots.
     * If occupiedSpots > totalSpots the model is in an invalid state — guard
     * against negative free spots by returning 0.
     */
    fun getFreeSpots(): Int = maxOf(0, totalSpots - occupiedSpots)

    /** FR04: Returns true when no spots are available. */
    fun isFull(): Boolean = getFreeSpots() == 0
}

enum class BookingStatus {
    SUCCESS,
    PARKING_FULL,
    ERROR
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT01 — AuthValidator
// ══════════════════════════════════════════════════════════════════════════════

/**
 * FR01: Validates user credentials before sending to Firebase Authentication.
 */
class AuthValidator {

    data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

    /** Validates email format. */
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Поле не може бути порожнім")
        }
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return if (emailRegex.matches(email.trim())) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "Некоректний формат email")
        }
    }

    /**
     * Validates password.
     * Rules: minimum 6 characters, must contain at least one digit.
     */
    fun validatePassword(password: String): ValidationResult {
        if (password.length < 6) {
            return ValidationResult(false, "Пароль занадто короткий")
        }
        if (!password.any { it.isDigit() }) {
            return ValidationResult(false, "Пароль повинен містити хоча б одну цифру")
        }
        return ValidationResult(true)
    }
}

class AuthValidatorTest {

    private lateinit var validator: AuthValidator

    @Before
    fun setUp() {
        validator = AuthValidator()
    }

    // ── UNIT01.TS01 — Email validation ─────────────────────────────────────

    /** UNIT01.TS01.TC001 — Valid email → isValid = true */
    @Test
    fun `validateEmail with valid email returns true`() {
        val result = validator.validateEmail("test@parking.ua")
        assertTrue("Expected valid email to pass", result.isValid)
        assertEquals("", result.errorMessage)
    }

    /** UNIT01.TS01.TC002 — Email without @ symbol → isValid = false */
    @Test
    fun `validateEmail without at-sign returns false with error message`() {
        val result = validator.validateEmail("testparking.ua")
        assertFalse("Expected invalid email to fail", result.isValid)
        assertEquals("Некоректний формат email", result.errorMessage)
    }

    /** UNIT01.TS01.TC003 — Empty string → isValid = false, field-empty message */
    @Test
    fun `validateEmail with empty string returns false with empty-field message`() {
        val result = validator.validateEmail("")
        assertFalse("Expected empty email to fail", result.isValid)
        assertEquals("Поле не може бути порожнім", result.errorMessage)
    }

    /** Extra: Email with spaces only → treated as blank */
    @Test
    fun `validateEmail with blank spaces returns false`() {
        val result = validator.validateEmail("   ")
        assertFalse(result.isValid)
        assertEquals("Поле не може бути порожнім", result.errorMessage)
    }

    // ── UNIT01.TS02 — Password validation ──────────────────────────────────

    /** UNIT01.TS02.TC001 — Valid password (≥6 chars, has digit) → isValid = true */
    @Test
    fun `validatePassword with valid password returns true`() {
        val result = validator.validatePassword("Test123")
        assertTrue("Expected valid password to pass", result.isValid)
    }

    /** UNIT01.TS02.TC002 — Password shorter than 6 chars → isValid = false */
    @Test
    fun `validatePassword shorter than 6 chars returns false`() {
        val result = validator.validatePassword("Ab1")
        assertFalse("Expected short password to fail", result.isValid)
        assertEquals("Пароль занадто короткий", result.errorMessage)
    }

    /** Boundary: exactly 5 chars → still too short */
    @Test
    fun `validatePassword with exactly 5 chars returns false`() {
        val result = validator.validatePassword("Ab1cd")
        assertFalse(result.isValid)
        assertEquals("Пароль занадто короткий", result.errorMessage)
    }

    /** Boundary: exactly 6 chars with digit → valid */
    @Test
    fun `validatePassword with exactly 6 chars and digit returns true`() {
        val result = validator.validatePassword("Abc123")
        assertTrue(result.isValid)
    }

    /** Password with no digits → isValid = false */
    @Test
    fun `validatePassword without digits returns false`() {
        val result = validator.validatePassword("TestPassword")
        assertFalse(result.isValid)
        assertEquals("Пароль повинен містити хоча б одну цифру", result.errorMessage)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT02 — ParkingSearchFilter
// ══════════════════════════════════════════════════════════════════════════════

/**
 * FR02: Filters a list of parking lots by a partial, case-insensitive name match.
 */
class ParkingSearchFilter {

    /**
     * Returns all parkings whose name contains [query] (case-insensitive).
     * If [query] is blank the full list is returned.
     */
    fun filter(list: List<ParkingModel>, query: String): List<ParkingModel> {
        if (query.isBlank()) return list
        val lowerQuery = query.trim().lowercase()
        return list.filter { it.name.lowercase().contains(lowerQuery) }
    }
}

class ParkingSearchFilterTest {

    private lateinit var filter: ParkingSearchFilter
    private lateinit var parkingList: List<ParkingModel>

    @Before
    fun setUp() {
        filter = ParkingSearchFilter()
        parkingList = listOf(
            ParkingModel("p001", "Центральна",         "вул. Центральна 1", "", "08:00-22:00", 50, 20),
            ParkingModel("p002", "Центральний сквер",  "пл. Свободи 2",    "", "00:00-24:00", 30, 5),
            ParkingModel("p003", "Привокзальна",       "пл. Привокзальна", "", "06:00-23:00", 40, 40),
            ParkingModel("p004", "Університетська",    "вул. Науки 14",    "", "08:00-20:00", 60, 10),
            ParkingModel("p005", "Салтівська",         "вул. Салтівська",  "", "24/7",        35, 0),
        )
    }

    // ── UNIT02.TS01 ────────────────────────────────────────────────────────

    /** UNIT02.TS01.TC001 — Exact name match returns exactly 1 result */
    @Test
    fun `filter with exact name returns single matching parking`() {
        val result = filter.filter(parkingList, "Центральна")
        assertEquals(1, result.size)
        assertEquals("Центральна", result.first().name)
    }

    /** UNIT02.TS01.TC002 — Partial case-insensitive match returns multiple */
    @Test
    fun `filter with partial lowercase query returns all matching parkings`() {
        val result = filter.filter(parkingList, "центр")
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Центральна" })
        assertTrue(result.any { it.name == "Центральний сквер" })
    }

    /** UNIT02.TS01.TC003 — Empty query returns full list */
    @Test
    fun `filter with empty query returns full list`() {
        val result = filter.filter(parkingList, "")
        assertEquals(5, result.size)
    }

    /** Extra: Query with no matches returns empty list */
    @Test
    fun `filter with non-matching query returns empty list`() {
        val result = filter.filter(parkingList, "Xxxxxx")
        assertTrue(result.isEmpty())
    }

    /** Extra: Blank (spaces only) query returns full list */
    @Test
    fun `filter with blank spaces query returns full list`() {
        val result = filter.filter(parkingList, "   ")
        assertEquals(5, result.size)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT03 — ParkingModel (business logic)
// ══════════════════════════════════════════════════════════════════════════════

class ParkingModelTest {

    // ── UNIT03.TS01 ────────────────────────────────────────────────────────

    /** UNIT03.TS01.TC001 — getFreeSpots() = total − occupied (normal state) */
    @Test
    fun `getFreeSpots returns correct difference between total and occupied`() {
        val parking = ParkingModel("p1", "Test", "", "", "", totalSpots = 50, occupiedSpots = 20)
        assertEquals(30, parking.getFreeSpots())
    }

    /** UNIT03.TS01.TC002 — isFull() = true when freeSpots == 0 */
    @Test
    fun `isFull returns true when all spots are occupied`() {
        val parking = ParkingModel("p1", "Test", "", "", "", totalSpots = 30, occupiedSpots = 30)
        assertTrue(parking.isFull())
        assertEquals(0, parking.getFreeSpots())
    }

    /** UNIT03.TS01.TC003 — Invalid state: occupied > total → getFreeSpots = 0 (guarded) */
    @Test
    fun `getFreeSpots guards against negative value when occupied exceeds total`() {
        val parking = ParkingModel("p1", "Test", "", "", "", totalSpots = 30, occupiedSpots = 35)
        assertEquals(0, parking.getFreeSpots())
        assertTrue(parking.isFull())
    }

    /** Boundary: 1 free spot remaining → not full */
    @Test
    fun `isFull returns false when one spot remains`() {
        val parking = ParkingModel("p1", "Test", "", "", "", totalSpots = 30, occupiedSpots = 29)
        assertFalse(parking.isFull())
        assertEquals(1, parking.getFreeSpots())
    }

    /** Boundary: all spots free (occupiedSpots = 0) */
    @Test
    fun `getFreeSpots equals totalSpots when no spots are occupied`() {
        val parking = ParkingModel("p1", "Test", "", "", "", totalSpots = 60, occupiedSpots = 0)
        assertEquals(60, parking.getFreeSpots())
        assertFalse(parking.isFull())
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT04 — BookingManager
// ══════════════════════════════════════════════════════════════════════════════

/**
 * FR04: Manages spot booking logic in isolation (Firestore replaced by mock).
 */
class BookingManager(private var freeSpots: Int) {

    /**
     * Attempts to book one spot.
     * @return [BookingStatus.SUCCESS] if booked, [BookingStatus.PARKING_FULL] otherwise.
     */
    fun bookSpot(): BookingStatus {
        return if (freeSpots > 0) {
            freeSpots--
            BookingStatus.SUCCESS
        } else {
            BookingStatus.PARKING_FULL
        }
    }

    fun getFreeSpots(): Int = freeSpots

    fun getBookingStatus(): BookingStatus =
        if (freeSpots == 0) BookingStatus.PARKING_FULL else BookingStatus.SUCCESS
}

class BookingManagerTest {

    // ── UNIT04.TS01 ────────────────────────────────────────────────────────

    /** UNIT04.TS01.TC001 — bookSpot() decrements freeSpots by 1 and returns SUCCESS */
    @Test
    fun `bookSpot decrements freeSpots by one and returns SUCCESS`() {
        val manager = BookingManager(freeSpots = 10)
        val status = manager.bookSpot()
        assertEquals(BookingStatus.SUCCESS, status)
        assertEquals(9, manager.getFreeSpots())
    }

    /** UNIT04.TS01.TC002 — bookSpot() returns PARKING_FULL when freeSpots == 0 */
    @Test
    fun `bookSpot returns PARKING_FULL when no spots available`() {
        val manager = BookingManager(freeSpots = 0)
        val status = manager.bookSpot()
        assertEquals(BookingStatus.PARKING_FULL, status)
        assertEquals(0, manager.getFreeSpots()) // unchanged
    }

    /** UNIT04.TS01.TC003 — getBookingStatus() returns PARKING_FULL when freeSpots == 0 */
    @Test
    fun `getBookingStatus returns PARKING_FULL when zero free spots`() {
        val manager = BookingManager(freeSpots = 0)
        assertEquals(BookingStatus.PARKING_FULL, manager.getBookingStatus())
    }

    /** Boundary: last spot is booked → next bookSpot() returns PARKING_FULL */
    @Test
    fun `booking last spot makes parking full on next attempt`() {
        val manager = BookingManager(freeSpots = 1)
        assertEquals(BookingStatus.SUCCESS, manager.bookSpot())
        assertEquals(0, manager.getFreeSpots())
        assertEquals(BookingStatus.PARKING_FULL, manager.bookSpot())
    }

    /** getBookingStatus() returns SUCCESS when spots remain */
    @Test
    fun `getBookingStatus returns SUCCESS when spots are available`() {
        val manager = BookingManager(freeSpots = 5)
        assertEquals(BookingStatus.SUCCESS, manager.getBookingStatus())
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT05 — FavoritesManager
// ══════════════════════════════════════════════════════════════════════════════

/**
 * FR05: In-memory favorites management logic (Firestore replaced by mock).
 */
class FavoritesManager {

    private val favorites = mutableSetOf<String>()

    fun addFavorite(parkingId: String) {
        favorites.add(parkingId)
    }

    fun removeFavorite(parkingId: String) {
        favorites.remove(parkingId)
    }

    fun isFavorite(parkingId: String): Boolean = favorites.contains(parkingId)

    fun getFavorites(): Set<String> = favorites.toSet()
}

class FavoritesManagerTest {

    private lateinit var manager: FavoritesManager

    @Before
    fun setUp() {
        manager = FavoritesManager()
    }

    // ── UNIT05.TS01 ────────────────────────────────────────────────────────

    /** UNIT05.TS01.TC001 — addFavorite() adds parkingId; isFavorite() = true */
    @Test
    fun `addFavorite adds parkingId and isFavorite returns true`() {
        manager.addFavorite("p001")
        assertTrue(manager.isFavorite("p001"))
        assertTrue(manager.getFavorites().contains("p001"))
    }

    /** UNIT05.TS01.TC002 — removeFavorite() removes parkingId; isFavorite() = false */
    @Test
    fun `removeFavorite removes parkingId and isFavorite returns false`() {
        manager.addFavorite("p001")
        manager.addFavorite("p002")
        manager.removeFavorite("p001")
        assertFalse(manager.isFavorite("p001"))
        assertTrue(manager.isFavorite("p002"))
        assertEquals(setOf("p002"), manager.getFavorites())
    }

    /** Extra: isFavorite() on empty list returns false */
    @Test
    fun `isFavorite returns false for unknown parkingId`() {
        assertFalse(manager.isFavorite("p999"))
    }

    /** Extra: Duplicate addFavorite has no side effect (idempotent) */
    @Test
    fun `addFavorite is idempotent — duplicate adds result in single entry`() {
        manager.addFavorite("p001")
        manager.addFavorite("p001")
        assertEquals(1, manager.getFavorites().size)
    }

    /** Extra: removeFavorite on non-existent ID does not throw */
    @Test
    fun `removeFavorite on non-existent id does not throw`() {
        // should not throw any exception
        manager.removeFavorite("p999")
        assertTrue(manager.getFavorites().isEmpty())
    }
}
