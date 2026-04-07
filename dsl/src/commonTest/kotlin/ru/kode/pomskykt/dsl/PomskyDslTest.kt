package ru.kode.pomskykt.dsl

import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PomskyDslTest {

    @Test
    fun simpleLiteral() {
        val pattern = pomsky { literal("hello") }
        assertEquals("hello", pattern.toRegex())
    }

    @Test
    fun startEndAnchors() {
        val pattern = pomsky {
            start
            literal("test")
            end
        }
        assertEquals("^test\$", pattern.toRegex())
    }

    @Test
    fun digitPlus() {
        val pattern = pomsky {
            oneOrMore { digit }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("\\d") || regex.contains("[0-9]"))
        assertTrue(regex.contains("+"))
    }

    @Test
    fun charRange() {
        val pattern = pomsky { charRange('a', 'z') }
        assertEquals("[a-z]", pattern.toRegex())
    }

    @Test
    fun alternation() {
        val pattern = pomsky {
            either(
                { literal("foo") },
                { literal("bar") },
            )
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("foo"))
        assertTrue(regex.contains("bar"))
        assertTrue(regex.contains("|"))
    }

    @Test
    fun namedCapture() {
        val pattern = pomsky {
            capture("name") { word }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("name"))
        assertTrue(regex.contains("\\w"))
    }

    @Test
    fun numberedCapture() {
        val pattern = pomsky {
            capture { digit }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("("))
    }

    @Test
    fun optionalWithLiteral() {
        val pattern = pomsky {
            optional { literal("prefix") }
            literal("body")
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("?"))
        assertTrue(regex.contains("body"))
    }

    @Test
    fun toKotlinRegex() {
        val pattern = pomsky {
            start
            literal("hello")
            end
        }
        val regex = pattern.toKotlinRegex()
        assertTrue(regex.matches("hello"))
        assertTrue(!regex.matches("hello world"))
    }

    @Test
    fun complexEmailLikePattern() {
        val pattern = pomsky {
            start
            oneOrMore { word }
            literal("@")
            oneOrMore { word }
            literal(".")
            repeat(2, 4) { charRange('a', 'z') }
            end
        }
        val regex = pattern.toKotlinRegex()
        assertTrue(regex.matches("user@host.com"))
        assertTrue(!regex.matches("@host.com"))
        assertTrue(!regex.matches("user@.com"))
    }

    @Test
    fun zeroOrMore() {
        val pattern = pomsky {
            zeroOrMore { digit }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("*"))
    }

    @Test
    fun repeatExact() {
        val pattern = pomsky {
            repeat(3) { digit }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("{3}"))
    }

    @Test
    fun atLeast() {
        val pattern = pomsky {
            atLeast(2) { digit }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("{2,}"))
    }

    @Test
    fun lookahead() {
        val pattern = pomsky {
            lookahead { literal("foo") }
            oneOrMore { word }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("(?="))
    }

    @Test
    fun negativeLookahead() {
        val pattern = pomsky {
            negativeLookahead { literal("bar") }
            oneOrMore { word }
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("(?!"))
    }

    @Test
    fun wordBoundary() {
        val pattern = pomsky {
            wordBoundary
            literal("test")
            wordBoundary
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("\\b"))
    }

    @Test
    fun rawExpression() {
        val pattern = pomsky {
            raw("[digit]+")
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("\\d") || regex.contains("[0-9]"))
    }

    @Test
    fun charClass() {
        val pattern = pomsky {
            charClass('a', 'b', 'c')
        }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("["))
        assertTrue(regex.contains("]"))
    }

    @Test
    fun patternSource() {
        val pattern = pomsky {
            literal("hello")
            literal("world")
        }
        assertEquals("'hello' 'world'", pattern.source)
        assertEquals("'hello' 'world'", pattern.toString())
    }

    @Test
    fun pcreFlavor() {
        val pattern = pomsky { literal("test") }
        assertEquals("test", pattern.toRegex(RegexFlavor.Pcre))
    }

    @Test
    fun group() {
        val pattern = pomsky {
            group {
                literal("a")
                literal("b")
            }
        }
        val regex = pattern.toRegex()
        assertNotNull(regex)
        assertTrue(regex.contains("a"))
        assertTrue(regex.contains("b"))
    }

    @Test
    fun spaceClass() {
        val pattern = pomsky { space }
        val regex = pattern.toRegex()
        assertTrue(regex.contains("\\s"))
    }

    @Test
    fun dotAny() {
        val pattern = pomsky { any }
        val regex = pattern.toRegex()
        assertEquals(".", regex)
    }
}
