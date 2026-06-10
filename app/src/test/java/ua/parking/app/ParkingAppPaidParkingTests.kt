package ua.parking.app

// ─────────────────────────────────────────────────────────────────────────────
// PARKING APP — Unit Tests: Paid Parking, Balance & Tarification
//
// New features covered:
//   UNIT09 — ParkingSearchEngine   search by name AND address
//   UNIT10 — ParkingFilter         free/paid filter + combined filters
//   UNIT11 — ParkingPriceValidator price-per-hour validation
//   UNIT12 — UserBalance           balance top-up & deduction
//   UNIT13 — TariffCalculator      cost calculation based on elapsed time
//   UNIT14 — CheckoutManager       full checkout flow (balance check + deduct)
//
// Dependencies (build.gradle):
//   testImplementation 'junit:junit:4.13.2'
//   testImplementation 'org.mockito:mockito-core:5.5.0'
//   testImplementation 'org.mockito.kotlin:mockito-kotlin:5.1.0'
// ─────────────────────────────────────────────────────────────────────────────

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// ══════════════════════════════════════════════════════════════════════════════
//  UPDATED DATA MODEL
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Extended parking model with paid parking support.
 *
 * @param pricePerHour  null  → free parking
 *                      > 0   → paid parking (UAH per hour)
 */
data class ParkingSpot(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String = "",
    val schedule: String = "24/7",
    val totalSpots: Int = 0,
    val occupiedSpots: Int = 0,
    val pricePerHour: Double? = null   // null = free
) {
    val freeSpots: Int get() = maxOf(0, totalSpots - occupiedSpots)
    val isFull:    Boolean get() = freeSpots == 0
    val isPaid:    Boolean get() = pricePerHour != null && pricePerHour > 0.0
    val isFree:    Boolean get() = !isPaid
}

enum class FilterType { ALL, FREE_ONLY, PAID_ONLY }

enum class CheckoutResult {
    SUCCESS,
    INSUFFICIENT_BALANCE,
    NO_ACTIVE_BOOKING,
    ZERO_COST           // free parking — no charge
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT09 — ParkingSearchEngine
//  Search by name AND/OR address (partial, case-insensitive)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Searches parking lots by name and/or address.
 * A parking matches if [query] appears in its name OR address (case-insensitive).
 * Empty query returns the full list.
 */
class ParkingSearchEngine {
    fun search(list: List<ParkingSpot>, query: String): List<ParkingSpot> {
        if (query.isBlank()) return list
        val q = query.trim().lowercase()
        return list.filter {
            it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
    }
}

class ParkingSearchEngineTest {

    private lateinit var engine: ParkingSearchEngine
    private lateinit var lots: List<ParkingSpot>

    @Before
    fun setUp() {
        engine = ParkingSearchEngine()
        lots = listOf(
            ParkingSpot("p001", "Центральна",       "вул. Сумська, 12",            pricePerHour = null),
            ParkingSpot("p002", "Привокзальна",     "пл. Привокзальна, 1",         pricePerHour = 30.0),
            ParkingSpot("p003", "Університетська",  "вул. Науки, 14",              pricePerHour = null),
            ParkingSpot("p004", "Салтівська",       "просп. Салтівське шосе, 73",  pricePerHour = 20.0),
            ParkingSpot("p005", "Площа Свободи",    "пл. Свободи, 4",              pricePerHour = null),
        )
    }

    // ── UNIT09.TS01 — Search by name ───────────────────────────────────────

    /** UNIT09.TS01.TC001 — Exact name match returns 1 result */
    @Test
    fun `search by exact name returns single result`() {
        val result = engine.search(lots, "Центральна")
        assertEquals(1, result.size)
        assertEquals("Центральна", result.first().name)
    }

    /** UNIT09.TS01.TC002 — Partial name match (case-insensitive) */
    @Test
    fun `search by partial name returns all matching lots`() {
        val result = engine.search(lots, "площа")
        assertEquals(1, result.size)
        assertEquals("Площа Свободи", result.first().name)
    }

