package ru.kode.pomskykt.syntax.unicode

/** Other Unicode boolean properties. */
enum class OtherProperties(val fullName: String) {
    ASCII("ASCII"),
    ASCIIHexDigit("ASCII_Hex_Digit"),
    Alphabetic("Alphabetic"),
    Any("Any"),
    Assigned("Assigned"),
    BidiControl("Bidi_Control"),
    BidiMirrored("Bidi_Mirrored"),
    CaseIgnorable("Case_Ignorable"),
    Cased("Cased"),
    ChangesWhenCasefolded("Changes_When_Casefolded"),
    ChangesWhenCasemapped("Changes_When_Casemapped"),
    ChangesWhenLowercased("Changes_When_Lowercased"),
    ChangesWhenNFKCCasefolded("Changes_When_NFKC_Casefolded"),
    ChangesWhenTitlecased("Changes_When_Titlecased"),
    ChangesWhenUppercased("Changes_When_Uppercased"),
    Dash("Dash"),
    DefaultIgnorableCodePoint("Default_Ignorable_Code_Point"),
    Deprecated("Deprecated"),
    Diacritic("Diacritic"),
    Emoji("Emoji"),
    EmojiComponent("Emoji_Component"),
    EmojiModifier("Emoji_Modifier"),
    EmojiModifierBase("Emoji_Modifier_Base"),
    EmojiPresentation("Emoji_Presentation"),
    ExtendedPictographic("Extended_Pictographic"),
    Extender("Extender"),
    GraphemeBase("Grapheme_Base"),
    GraphemeExtend("Grapheme_Extend"),
    HexDigit("Hex_Digit"),
    IDSBinaryOperator("IDS_Binary_Operator"),
    IDSTrinaryOperator("IDS_Trinary_Operator"),
    IDContinue("ID_Continue"),
    IDStart("ID_Start"),
    Ideographic("Ideographic"),
    JoinControl("Join_Control"),
    LogicalOrderException("Logical_Order_Exception"),
    Lowercase("Lowercase"),
    Math("Math"),
    NoncharacterCodePoint("Noncharacter_Code_Point"),
    PatternSyntax("Pattern_Syntax"),
    PatternWhiteSpace("Pattern_White_Space"),
    QuotationMark("Quotation_Mark"),
    Radical("Radical"),
    RegionalIndicator("Regional_Indicator"),
    SentenceTerminal("Sentence_Terminal"),
    SoftDotted("Soft_Dotted"),
    TerminalPunctuation("Terminal_Punctuation"),
    UnifiedIdeograph("Unified_Ideograph"),
    Uppercase("Uppercase"),
    VariationSelector("Variation_Selector"),
    WhiteSpace("White_Space"),
    XIDContinue("XID_Continue"),
    XIDStart("XID_Start"),
    ;

