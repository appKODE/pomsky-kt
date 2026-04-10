package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatternLibraryTest {
    // Test each pattern compiles successfully
    @Test fun emailCompiles() = assertPatternCompiles("email")
    @Test fun semverCompiles() = assertPatternCompiles("semver")
    @Test fun uuidCompiles() = assertPatternCompiles("uuid")
    @Test fun dateIso8601Compiles() = assertPatternCompiles("date_iso8601")
    @Test fun time24hCompiles() = assertPatternCompiles("time_24h")
    @Test fun hexColorCompiles() = assertPatternCompiles("hex_color")
    @Test fun ipv4Compiles() = assertPatternCompiles("ipv4")
    @Test fun jiraTicketCompiles() = assertPatternCompiles("jira_ticket")
    @Test fun conventionalCommitCompiles() = assertPatternCompiles("conventional_commit")

    // Test regex matches expected strings
    @Test
    fun emailMatchesValid() {
        val regex = compilePattern("email")
        assertTrue(Regex(regex).containsMatchIn("user@example.com"))
    }

    @Test
    fun semverMatchesValid() {
        val regex = compilePattern("semver")
        assertTrue(Regex(regex).containsMatchIn("1.2.3"))
        assertTrue(Regex(regex).containsMatchIn("1.0.0-beta.1"))
    }

    @Test
    fun uuidMatchesValid() {
        val regex = compilePattern("uuid")
        assertTrue(Regex(regex).containsMatchIn("550e8400-e29b-41d4-a716-446655440000"))
    }

    @Test
    fun jiraTicketMatchesValid() {
        val regex = compilePattern("jira_ticket")
        assertTrue(Regex(regex).containsMatchIn("MAPS-123"))
        assertTrue(Regex(regex).containsMatchIn("AB-1"))
    }

    @Test
    fun conventionalCommitMatchesValid() {
        val regex = compilePattern("conventional_commit")
        assertTrue(Regex(regex).containsMatchIn("feat(auth): add OAuth"))
        assertTrue(Regex(regex).containsMatchIn("fix!: critical bug"))
    }

    // Test user variable overrides library
    @Test
    fun userVariableOverridesLibrary() {
        val (result, diags, _) = Expr.parseAndCompile(
            "let email = 'custom'; email",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(diags.none { it.severity == Severity.Error })
        assertTrue(result == "custom", "Expected 'custom' but got: $result")
    }

    // Test disabled library
    @Test
    fun disabledLibraryRejectsPatternNames() {
        val (result, diags, _) = Expr.parseAndCompile(
            "email",
            CompileOptions(flavor = RegexFlavor.Java, patternLibraryEnabled = false),
        )
        assertNull(result)
        assertTrue(diags.any { it.severity == Severity.Error })
    }

    // --- Helpers ---

    private fun assertPatternCompiles(name: String) {
        val (result, diags, _) = Expr.parseAndCompile(
            name,
            CompileOptions(flavor = RegexFlavor.Java),
        )
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Pattern '$name' failed to compile: ${errors.map { it.msg }}")
        assertNotNull(result, "Pattern '$name' produced null result")
    }

    private fun compilePattern(name: String): String {
        val (result, _, _) = Expr.parseAndCompile(
            name,
            CompileOptions(flavor = RegexFlavor.Java),
        )
        return result!!
    }
}