    /** UNIT09.TS01.TC003 — Uppercase query matches lowercase name */
    @Test
    fun `search is case-insensitive for names`() {
        val result = engine.search(lots, "САЛТІВСЬКА")
        assertEquals(1, result.size)
        assertEquals("Салтівська", result.first().name)
    }

    // ── UNIT09.TS02 — Search by address ────────────────────────────────────

    /** UNIT09.TS02.TC001 — Search by street name in address */
    @Test
    fun `search by street name in address returns correct lot`() {
        val result = engine.search(lots, "Сумська")
        assertEquals(1, result.size)
        assertEquals("Центральна", result.first().name)
    }

    /** UNIT09.TS02.TC002 — Search by house number in address */
    @Test
    fun `search by house number returns matching lot`() {
        val result = engine.search(lots, "14")
        assertEquals(1, result.size)
        assertEquals("Університетська", result.first().name)
    }

    /** UNIT09.TS02.TC003 — Partial address match (case-insensitive) */
    @Test
    fun `search by partial address is case-insensitive`() {
        val result = engine.search(lots, "просп")
        assertEquals(1, result.size)
        assertEquals("Салтівська", result.first().name)
    }

    /** UNIT09.TS02.TC004 — Query "пл." matches two lots by address */
    @Test
    fun `search by shared address token returns multiple results`() {
        val result = engine.search(lots, "пл.")
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Привокзальна" })
        assertTrue(result.any { it.name == "Площа Свободи" })
    }

    // ── UNIT09.TS03 — Combined / edge cases ────────────────────────────────

    /** UNIT09.TS03.TC001 — Empty query returns full list */
    @Test
    fun `search with empty query returns all lots`() {
        assertEquals(5, engine.search(lots, "").size)
    }

    /** UNIT09.TS03.TC002 — Blank query returns full list */
    @Test
    fun `search with blank spaces returns all lots`() {
        assertEquals(5, engine.search(lots, "   ").size)
    }

    /** UNIT09.TS03.TC003 — No match → empty list */
    @Test
    fun `search with no match returns empty list`() {
        assertTrue(engine.search(lots, "Дніпро").isEmpty())
    }

