package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RangeTest {

    @Test fun range0to9() = assertRange("range '0'-'9'", "[0-9]")

    @Test fun range0to99() = assertRange("range '0'-'99'", "0|[1-9][0-9]?")

    @Test fun range0to57() = assertRange(
        "range '0'-'57'",
        "0|[1-4][0-9]?|5[0-7]?|[6-9]",
    )

    @Test fun range0to75() = assertRange(
        "range '0'-'75'",
        "0|[1-6][0-9]?|7[0-5]?|[89]",
    )

    @Test fun range0to255() = assertRange(
        "range '0'-'255'",
        "0|1[0-9]{0,2}|2(?:[0-4][0-9]?|5[0-5]?|[6-9])?|[3-9][0-9]?",
    )

    @Test fun range0to471() = assertRange(
        "range '0'-'471'",
        "0|[1-3][0-9]{0,2}|4(?:[0-6][0-9]?|7[01]?|[89])?|[5-9][0-9]?",
    )

    @Test fun range0to741() = assertRange(
        "range '0'-'741'",
        "0|[1-6][0-9]{0,2}|7(?:[0-3][0-9]?|4[01]?|[5-9])?|[89][0-9]?",
    )

    @Test fun range5to70() = assertRange(
        "range '5'-'70'",
        "[1-4][0-9]|[56][0-9]?|70?|[89]",
    )

    @Test fun range70to500() = assertRange(
        "range '70'-'500'",
        "[1-4][0-9]{2}|50{2}|[7-9][0-9]",
    )

    @Test fun range3918to4918() = assertRange(
        "range '3918'-'4918'",
        "39(?:1[89]|[2-9][0-9])|4(?:[0-8][0-9]{2}|9(?:0[0-9]|1[0-8]))",
    )

    @Test fun range000to471() = assertRange(
        "range '000'-'471'",
        "[0-3][0-9]{2}|4(?:[0-6][0-9]|7[01])",
    )

    @Test fun range00018to09180() = assertRange(
        "range '00018'-'09180'",
        "0(?:0(?:0(?:1[89]|[2-9][0-9])|[1-9][0-9]{2})|[1-8][0-9]{3}|9(?:0[0-9]{2}|1(?:[0-7][0-9]|80)))",
    )

    @Test fun range03918to49180() = assertRange(
        "range '03918'-'49180'",
        "0(?:39(?:1[89]|[2-9][0-9])|[4-9][0-9]{3})|[1-3][0-9]{4}|4(?:[0-8][0-9]{3}|9(?:0[0-9]{2}|1(?:[0-7][0-9]|80)))",
    )

    @Test fun range3918to49180() = assertRange(
        "range '3918'-'49180'",
        "[12][0-9]{4}|3(?:[0-8][0-9]{3}|9(?:0[0-9]{2}|1(?:[0-7][0-9]|[89][0-9]?)|[2-9][0-9]{1,2}))|4(?:[0-8][0-9]{2,3}|9(?:0[0-9]{1,2}|1(?:[0-7][0-9]?|80?|9)|[2-9][0-9]))|[5-9][0-9]{3}",
    )

    @Test fun range3900to491999() = assertRange(
        "range '3900'-'491999'",
        "[12][0-9]{4,5}|3(?:[0-8][0-9]{3,4}|9[0-9]{2,4})|4(?:[0-8][0-9]{2,4}|9(?:[01][0-9]{1,3}|[2-9][0-9]{1,2}))|[5-9][0-9]{3,4}",
    )

    @Test fun rangeBase16() = assertRange(
        """range "0"-"10FFFF" base 16""",
        "0|1(?:0[0-9A-Fa-f]{0,4}|[1-9A-Fa-f][0-9A-Fa-f]{0,3})?|[2-9A-Fa-f][0-9A-Fa-f]{0,4}",
    )

    // --- Helpers ---

    private fun assertRange(input: String, expected: String) {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions())
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        assertEquals(expected, result, "Range output mismatch for: $input")
    }
}