    companion object {
        private val byName: Map<String, OtherProperties> by lazy {
            buildMap {
                put("ASCII", ASCII)
                put("AHex", ASCIIHexDigit)
                put("ASCII_Hex_Digit", ASCIIHexDigit)
                put("Alpha", Alphabetic)
                put("Alphabetic", Alphabetic)
                put("Any", Any)
                put("Assigned", Assigned)
                put("Bidi_C", BidiControl)
                put("Bidi_Control", BidiControl)
                put("Bidi_M", BidiMirrored)
                put("Bidi_Mirrored", BidiMirrored)
                put("CI", CaseIgnorable)
                put("Case_Ignorable", CaseIgnorable)
                put("Cased", Cased)
                put("CWCF", ChangesWhenCasefolded)
                put("Changes_When_Casefolded", ChangesWhenCasefolded)
                put("CWCM", ChangesWhenCasemapped)
                put("Changes_When_Casemapped", ChangesWhenCasemapped)
                put("CWL", ChangesWhenLowercased)
                put("Changes_When_Lowercased", ChangesWhenLowercased)
                put("CWKCF", ChangesWhenNFKCCasefolded)
                put("Changes_When_NFKC_Casefolded", ChangesWhenNFKCCasefolded)
                put("CWT", ChangesWhenTitlecased)
                put("Changes_When_Titlecased", ChangesWhenTitlecased)
                put("CWU", ChangesWhenUppercased)
                put("Changes_When_Uppercased", ChangesWhenUppercased)
                put("Dash", Dash)
                put("DI", DefaultIgnorableCodePoint)
                put("Default_Ignorable_Code_Point", DefaultIgnorableCodePoint)
                put("Dep", Deprecated)
                put("Deprecated", Deprecated)
                put("Dia", Diacritic)
                put("Diacritic", Diacritic)
                put("Emoji", Emoji)
                put("EComp", EmojiComponent)
                put("Emoji_Component", EmojiComponent)
                put("EMod", EmojiModifier)
                put("Emoji_Modifier", EmojiModifier)
                put("EBase", EmojiModifierBase)
                put("Emoji_Modifier_Base", EmojiModifierBase)
                put("EPres", EmojiPresentation)
                put("Emoji_Presentation", EmojiPresentation)
                put("ExtPict", ExtendedPictographic)
                put("Extended_Pictographic", ExtendedPictographic)
                put("Ext", Extender)
                put("Extender", Extender)
                put("Gr_Base", GraphemeBase)
                put("Grapheme_Base", GraphemeBase)
                put("Gr_Ext", GraphemeExtend)
                put("Grapheme_Extend", GraphemeExtend)
                put("Hex", HexDigit)
                put("Hex_Digit", HexDigit)
                put("IDSB", IDSBinaryOperator)
                put("IDS_Binary_Operator", IDSBinaryOperator)
                put("IDST", IDSTrinaryOperator)
                put("IDS_Trinary_Operator", IDSTrinaryOperator)
                put("IDC", IDContinue)
                put("ID_Continue", IDContinue)
                put("IDS", IDStart)
                put("ID_Start", IDStart)
                put("Ideo", Ideographic)
                put("Ideographic", Ideographic)
                put("Join_C", JoinControl)
                put("Join_Control", JoinControl)
                put("LOE", LogicalOrderException)
                put("Logical_Order_Exception", LogicalOrderException)
                put("Lower", Lowercase)
                put("Lowercase", Lowercase)
                put("Math", Math)
                put("NChar", NoncharacterCodePoint)
                put("Noncharacter_Code_Point", NoncharacterCodePoint)
                put("Pat_Syn", PatternSyntax)
                put("Pattern_Syntax", PatternSyntax)
                put("Pat_WS", PatternWhiteSpace)
                put("Pattern_White_Space", PatternWhiteSpace)
                put("QMark", QuotationMark)
                put("Quotation_Mark", QuotationMark)
                put("Radical", Radical)
                put("RI", RegionalIndicator)
                put("Regional_Indicator", RegionalIndicator)
                put("STerm", SentenceTerminal)
                put("Sentence_Terminal", SentenceTerminal)
                put("SD", SoftDotted)
                put("Soft_Dotted", SoftDotted)
                put("Term", TerminalPunctuation)
                put("Terminal_Punctuation", TerminalPunctuation)
                put("UIdeo", UnifiedIdeograph)
                put("Unified_Ideograph", UnifiedIdeograph)
                put("Upper", Uppercase)
                put("Uppercase", Uppercase)
                put("VS", VariationSelector)
                put("Variation_Selector", VariationSelector)
                put("space", WhiteSpace)
                put("White_Space", WhiteSpace)
                put("XIDC", XIDContinue)
                put("XID_Continue", XIDContinue)
                put("XIDS", XIDStart)
                put("XID_Start", XIDStart)
            }
        }

        fun fromName(name: String): OtherProperties? = byName[name]

        fun aliases(prop: OtherProperties): List<String> = when (prop) {
            ASCII -> listOf("ASCII")
            ASCIIHexDigit -> listOf("AHex", "ASCII_Hex_Digit")
            Alphabetic -> listOf("Alpha", "Alphabetic")
            Any -> listOf("Any")
            Assigned -> listOf("Assigned")
            BidiControl -> listOf("Bidi_C", "Bidi_Control")
            BidiMirrored -> listOf("Bidi_M", "Bidi_Mirrored")
            CaseIgnorable -> listOf("CI", "Case_Ignorable")
            Cased -> listOf("Cased")
            ChangesWhenCasefolded -> listOf("CWCF", "Changes_When_Casefolded")
            ChangesWhenCasemapped -> listOf("CWCM", "Changes_When_Casemapped")
            ChangesWhenLowercased -> listOf("CWL", "Changes_When_Lowercased")
            ChangesWhenNFKCCasefolded -> listOf("CWKCF", "Changes_When_NFKC_Casefolded")
            ChangesWhenTitlecased -> listOf("CWT", "Changes_When_Titlecased")
            ChangesWhenUppercased -> listOf("CWU", "Changes_When_Uppercased")
            Dash -> listOf("Dash")
            DefaultIgnorableCodePoint -> listOf("DI", "Default_Ignorable_Code_Point")
            Deprecated -> listOf("Dep", "Deprecated")
            Diacritic -> listOf("Dia", "Diacritic")
            Emoji -> listOf("Emoji")
            EmojiComponent -> listOf("EComp", "Emoji_Component")
            EmojiModifier -> listOf("EMod", "Emoji_Modifier")
            EmojiModifierBase -> listOf("EBase", "Emoji_Modifier_Base")
            EmojiPresentation -> listOf("EPres", "Emoji_Presentation")
            ExtendedPictographic -> listOf("ExtPict", "Extended_Pictographic")
            Extender -> listOf("Ext", "Extender")
            GraphemeBase -> listOf("Gr_Base", "Grapheme_Base")
            GraphemeExtend -> listOf("Gr_Ext", "Grapheme_Extend")
            HexDigit -> listOf("Hex", "Hex_Digit")
            IDSBinaryOperator -> listOf("IDSB", "IDS_Binary_Operator")
            IDSTrinaryOperator -> listOf("IDST", "IDS_Trinary_Operator")
            IDContinue -> listOf("IDC", "ID_Continue")
            IDStart -> listOf("IDS", "ID_Start")
            Ideographic -> listOf("Ideo", "Ideographic")
            JoinControl -> listOf("Join_C", "Join_Control")
            LogicalOrderException -> listOf("LOE", "Logical_Order_Exception")
            Lowercase -> listOf("Lower", "Lowercase")
            Math -> listOf("Math")
            NoncharacterCodePoint -> listOf("NChar", "Noncharacter_Code_Point")
            PatternSyntax -> listOf("Pat_Syn", "Pattern_Syntax")
            PatternWhiteSpace -> listOf("Pat_WS", "Pattern_White_Space")
            QuotationMark -> listOf("QMark", "Quotation_Mark")
            Radical -> listOf("Radical")
            RegionalIndicator -> listOf("RI", "Regional_Indicator")
            SentenceTerminal -> listOf("STerm", "Sentence_Terminal")
            SoftDotted -> listOf("SD", "Soft_Dotted")
            TerminalPunctuation -> listOf("Term", "Terminal_Punctuation")
            UnifiedIdeograph -> listOf("UIdeo", "Unified_Ideograph")
            Uppercase -> listOf("Upper", "Uppercase")
            VariationSelector -> listOf("VS", "Variation_Selector")
            WhiteSpace -> listOf("space", "White_Space")
            XIDContinue -> listOf("XIDC", "XID_Continue")
            XIDStart -> listOf("XIDS", "XID_Start")
        }
    }
}
