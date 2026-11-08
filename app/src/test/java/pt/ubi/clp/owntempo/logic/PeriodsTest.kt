package pt.ubi.clp.owntempo.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodsTest {

    // Saturday 7 Nov 2026; the 1st of November 2026 is a Sunday
    private val anchor = LocalDate.of(2026, 11, 7)

    @Test
    fun weekStartsOnMonday() {
        assertEquals(LocalDate.of(2026, 11, 2), gridStart(ViewMode.WEEK, anchor))
    }

    @Test
    fun monthGridStartsOnTheMondayBeforeTheFirst() {
        assertEquals(LocalDate.of(2026, 10, 26), gridStart(ViewMode.MONTH, anchor))
    }

    @Test
    fun gridLengths() {
        assertEquals(7, gridLength(ViewMode.WEEK))
        assertEquals(42, gridLength(ViewMode.MONTH))
    }

    @Test
    fun shiftingAMonthKeepsAValidDay() {
        assertEquals(LocalDate.of(2026, 2, 28), shiftAnchor(ViewMode.MONTH, LocalDate.of(2026, 1, 31), 1))
        assertEquals(LocalDate.of(2026, 11, 14), shiftAnchor(ViewMode.WEEK, anchor, 1))
    }
}