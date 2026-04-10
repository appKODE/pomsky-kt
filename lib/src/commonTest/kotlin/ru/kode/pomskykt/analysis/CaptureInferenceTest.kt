package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CaptureInferenceTest {
    @Test
    fun namedGroupsGenerateProperties() {
        val result = CaptureInference.inferGroups(
            ":year([digit]{4}) '-' :month([digit]{2}) '-' :day([digit]{2})",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertEquals(3, result.groups.size)
        assertTrue(result.groups.any { it.propertyName == "year" })
        assertTrue(result.groups.any { it.propertyName == "month" })
        assertTrue(result.groups.any { it.propertyName == "day" })
        assertTrue(result.code.contains("data class MatchGroups"))
        assertTrue(result.code.contains("val year: String"))
    }

    @Test
    fun unnamedGroupsGenerateNumberedProperties() {
        val result = CaptureInference.inferGroups(
            ":('hello') '-' :('world')",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertEquals(2, result.groups.size)
        assertTrue(result.code.contains("group"))
    }

    @Test
    fun noCapturesReturnsNull() {
        val result = CaptureInference.inferGroups(
            "'hello' 'world'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNull(result)
    }

    @Test
    fun customClassNameUsed() {
        val result = CaptureInference.inferGroups(
            ":year([digit]{4})",
            CompileOptions(flavor = RegexFlavor.Java),
            className = "DateMatch",
        )
        assertNotNull(result)
        assertTrue(result.code.contains("data class DateMatch"))
        assertEquals("DateMatch", result.className)
    }

    @Test
    fun generatedCodeHasExtractFunction() {
        val result = CaptureInference.inferGroups(
            ":name([word]+)",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(result.code.contains("fun Regex.extractMatchGroups"))
        assertTrue(result.code.contains("match.groupValues"))
    }

    @Test
    fun snakeCaseNameConvertsToCamelCase() {
        val result = CaptureInference.inferGroups(
            ":first_name([word]+) ' ' :last_name([word]+)",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(result.groups.any { it.propertyName == "firstName" })
        assertTrue(result.groups.any { it.propertyName == "lastName" })
    }
}
