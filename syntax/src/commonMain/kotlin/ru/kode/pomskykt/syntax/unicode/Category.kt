package ru.kode.pomskykt.syntax.unicode

/** Unicode general category (gc). */
enum class Category(val fullName: String, val abbreviation: String) {
    Other("Other", "C"),
    Control("Control", "Cc"),
    Format("Format", "Cf"),
    Unassigned("Unassigned", "Cn"),
    PrivateUse("Private_Use", "Co"),
    Surrogate("Surrogate", "Cs"),
    Letter("Letter", "L"),
    CasedLetter("Cased_Letter", "LC"),
    LowercaseLetter("Lowercase_Letter", "Ll"),
    ModifierLetter("Modifier_Letter", "Lm"),
    OtherLetter("Other_Letter", "Lo"),
    TitlecaseLetter("Titlecase_Letter", "Lt"),
    UppercaseLetter("Uppercase_Letter", "Lu"),
    Mark("Mark", "M"),
    SpacingMark("Spacing_Mark", "Mc"),
    EnclosingMark("Enclosing_Mark", "Me"),
    NonspacingMark("Nonspacing_Mark", "Mn"),
    Number("Number", "N"),
    DecimalNumber("Decimal_Number", "Nd"),
    LetterNumber("Letter_Number", "Nl"),
    OtherNumber("Other_Number", "No"),
    Punctuation("Punctuation", "P"),
    ConnectorPunctuation("Connector_Punctuation", "Pc"),
    DashPunctuation("Dash_Punctuation", "Pd"),
    ClosePunctuation("Close_Punctuation", "Pe"),
    FinalPunctuation("Final_Punctuation", "Pf"),
    InitialPunctuation("Initial_Punctuation", "Pi"),
    OtherPunctuation("Other_Punctuation", "Po"),
    OpenPunctuation("Open_Punctuation", "Ps"),
    Symbol("Symbol", "S"),
    CurrencySymbol("Currency_Symbol", "Sc"),
    ModifierSymbol("Modifier_Symbol", "Sk"),
    MathSymbol("Math_Symbol", "Sm"),
    OtherSymbol("Other_Symbol", "So"),
    Separator("Separator", "Z"),
    LineSeparator("Line_Separator", "Zl"),
    ParagraphSeparator("Paragraph_Separator", "Zp"),
    SpaceSeparator("Space_Separator", "Zs"),
    ;

