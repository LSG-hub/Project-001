package app.project001.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryTest {
    private val india = Country("IN", 91, "India", "India")
    private val ivory = Country("CI", 225, "Côte d’Ivoire", "Côte d’Ivoire")
    private val germany = Country("DE", 49, "Deutschland", "Germany")

    @Test fun `flag from region letters`() {
        assertEquals("🇮🇳", flagOf("IN"))
        assertEquals("🇬🇧", flagOf("gb"))
    }

    @Test fun `search by name, english name, code or region`() {
        assertTrue(india.matches("ind"))
        assertTrue(india.matches("+91"))
        assertTrue(india.matches("9"))
        assertFalse(india.matches("+44"))
        assertTrue(ivory.matches("cote"))
        assertTrue(germany.matches("germ"))
        assertTrue(germany.matches("de"))
        assertTrue(india.matches("  "))
    }
}
