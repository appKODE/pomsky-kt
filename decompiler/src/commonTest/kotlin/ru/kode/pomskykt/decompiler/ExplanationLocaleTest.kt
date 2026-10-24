package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplanationLocaleTest {

    @Test
    fun defaultLocaleIsEnglish() {
        assertEquals(
            Decompiler.explain("\\d+", RegexFlavor.Java).explanation,
            Decompiler.explain("\\d+", RegexFlavor.Java, ExplainLocale.EN).explanation,
        )
    }

    @Test
    fun russianLiteral() {
        assertEquals("\"hello\"", explainRu("hello"))
    }

    @Test
    fun russianCharClass() {
        assertEquals("символ из набора: буква (a-z), цифра (0-9)", explainRu("[a-z\\d]"))
    }

    @Test
    fun russianRepetition() {
        assertEquals("1 или более раз: цифра (0-9)", explainRu("\\d+"))
        assertEquals("ровно 4 раза: цифра (0-9)", explainRu("\\d{4}"))
        assertEquals("ровно 5 раз: \"ab\"", explainRu("(?:ab){5}"))
        assertEquals("от 2 до 3 раз: любой символ", explainRu(".{2,3}"))
    }

    @Test
    fun russianAlternation() {
        assertEquals("либо \"feat\", \"fix\", либо \"docs\"", explainRu("feat|fix|docs"))
    }

    @Test
    fun russianNamedGroup() {
        assertEquals("ровно 4 раза: цифра (0-9) (сохраняется как \"year\")", explainRu("(?P<year>\\d{4})"))
    }

    @Test
    fun russianAnchors() {
        assertEquals(
            "начало строки, затем 1 или более раз: буква (a-z), затем конец строки",
            explainRu("^[a-z]+\$"),
        )
    }

    private fun explainRu(regex: String): String? {
        val result = Decompiler.explain(regex, RegexFlavor.Java, ExplainLocale.RU)
        assertNull(result.error, "Explain error: ${result.error}")
        return result.explanation
    }
}