    companion object {
        private val byName: Map<String, Category> by lazy {
            buildMap {
                put("C", Other)
                put("Other", Other)
                put("Cc", Control)
                put("Control", Control)
                put("cntrl", Control)
                put("Cf", Format)
                put("Format", Format)
                put("Cn", Unassigned)
                put("Unassigned", Unassigned)
                put("Co", PrivateUse)
                put("Private_Use", PrivateUse)
                put("Cs", Surrogate)
                put("Surrogate", Surrogate)
                put("L", Letter)
                put("Letter", Letter)
                put("LC", CasedLetter)
                put("Cased_Letter", CasedLetter)
                put("Ll", LowercaseLetter)
                put("Lowercase_Letter", LowercaseLetter)
                put("Lm", ModifierLetter)
                put("Modifier_Letter", ModifierLetter)
                put("Lo", OtherLetter)
                put("Other_Letter", OtherLetter)
                put("Lt", TitlecaseLetter)
                put("Titlecase_Letter", TitlecaseLetter)
                put("Lu", UppercaseLetter)
                put("Uppercase_Letter", UppercaseLetter)
                put("M", Mark)
                put("Mark", Mark)
                put("Combining_Mark", Mark)
                put("Mc", SpacingMark)
                put("Spacing_Mark", SpacingMark)
                put("Me", EnclosingMark)
                put("Enclosing_Mark", EnclosingMark)
                put("Mn", NonspacingMark)
                put("Nonspacing_Mark", NonspacingMark)
                put("N", Number)
                put("Number", Number)
                put("Nd", DecimalNumber)
                put("Decimal_Number", DecimalNumber)
                put("digit", DecimalNumber)
                put("Nl", LetterNumber)
                put("Letter_Number", LetterNumber)
                put("No", OtherNumber)
                put("Other_Number", OtherNumber)
                put("P", Punctuation)
                put("Punctuation", Punctuation)
                put("punct", Punctuation)
                put("Pc", ConnectorPunctuation)
                put("Connector_Punctuation", ConnectorPunctuation)
                put("Pd", DashPunctuation)
                put("Dash_Punctuation", DashPunctuation)
                put("Pe", ClosePunctuation)
                put("Close_Punctuation", ClosePunctuation)
                put("Pf", FinalPunctuation)
                put("Final_Punctuation", FinalPunctuation)
                put("Pi", InitialPunctuation)
                put("Initial_Punctuation", InitialPunctuation)
                put("Po", OtherPunctuation)
                put("Other_Punctuation", OtherPunctuation)
                put("Ps", OpenPunctuation)
                put("Open_Punctuation", OpenPunctuation)
                put("S", Symbol)
                put("Symbol", Symbol)
                put("Sc", CurrencySymbol)
                put("Currency_Symbol", CurrencySymbol)
                put("Sk", ModifierSymbol)
                put("Modifier_Symbol", ModifierSymbol)
                put("Sm", MathSymbol)
                put("Math_Symbol", MathSymbol)
                put("So", OtherSymbol)
                put("Other_Symbol", OtherSymbol)
                put("Z", Separator)
                put("Separator", Separator)
                put("Zl", LineSeparator)
                put("Line_Separator", LineSeparator)
                put("Zp", ParagraphSeparator)
                put("Paragraph_Separator", ParagraphSeparator)
                put("Zs", SpaceSeparator)
                put("Space_Separator", SpaceSeparator)
            }
        }

        fun fromName(name: String): Category? = byName[name]

        fun aliases(cat: Category): List<String> = when (cat) {
            Other -> listOf("C", "Other")
            Control -> listOf("Cc", "Control", "cntrl")
            Format -> listOf("Cf", "Format")
            Unassigned -> listOf("Cn", "Unassigned")
            PrivateUse -> listOf("Co", "Private_Use")
            Surrogate -> listOf("Cs", "Surrogate")
            Letter -> listOf("L", "Letter")
            CasedLetter -> listOf("LC", "Cased_Letter")
            LowercaseLetter -> listOf("Ll", "Lowercase_Letter")
            ModifierLetter -> listOf("Lm", "Modifier_Letter")
            OtherLetter -> listOf("Lo", "Other_Letter")
            TitlecaseLetter -> listOf("Lt", "Titlecase_Letter")
            UppercaseLetter -> listOf("Lu", "Uppercase_Letter")
            Mark -> listOf("M", "Mark", "Combining_Mark")
            SpacingMark -> listOf("Mc", "Spacing_Mark")
            EnclosingMark -> listOf("Me", "Enclosing_Mark")
            NonspacingMark -> listOf("Mn", "Nonspacing_Mark")
            Number -> listOf("N", "Number")
            DecimalNumber -> listOf("Nd", "Decimal_Number", "digit")
            LetterNumber -> listOf("Nl", "Letter_Number")
            OtherNumber -> listOf("No", "Other_Number")
            Punctuation -> listOf("P", "Punctuation", "punct")
            ConnectorPunctuation -> listOf("Pc", "Connector_Punctuation")
            DashPunctuation -> listOf("Pd", "Dash_Punctuation")
            ClosePunctuation -> listOf("Pe", "Close_Punctuation")
            FinalPunctuation -> listOf("Pf", "Final_Punctuation")
            InitialPunctuation -> listOf("Pi", "Initial_Punctuation")
            OtherPunctuation -> listOf("Po", "Other_Punctuation")
            OpenPunctuation -> listOf("Ps", "Open_Punctuation")
            Symbol -> listOf("S", "Symbol")
            CurrencySymbol -> listOf("Sc", "Currency_Symbol")
            ModifierSymbol -> listOf("Sk", "Modifier_Symbol")
            MathSymbol -> listOf("Sm", "Math_Symbol")
            OtherSymbol -> listOf("So", "Other_Symbol")
            Separator -> listOf("Z", "Separator")
            LineSeparator -> listOf("Zl", "Line_Separator")
            ParagraphSeparator -> listOf("Zp", "Paragraph_Separator")
            SpaceSeparator -> listOf("Zs", "Space_Separator")
        }
    }
}