    /** UNIT09.TS03.TC004 — Query matches both name and address of different lots */
    @Test
    fun `query matching name of one lot and address of another returns both`() {
        // "Науки" appears in address of p003; not in any name
        val result = engine.search(lots, "Науки")
        assertEquals(1, result.size)
        assertEquals("Університетська", result.first().name)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT10 — ParkingFilter
//  Filter by FREE / PAID / ALL; combinable with search results
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Filters a parking list by [FilterType].
 * Designed to be chained after [ParkingSearchEngine.search].
 */
class ParkingFilter {
    fun filter(list: List<ParkingSpot>, type: FilterType): List<ParkingSpot> = when (type) {
        FilterType.ALL       -> list
        FilterType.FREE_ONLY -> list.filter { it.isFree }
        FilterType.PAID_ONLY -> list.filter { it.isPaid }
    }
}

class ParkingFilterTest {

    private lateinit var filter: ParkingFilter
    private lateinit var lots: List<ParkingSpot>

    @Before
    fun setUp() {
        filter = ParkingFilter()
        lots = listOf(
            ParkingSpot("p001", "Центральна",       "вул. Сумська, 12",           pricePerHour = null,  totalSpots = 48, occupiedSpots = 21),
            ParkingSpot("p002", "Привокзальна",     "пл. Привокзальна, 1",        pricePerHour = 30.0,  totalSpots = 60, occupiedSpots = 60),
            ParkingSpot("p003", "Університетська",  "вул. Науки, 14",             pricePerHour = null,  totalSpots = 36, occupiedSpots = 8),
            ParkingSpot("p004", "Салтівська",       "просп. Салтівське шосе, 73", pricePerHour = 20.0,  totalSpots = 54, occupiedSpots = 29),
            ParkingSpot("p005", "Площа Свободи",    "пл. Свободи, 4",             pricePerHour = null,  totalSpots = 42, occupiedSpots = 0),
        )
    }

    // ── UNIT10.TS01 — Filter type ───────────────────────────────────────────

    /** UNIT10.TS01.TC001 — ALL returns full list unchanged */
    @Test
    fun `filter ALL returns full list`() {
        assertEquals(5, filter.filter(lots, FilterType.ALL).size)
    }

    /** UNIT10.TS01.TC002 — FREE_ONLY returns only free lots */
    @Test
    fun `filter FREE_ONLY returns only free parking lots`() {
        val result = filter.filter(lots, FilterType.FREE_ONLY)
        assertEquals(3, result.size)
        assertTrue(result.all { it.isFree })
        assertTrue(result.none { it.isPaid })
    }

    /** UNIT10.TS01.TC003 — PAID_ONLY returns only paid lots */
    @Test
    fun `filter PAID_ONLY returns only paid parking lots`() {
        val result = filter.filter(lots, FilterType.PAID_ONLY)
        assertEquals(2, result.size)
        assertTrue(result.all { it.isPaid })
        assertTrue(result.none { it.isFree })
    }

    /** UNIT10.TS01.TC004 — FREE_ONLY on all-paid list returns empty */
    @Test
    fun `filter FREE_ONLY on all-paid list returns empty`() {
        val paidOnly = lots.filter { it.isPaid }
        assertTrue(filter.filter(paidOnly, FilterType.FREE_ONLY).isEmpty())
    }

    /** UNIT10.TS01.TC005 — PAID_ONLY on all-free list returns empty */
    @Test
    fun `filter PAID_ONLY on all-free list returns empty`() {
        val freeOnly = lots.filter { it.isFree }
        assertTrue(filter.filter(freeOnly, FilterType.PAID_ONLY).isEmpty())
    }

    // ── UNIT10.TS02 — isPaid / isFree flags ────────────────────────────────

    /** UNIT10.TS02.TC001 — pricePerHour = null → isFree = true, isPaid = false */
    @Test
    fun `parking with null pricePerHour is free`() {
        val p = ParkingSpot("x", "Test", "addr", pricePerHour = null)
        assertTrue(p.isFree)
        assertFalse(p.isPaid)
    }

    /** UNIT10.TS02.TC002 — pricePerHour > 0 → isPaid = true, isFree = false */
    @Test
    fun `parking with positive pricePerHour is paid`() {
        val p = ParkingSpot("x", "Test", "addr", pricePerHour = 25.0)
        assertTrue(p.isPaid)
        assertFalse(p.isFree)
    }

    // ── UNIT10.TS03 — Chained search + filter ──────────────────────────────

    /** UNIT10.TS03.TC001 — Search "пл." returns 2, then PAID_ONLY narrows to 1 */
    @Test
    fun `search then PAID_ONLY filter narrows results correctly`() {
        val engine   = ParkingSearchEngine()
        val searched = engine.search(lots, "пл.")        // Привокзальна + Площа Свободи
        val filtered = filter.filter(searched, FilterType.PAID_ONLY)
        assertEquals(1, filtered.size)
        assertEquals("Привокзальна", filtered.first().name)
    }

    /** UNIT10.TS03.TC002 — Search "пл." then FREE_ONLY returns 1 free lot */
    @Test
    fun `search then FREE_ONLY filter returns only free results`() {
        val engine   = ParkingSearchEngine()
        val searched = engine.search(lots, "пл.")
        val filtered = filter.filter(searched, FilterType.FREE_ONLY)
        assertEquals(1, filtered.size)
        assertEquals("Площа Свободи", filtered.first().name)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT11 — ParkingPriceValidator
//  Validate price-per-hour before saving to Firestore
// ══════════════════════════════════════════════════════════════════════════════

class ParkingPriceValidator {

    data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

    companion object {
        const val MAX_PRICE = 1_000.0   // UAH/hour sanity cap
        const val MIN_PRICE = 1.0       // minimum 1 UAH/hour if paid
    }

    /**
     * Validates the price-per-hour field from the admin "Add/Edit parking" form.
     * [rawInput] is the string entered by the admin (may be empty → free parking).
     * Returns a pair: (isValid, parsedPrice) where parsedPrice is null for free.
     */
    fun validate(rawInput: String): Pair<ValidationResult, Double?> {
        if (rawInput.isBlank()) return Pair(ValidationResult(true), null)   // free parking

        val price = rawInput.trim().replace(",", ".").toDoubleOrNull()
            ?: return Pair(ValidationResult(false, "Введіть коректне число"), null)

        if (price < MIN_PRICE)
            return Pair(ValidationResult(false, "Мінімальна ціна — ${MIN_PRICE.toInt()} грн/год"), null)
        if (price > MAX_PRICE)
            return Pair(ValidationResult(false, "Максимальна ціна — ${MAX_PRICE.toInt()} грн/год"), null)

        return Pair(ValidationResult(true), price)
    }
}

class ParkingPriceValidatorTest {

    private lateinit var validator: ParkingPriceValidator

    @Before
    fun setUp() {
        validator = ParkingPriceValidator()
    }

    // ── UNIT11.TS01 ────────────────────────────────────────────────────────

    /** UNIT11.TS01.TC001 — Empty input → free parking, isValid = true */
    @Test
    fun `empty input is valid and represents free parking`() {
        val (result, price) = validator.validate("")
        assertTrue(result.isValid)
        assertNull(price)
    }

    /** UNIT11.TS01.TC002 — Valid price "25" → isValid = true, price = 25.0 */
    @Test
    fun `valid integer price string returns correct double`() {
        val (result, price) = validator.validate("25")
        assertTrue(result.isValid)
        assertEquals(25.0, price)
    }

    /** UNIT11.TS01.TC003 — Valid decimal price "30.50" → isValid = true */
    @Test
    fun `valid decimal price string is accepted`() {
        val (result, price) = validator.validate("30.50")
        assertTrue(result.isValid)
        assertEquals(30.50, price)
    }

    /** UNIT11.TS01.TC004 — Comma decimal "20,5" → isValid = true (normalized) */
    @Test
    fun `comma as decimal separator is normalized and accepted`() {
        val (result, price) = validator.validate("20,5")
        assertTrue(result.isValid)
        assertEquals(20.5, price)
    }

    /** UNIT11.TS01.TC005 — Non-numeric input → isValid = false */
    @Test
    fun `non-numeric input returns invalid result`() {
        val (result, _) = validator.validate("abc")
        assertFalse(result.isValid)
        assertEquals("Введіть коректне число", result.errorMessage)
    }

    /** UNIT11.TS01.TC006 — Price below minimum (0.5) → isValid = false */
    @Test
    fun `price below minimum returns invalid result`() {
        val (result, _) = validator.validate("0.5")
        assertFalse(result.isValid)
        assertEquals("Мінімальна ціна — 1 грн/год", result.errorMessage)
    }

    /** UNIT11.TS01.TC007 — Price above maximum → isValid = false */
    @Test
    fun `price above maximum returns invalid result`() {
        val (result, _) = validator.validate("1001")
        assertFalse(result.isValid)
        assertEquals("Максимальна ціна — 1000 грн/год", result.errorMessage)
    }

    /** Boundary: exactly MIN_PRICE (1.0) → valid */
    @Test
    fun `price equal to minimum is valid`() {
        val (result, price) = validator.validate("1")
        assertTrue(result.isValid)
        assertEquals(1.0, price)
    }

    /** Boundary: exactly MAX_PRICE (1000.0) → valid */
    @Test
    fun `price equal to maximum is valid`() {
        val (result, price) = validator.validate("1000")
        assertTrue(result.isValid)
        assertEquals(1000.0, price)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT12 — UserBalance
//  Profile balance: top-up simulation and deductions
// ══════════════════════════════════════════════════════════════════════════════

class UserBalance(initialBalance: Double = 0.0) {

    private var balance: Double = initialBalance

    fun getBalance(): Double = balance

    /**
     * Simulates a card top-up.
     * [amount] must be positive.
     */
    fun topUp(amount: Double): Boolean {
        if (amount <= 0) return false
        balance += amount
        return true
    }

    /**
     * Deducts [amount] from balance.
     * Returns false (and does NOT deduct) if balance is insufficient.
     */
    fun deduct(amount: Double): Boolean {
        if (amount <= 0) return false
        if (balance < amount) return false
        balance -= amount
        return true
    }

    fun hasSufficientFunds(amount: Double): Boolean = balance >= amount
}

class UserBalanceTest {

    private lateinit var balance: UserBalance

    @Before
    fun setUp() {
        balance = UserBalance(initialBalance = 0.0)
    }

    // ── UNIT12.TS01 — Top-up ───────────────────────────────────────────────

    /** UNIT12.TS01.TC001 — topUp with valid amount increases balance */
    @Test
    fun `topUp with positive amount increases balance correctly`() {
        balance.topUp(100.0)
        assertEquals(100.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS01.TC002 — Multiple top-ups accumulate correctly */
    @Test
    fun `multiple topUps accumulate balance`() {
        balance.topUp(50.0)
        balance.topUp(150.0)
        balance.topUp(200.0)
        assertEquals(400.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS01.TC003 — topUp with 0 returns false and balance unchanged */
    @Test
    fun `topUp with zero returns false and does not change balance`() {
        assertFalse(balance.topUp(0.0))
        assertEquals(0.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS01.TC004 — topUp with negative amount returns false */
    @Test
    fun `topUp with negative amount returns false`() {
        assertFalse(balance.topUp(-50.0))
        assertEquals(0.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS01.TC005 — Common top-up presets: 50, 100, 200, 500 UAH */
    @Test
    fun `preset top-up amounts are all valid`() {
        listOf(50.0, 100.0, 200.0, 500.0).forEach { amount ->
            val b = UserBalance()
            assertTrue("Top-up of $amount should succeed", b.topUp(amount))
            assertEquals(amount, b.getBalance(), 0.001)
        }
    }

    // ── UNIT12.TS02 — Deduct ───────────────────────────────────────────────

    /** UNIT12.TS02.TC001 — Deduct less than balance → succeeds */
    @Test
    fun `deduct less than balance succeeds and reduces balance`() {
        balance.topUp(200.0)
        assertTrue(balance.deduct(75.0))
        assertEquals(125.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS02.TC002 — Deduct exact balance → succeeds, balance = 0 */
    @Test
    fun `deduct exact balance amount succeeds and leaves zero balance`() {
        balance.topUp(100.0)
        assertTrue(balance.deduct(100.0))
        assertEquals(0.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS02.TC003 — Deduct more than balance → fails, balance unchanged */
    @Test
    fun `deduct more than balance returns false and does not change balance`() {
        balance.topUp(50.0)
        assertFalse(balance.deduct(100.0))
        assertEquals(50.0, balance.getBalance(), 0.001)
    }

    /** UNIT12.TS02.TC004 — Deduct from empty balance → fails */
    @Test
    fun `deduct from zero balance returns false`() {
        assertFalse(balance.deduct(10.0))
        assertEquals(0.0, balance.getBalance(), 0.001)
    }

    // ── UNIT12.TS03 — hasSufficientFunds ───────────────────────────────────

    /** UNIT12.TS03.TC001 — hasSufficientFunds returns true when balance covers cost */
    @Test
    fun `hasSufficientFunds returns true when balance is enough`() {
        balance.topUp(100.0)
        assertTrue(balance.hasSufficientFunds(99.99))
    }

    /** UNIT12.TS03.TC002 — hasSufficientFunds returns false when balance is short */
    @Test
    fun `hasSufficientFunds returns false when balance is insufficient`() {
        balance.topUp(50.0)
        assertFalse(balance.hasSufficientFunds(50.01))
    }

    /** UNIT12.TS03.TC003 — hasSufficientFunds returns true for exact amount */
    @Test
    fun `hasSufficientFunds returns true for exact balance match`() {
        balance.topUp(75.0)
        assertTrue(balance.hasSufficientFunds(75.0))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT13 — TariffCalculator
//  Calculate cost based on elapsed seconds and price per hour
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Calculates parking cost based on elapsed time.
 *
 * Billing rules:
 * - Minimum billable unit: 1 minute (округлення вгору до хвилини)
 * - Cost = ceil(elapsedSeconds / 60) * (pricePerHour / 60)
 * - Free parkings (pricePerHour = null or 0) → cost = 0.0
 */
class TariffCalculator {

    /**
     * @param elapsedSeconds  Duration of the parking session in seconds
     * @param pricePerHour    Price in UAH per hour; null or 0 = free
     * @return                Cost in UAH, rounded to 2 decimal places
     */
    fun calculate(elapsedSeconds: Long, pricePerHour: Double?): Double {
        if (pricePerHour == null || pricePerHour <= 0.0) return 0.0
        if (elapsedSeconds <= 0) return 0.0

        val pricePerMinute = pricePerHour / 60.0
        val minutes = Math.ceil(elapsedSeconds / 60.0)
        val raw = minutes * pricePerMinute
        return Math.round(raw * 100.0) / 100.0   // round to 2 decimal places
    }

    /**
     * Returns a human-readable cost preview string.
     * e.g. "37.50 грн" or "Безкоштовно"
     */
    fun formatCost(cost: Double): String =
        if (cost <= 0.0) "Безкоштовно" else "%.2f грн".format(cost)
}

class TariffCalculatorTest {

    private lateinit var calc: TariffCalculator

    @Before
    fun setUp() {
        calc = TariffCalculator()
    }

    // ── UNIT13.TS01 — Free parking ─────────────────────────────────────────

    /** UNIT13.TS01.TC001 — null price → cost = 0.0 */
    @Test
    fun `calculate returns zero for null pricePerHour`() {
        assertEquals(0.0, calc.calculate(3600, null), 0.001)
    }

    /** UNIT13.TS01.TC002 — pricePerHour = 0.0 → cost = 0.0 */
    @Test
    fun `calculate returns zero for zero pricePerHour`() {
        assertEquals(0.0, calc.calculate(3600, 0.0), 0.001)
    }

    /** UNIT13.TS01.TC003 — elapsed = 0 → cost = 0.0 regardless of price */
    @Test
    fun `calculate returns zero when elapsed time is zero`() {
        assertEquals(0.0, calc.calculate(0, 60.0), 0.001)
    }

    // ── UNIT13.TS02 — Exact hour increments ────────────────────────────────

    /** UNIT13.TS02.TC001 — Exactly 1 hour at 60 UAH/h → 60.0 UAH */
    @Test
    fun `calculate 1 hour at 60 UAH per hour returns 60 UAH`() {
        assertEquals(60.0, calc.calculate(3600, 60.0), 0.001)
    }

    /** UNIT13.TS02.TC002 — Exactly 2 hours at 30 UAH/h → 60.0 UAH */
    @Test
    fun `calculate 2 hours at 30 UAH per hour returns 60 UAH`() {
        assertEquals(60.0, calc.calculate(7200, 30.0), 0.001)
    }

    // ── UNIT13.TS03 — Per-minute rounding ──────────────────────────────────

    /** UNIT13.TS03.TC001 — 30 min at 60 UAH/h → 30.0 UAH */
    @Test
    fun `calculate 30 minutes at 60 UAH per hour returns 30 UAH`() {
        assertEquals(30.0, calc.calculate(1800, 60.0), 0.001)
    }

    /** UNIT13.TS03.TC002 — 1 min exactly at 60 UAH/h → 1.0 UAH */
    @Test
    fun `calculate exactly 1 minute at 60 UAH per hour returns 1 UAH`() {
        assertEquals(1.0, calc.calculate(60, 60.0), 0.001)
    }

    /** UNIT13.TS03.TC003 — 61 seconds → rounds UP to 2 minutes */
    @Test
    fun `calculate 61 seconds rounds up to 2 billable minutes`() {
        // 2 min * (60/60) = 2.0 UAH
        assertEquals(2.0, calc.calculate(61, 60.0), 0.001)
    }

    /** UNIT13.TS03.TC004 — 1 second → rounds UP to 1 billable minute */
    @Test
    fun `calculate 1 second rounds up to 1 billable minute`() {
        assertEquals(1.0, calc.calculate(1, 60.0), 0.001)
    }

    /** UNIT13.TS03.TC005 — 90 min at 20 UAH/h → 30.0 UAH */
    @Test
    fun `calculate 90 minutes at 20 UAH per hour returns 30 UAH`() {
        assertEquals(30.0, calc.calculate(5400, 20.0), 0.001)
    }

    // ── UNIT13.TS04 — Real tariffs from seed data ───────────────────────────

    /** UNIT13.TS04.TC001 — Привокзальна 30 UAH/h, 45 min → 22.50 UAH */
    @Test
    fun `Pryvokazalna 30 UAH per hour for 45 minutes costs 22_50 UAH`() {
        assertEquals(22.50, calc.calculate(2700, 30.0), 0.001)
    }

    /** UNIT13.TS04.TC002 — Салтівська 20 UAH/h, 1 h 10 min → 23.34 UAH */
    @Test
    fun `Saltivska 20 UAH per hour for 70 minutes costs 23_34 UAH`() {
        // ceil(4200/60) = 70 min; 70 * (20/60) = 23.3333 → 23.33
        assertEquals(23.33, calc.calculate(4200, 20.0), 0.001)
    }

    // ── UNIT13.TS05 — formatCost ────────────────────────────────────────────

    /** UNIT13.TS05.TC001 — 0.0 → "Безкоштовно" */
    @Test
    fun `formatCost returns Безкоштовно for zero cost`() {
        assertEquals("Безкоштовно", calc.formatCost(0.0))
    }

    /** UNIT13.TS05.TC002 — 37.5 → "37.50 грн" */
    @Test
    fun `formatCost formats non-zero cost with two decimal places`() {
        assertEquals("37.50 грн", calc.formatCost(37.5))
    }

    /** UNIT13.TS05.TC003 — 100.0 → "100.00 грн" */
    @Test
    fun `formatCost formats round number with two decimal places`() {
        assertEquals("100.00 грн", calc.formatCost(100.0))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  UNIT14 — CheckoutManager
//  Combines TariffCalculator + UserBalance at end of session
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Orchestrates the checkout at end of a parking session:
 * 1. Calculates cost via [TariffCalculator]
 * 2. Checks balance via [UserBalance]
 * 3. Deducts amount or returns INSUFFICIENT_BALANCE
 */
class CheckoutManager(
    private val calculator: TariffCalculator = TariffCalculator()
) {
    data class CheckoutSummary(
        val result: CheckoutResult,
        val cost: Double,
        val remainingBalance: Double
    )

    fun checkout(
        elapsedSeconds: Long,
        pricePerHour: Double?,
        balance: UserBalance
    ): CheckoutSummary {
        val cost = calculator.calculate(elapsedSeconds, pricePerHour)

        // Free parking — no charge needed
        if (cost == 0.0) {
            return CheckoutSummary(CheckoutResult.ZERO_COST, 0.0, balance.getBalance())
        }

        if (!balance.hasSufficientFunds(cost)) {
            return CheckoutSummary(
                CheckoutResult.INSUFFICIENT_BALANCE, cost, balance.getBalance()
            )
        }

        balance.deduct(cost)
        return CheckoutSummary(CheckoutResult.SUCCESS, cost, balance.getBalance())
    }
}

class CheckoutManagerTest {

    private lateinit var manager: CheckoutManager

    @Before
    fun setUp() {
        manager = CheckoutManager()
    }

    // ── UNIT14.TS01 — Free parking checkout ────────────────────────────────

    /** UNIT14.TS01.TC001 — Free parking → ZERO_COST, balance unchanged */
    @Test
    fun `checkout free parking returns ZERO_COST and does not touch balance`() {
        val balance = UserBalance(100.0)
        val summary = manager.checkout(3600, null, balance)
        assertEquals(CheckoutResult.ZERO_COST, summary.result)
        assertEquals(0.0, summary.cost, 0.001)
        assertEquals(100.0, summary.remainingBalance, 0.001)
    }

    // ── UNIT14.TS02 — Successful paid checkout ─────────────────────────────

    /** UNIT14.TS02.TC001 — Sufficient balance → SUCCESS, balance reduced */
    @Test
    fun `checkout with sufficient balance returns SUCCESS and deducts cost`() {
        val balance = UserBalance(200.0)
        // 1 hour at 60 UAH/h = 60 UAH
        val summary = manager.checkout(3600, 60.0, balance)
        assertEquals(CheckoutResult.SUCCESS, summary.result)
        assertEquals(60.0, summary.cost, 0.001)
        assertEquals(140.0, summary.remainingBalance, 0.001)
    }

    /** UNIT14.TS02.TC002 — Balance exactly covers cost → SUCCESS, balance = 0 */
    @Test
    fun `checkout with balance equal to cost returns SUCCESS and leaves zero balance`() {
        val balance = UserBalance(30.0)
        // 30 min at 60 UAH/h = 30 UAH
        val summary = manager.checkout(1800, 60.0, balance)
        assertEquals(CheckoutResult.SUCCESS, summary.result)
        assertEquals(30.0, summary.cost, 0.001)
        assertEquals(0.0, summary.remainingBalance, 0.001)
    }

    // ── UNIT14.TS03 — Insufficient balance ─────────────────────────────────

    /** UNIT14.TS03.TC001 — Balance too low → INSUFFICIENT_BALANCE, not deducted */
    @Test
    fun `checkout with insufficient balance returns INSUFFICIENT_BALANCE`() {
        val balance = UserBalance(10.0)
        // 1 hour at 60 UAH/h = 60 UAH — too expensive
        val summary = manager.checkout(3600, 60.0, balance)
        assertEquals(CheckoutResult.INSUFFICIENT_BALANCE, summary.result)
        assertEquals(60.0, summary.cost, 0.001)
        assertEquals(10.0, summary.remainingBalance, 0.001) // unchanged
    }

    /** UNIT14.TS03.TC002 — Empty balance → INSUFFICIENT_BALANCE */
    @Test
    fun `checkout with zero balance returns INSUFFICIENT_BALANCE`() {
        val balance = UserBalance(0.0)
        val summary = manager.checkout(3600, 30.0, balance)
        assertEquals(CheckoutResult.INSUFFICIENT_BALANCE, summary.result)
    }

    // ── UNIT14.TS04 — Real scenario ────────────────────────────────────────

    /** UNIT14.TS04.TC001 — User tops up 100, parks 45 min at 30 UAH/h (22.50), remainder 77.50 */
    @Test
    fun `full scenario - top up then park and checkout leaves correct remainder`() {
        val balance = UserBalance(0.0)
        balance.topUp(100.0)

        // Привокзальна: 30 UAH/h, 45 min session
        val summary = manager.checkout(2700, 30.0, balance)
        assertEquals(CheckoutResult.SUCCESS, summary.result)
        assertEquals(22.50, summary.cost, 0.001)
        assertEquals(77.50, summary.remainingBalance, 0.001)
    }

    /** UNIT14.TS04.TC002 — Two sequential sessions deduct correctly */
    @Test
    fun `two sequential checkouts deduct from same balance correctly`() {
        val balance = UserBalance(100.0)

        // Session 1: 30 min at 20 UAH/h = 10.0 UAH
        val s1 = manager.checkout(1800, 20.0, balance)
        assertEquals(CheckoutResult.SUCCESS, s1.result)
        assertEquals(10.0, s1.cost, 0.001)
        assertEquals(90.0, s1.remainingBalance, 0.001)

        // Session 2: 1 hour at 60 UAH/h = 60.0 UAH
        val s2 = manager.checkout(3600, 60.0, balance)
        assertEquals(CheckoutResult.SUCCESS, s2.result)
        assertEquals(60.0, s2.cost, 0.001)
        assertEquals(30.0, s2.remainingBalance, 0.001)
    }
}
