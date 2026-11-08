package pt.ubi.clp.owntempo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun keys_areUniqueAndNotBlank() {
        val keys = Category.entries.map { it.key }
        assertTrue(keys.all { it.isNotBlank() })
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun fromKey_findsEveryCategory() {
        Category.entries.forEach { assertEquals(it, Category.fromKey(it.key)) }
    }

    @Test
    fun fromKey_unknownKeyFallsBackToGym() {
        assertEquals(Category.GYM, Category.fromKey("something-old"))
    }
}