package ru.kode.pomskykt.syntax.unicode

/** Unicode code block (blk). */
enum class CodeBlock(val fullName: String) {
    BasicLatin("Basic_Latin"),
    Latin1Supplement("Latin_1_Supplement"),
    LatinExtendedA("Latin_Extended_A"),
    LatinExtendedB("Latin_Extended_B"),
    IPAExtensions("IPA_Extensions"),
    SpacingModifierLetters("Spacing_Modifier_Letters"),
    CombiningDiacriticalMarks("Combining_Diacritical_Marks"),
    GreekandCoptic("Greek_and_Coptic"),
    Cyrillic("Cyrillic"),
    CyrillicSupplement("Cyrillic_Supplement"),
    Armenian("Armenian"),
    Hebrew("Hebrew"),
    Arabic("Arabic"),
    Syriac("Syriac"),
    ArabicSupplement("Arabic_Supplement"),
    Thaana("Thaana"),
    NKo("NKo"),
    Samaritan("Samaritan"),
    Mandaic("Mandaic"),
    SyriacSupplement("Syriac_Supplement"),
    ArabicExtendedB("Arabic_Extended_B"),
    ArabicExtendedA("Arabic_Extended_A"),
    Devanagari("Devanagari"),
    Bengali("Bengali"),
    Gurmukhi("Gurmukhi"),
    Gujarati("Gujarati"),
    Oriya("Oriya"),
    Tamil("Tamil"),
    Telugu("Telugu"),
    Kannada("Kannada"),
    Malayalam("Malayalam"),
    Sinhala("Sinhala"),
    Thai("Thai"),
    Lao("Lao"),
    Tibetan("Tibetan"),
    Myanmar("Myanmar"),
    Georgian("Georgian"),
    HangulJamo("Hangul_Jamo"),
    Ethiopic("Ethiopic"),
    EthiopicSupplement("Ethiopic_Supplement"),
    Cherokee("Cherokee"),
    UnifiedCanadianAboriginalSyllabics("Unified_Canadian_Aboriginal_Syllabics"),
    Ogham("Ogham"),
    Runic("Runic"),
    Tagalog("Tagalog"),
    Hanunoo("Hanunoo"),
    Buhid("Buhid"),
    Tagbanwa("Tagbanwa"),
    Khmer("Khmer"),
    Mongolian("Mongolian"),
    UnifiedCanadianAboriginalSyllabicsExtended("Unified_Canadian_Aboriginal_Syllabics_Extended"),
    Limbu("Limbu"),
    TaiLe("Tai_Le"),
    NewTaiLue("New_Tai_Lue"),
    KhmerSymbols("Khmer_Symbols"),
    Buginese("Buginese"),
    TaiTham("Tai_Tham"),
    CombiningDiacriticalMarksExtended("Combining_Diacritical_Marks_Extended"),
    Balinese("Balinese"),
    Sundanese("Sundanese"),
    Batak("Batak"),
    Lepcha("Lepcha"),
    OlChiki("Ol_Chiki"),
    CyrillicExtendedC("Cyrillic_Extended_C"),
    GeorgianExtended("Georgian_Extended"),
    SundaneseSupplement("Sundanese_Supplement"),
    VedicExtensions("Vedic_Extensions"),
    PhoneticExtensions("Phonetic_Extensions"),
    PhoneticExtensionsSupplement("Phonetic_Extensions_Supplement"),
    CombiningDiacriticalMarksSupplement("Combining_Diacritical_Marks_Supplement"),
    LatinExtendedAdditional("Latin_Extended_Additional"),
    GreekExtended("Greek_Extended"),
    GeneralPunctuation("General_Punctuation"),
    SuperscriptsandSubscripts("Superscripts_and_Subscripts"),
    CurrencySymbols("Currency_Symbols"),
    CombiningDiacriticalMarksforSymbols("Combining_Diacritical_Marks_for_Symbols"),
    LetterlikeSymbols("Letterlike_Symbols"),
    NumberForms("Number_Forms"),
    Arrows("Arrows"),
    MathematicalOperators("Mathematical_Operators"),
    MiscellaneousTechnical("Miscellaneous_Technical"),
    ControlPictures("Control_Pictures"),
    OpticalCharacterRecognition("Optical_Character_Recognition"),
    EnclosedAlphanumerics("Enclosed_Alphanumerics"),
    BoxDrawing("Box_Drawing"),
    BlockElements("Block_Elements"),
    GeometricShapes("Geometric_Shapes"),
    MiscellaneousSymbols("Miscellaneous_Symbols"),
    Dingbats("Dingbats"),
    MiscellaneousMathematicalSymbolsA("Miscellaneous_Mathematical_Symbols_A"),
    SupplementalArrowsA("Supplemental_Arrows_A"),
    BraillePatterns("Braille_Patterns"),
    SupplementalArrowsB("Supplemental_Arrows_B"),
    MiscellaneousMathematicalSymbolsB("Miscellaneous_Mathematical_Symbols_B"),
    SupplementalMathematicalOperators("Supplemental_Mathematical_Operators"),
    MiscellaneousSymbolsandArrows("Miscellaneous_Symbols_and_Arrows"),
    Glagolitic("Glagolitic"),
    LatinExtendedC("Latin_Extended_C"),
    Coptic("Coptic"),
    GeorgianSupplement("Georgian_Supplement"),
    Tifinagh("Tifinagh"),
    EthiopicExtended("Ethiopic_Extended"),
    CyrillicExtendedA("Cyrillic_Extended_A"),
    SupplementalPunctuation("Supplemental_Punctuation"),
    CJKRadicalsSupplement("CJK_Radicals_Supplement"),
    KangxiRadicals("Kangxi_Radicals"),
    IdeographicDescriptionCharacters("Ideographic_Description_Characters"),
    CJKSymbolsandPunctuation("CJK_Symbols_and_Punctuation"),
    Hiragana("Hiragana"),
    Katakana("Katakana"),
    Bopomofo("Bopomofo"),
    HangulCompatibilityJamo("Hangul_Compatibility_Jamo"),
    Kanbun("Kanbun"),
    BopomofoExtended("Bopomofo_Extended"),
    CJKStrokes("CJK_Strokes"),
    KatakanaPhoneticExtensions("Katakana_Phonetic_Extensions"),
    EnclosedCJKLettersandMonths("Enclosed_CJK_Letters_and_Months"),
    CJKCompatibility("CJK_Compatibility"),
    CJKUnifiedIdeographsExtensionA("CJK_Unified_Ideographs_Extension_A"),
    YijingHexagramSymbols("Yijing_Hexagram_Symbols"),
    CJKUnifiedIdeographs("CJK_Unified_Ideographs"),
    YiSyllables("Yi_Syllables"),
    YiRadicals("Yi_Radicals"),
    Lisu("Lisu"),
    Vai("Vai"),
    CyrillicExtendedB("Cyrillic_Extended_B"),
    Bamum("Bamum"),
    ModifierToneLetters("Modifier_Tone_Letters"),
    LatinExtendedD("Latin_Extended_D"),
    SylotiNagri("Syloti_Nagri"),
    CommonIndicNumberForms("Common_Indic_Number_Forms"),
    Phagspa("Phags_pa"),
    Saurashtra("Saurashtra"),
    DevanagariExtended("Devanagari_Extended"),
    KayahLi("Kayah_Li"),
    Rejang("Rejang"),
    HangulJamoExtendedA("Hangul_Jamo_Extended_A"),
    Javanese("Javanese"),
    MyanmarExtendedB("Myanmar_Extended_B"),
    Cham("Cham"),
    MyanmarExtendedA("Myanmar_Extended_A"),
    TaiViet("Tai_Viet"),
    MeeteiMayekExtensions("Meetei_Mayek_Extensions"),
    EthiopicExtendedA("Ethiopic_Extended_A"),
    LatinExtendedE("Latin_Extended_E"),
    CherokeeSupplement("Cherokee_Supplement"),
    MeeteiMayek("Meetei_Mayek"),
    HangulSyllables("Hangul_Syllables"),
    HangulJamoExtendedB("Hangul_Jamo_Extended_B"),
    HighSurrogates("High_Surrogates"),
    HighPrivateUseSurrogates("High_Private_Use_Surrogates"),
    LowSurrogates("Low_Surrogates"),
    PrivateUseArea("Private_Use_Area"),
    CJKCompatibilityIdeographs("CJK_Compatibility_Ideographs"),
    AlphabeticPresentationForms("Alphabetic_Presentation_Forms"),
    ArabicPresentationFormsA("Arabic_Presentation_Forms_A"),
    VariationSelectors("Variation_Selectors"),
    VerticalForms("Vertical_Forms"),
    CombiningHalfMarks("Combining_Half_Marks"),
    CJKCompatibilityForms("CJK_Compatibility_Forms"),
    SmallFormVariants("Small_Form_Variants"),
    ArabicPresentationFormsB("Arabic_Presentation_Forms_B"),
    HalfwidthandFullwidthForms("Halfwidth_and_Fullwidth_Forms"),
    Specials("Specials"),
    LinearBSyllabary("Linear_B_Syllabary"),
    LinearBIdeograms("Linear_B_Ideograms"),
    AegeanNumbers("Aegean_Numbers"),
    AncientGreekNumbers("Ancient_Greek_Numbers"),
    AncientSymbols("Ancient_Symbols"),
    PhaistosDisc("Phaistos_Disc"),
    Lycian("Lycian"),
    Carian("Carian"),
    CopticEpactNumbers("Coptic_Epact_Numbers"),
    OldItalic("Old_Italic"),
    Gothic("Gothic"),
    OldPermic("Old_Permic"),
    Ugaritic("Ugaritic"),
    OldPersian("Old_Persian"),
    Deseret("Deseret"),
    Shavian("Shavian"),
    Osmanya("Osmanya"),
    Osage("Osage"),
    Elbasan("Elbasan"),
    CaucasianAlbanian("Caucasian_Albanian"),
    Vithkuqi("Vithkuqi"),
    LinearA("Linear_A"),
    LatinExtendedF("Latin_Extended_F"),
    CypriotSyllabary("Cypriot_Syllabary"),
    ImperialAramaic("Imperial_Aramaic"),
    Palmyrene("Palmyrene"),
    Nabataean("Nabataean"),
    Hatran("Hatran"),
    Phoenician("Phoenician"),
    Lydian("Lydian"),
    MeroiticHieroglyphs("Meroitic_Hieroglyphs"),
    MeroiticCursive("Meroitic_Cursive"),
    Kharoshthi("Kharoshthi"),
    OldSouthArabian("Old_South_Arabian"),
    OldNorthArabian("Old_North_Arabian"),
    Manichaean("Manichaean"),
    Avestan("Avestan"),
    InscriptionalParthian("Inscriptional_Parthian"),
    InscriptionalPahlavi("Inscriptional_Pahlavi"),
    PsalterPahlavi("Psalter_Pahlavi"),
    OldTurkic("Old_Turkic"),
    OldHungarian("Old_Hungarian"),
    HanifiRohingya("Hanifi_Rohingya"),
    RumiNumeralSymbols("Rumi_Numeral_Symbols"),
    Yezidi("Yezidi"),
    ArabicExtendedC("Arabic_Extended_C"),
    OldSogdian("Old_Sogdian"),
    Sogdian("Sogdian"),
    OldUyghur("Old_Uyghur"),
    Chorasmian("Chorasmian"),
    Elymaic("Elymaic"),
    Brahmi("Brahmi"),
    Kaithi("Kaithi"),
    SoraSompeng("Sora_Sompeng"),
    Chakma("Chakma"),
    Mahajani("Mahajani"),
    Sharada("Sharada"),
    SinhalaArchaicNumbers("Sinhala_Archaic_Numbers"),
    Khojki("Khojki"),
    Multani("Multani"),
    Khudawadi("Khudawadi"),
    Grantha("Grantha"),
    Newa("Newa"),
    Tirhuta("Tirhuta"),
    Siddham("Siddham"),
    Modi("Modi"),
    MongolianSupplement("Mongolian_Supplement"),
    Takri("Takri"),
    Ahom("Ahom"),
    Dogra("Dogra"),
    WarangCiti("Warang_Citi"),
    DivesAkuru("Dives_Akuru"),
    Nandinagari("Nandinagari"),
    ZanabazarSquare("Zanabazar_Square"),
    Soyombo("Soyombo"),
    UnifiedCanadianAboriginalSyllabicsExtendedA("Unified_Canadian_Aboriginal_Syllabics_Extended_A"),
    PauCinHau("Pau_Cin_Hau"),
    DevanagariExtendedA("Devanagari_Extended_A"),
    Bhaiksuki("Bhaiksuki"),
    Marchen("Marchen"),
    MasaramGondi("Masaram_Gondi"),
    GunjalaGondi("Gunjala_Gondi"),
    Makasar("Makasar"),
    Kawi("Kawi"),
    LisuSupplement("Lisu_Supplement"),
    TamilSupplement("Tamil_Supplement"),
    Cuneiform("Cuneiform"),
    CuneiformNumbersandPunctuation("Cuneiform_Numbers_and_Punctuation"),
    EarlyDynasticCuneiform("Early_Dynastic_Cuneiform"),
    CyproMinoan("Cypro_Minoan"),
    EgyptianHieroglyphs("Egyptian_Hieroglyphs"),
    EgyptianHieroglyphFormatControls("Egyptian_Hieroglyph_Format_Controls"),
    AnatolianHieroglyphs("Anatolian_Hieroglyphs"),
    BamumSupplement("Bamum_Supplement"),
    Mro("Mro"),
    Tangsa("Tangsa"),
    BassaVah("Bassa_Vah"),
    PahawhHmong("Pahawh_Hmong"),
    Medefaidrin("Medefaidrin"),
    Miao("Miao"),
    IdeographicSymbolsandPunctuation("Ideographic_Symbols_and_Punctuation"),
    Tangut("Tangut"),
    TangutComponents("Tangut_Components"),
    KhitanSmallScript("Khitan_Small_Script"),
    TangutSupplement("Tangut_Supplement"),
    KanaExtendedB("Kana_Extended_B"),
    KanaSupplement("Kana_Supplement"),
    KanaExtendedA("Kana_Extended_A"),
    SmallKanaExtension("Small_Kana_Extension"),
    Nushu("Nushu"),
    Duployan("Duployan"),
    ShorthandFormatControls("Shorthand_Format_Controls"),
    ZnamennyMusicalNotation("Znamenny_Musical_Notation"),
    ByzantineMusicalSymbols("Byzantine_Musical_Symbols"),
    MusicalSymbols("Musical_Symbols"),
    AncientGreekMusicalNotation("Ancient_Greek_Musical_Notation"),
    KaktovikNumerals("Kaktovik_Numerals"),
    MayanNumerals("Mayan_Numerals"),
    TaiXuanJingSymbols("Tai_Xuan_Jing_Symbols"),
    CountingRodNumerals("Counting_Rod_Numerals"),
    MathematicalAlphanumericSymbols("Mathematical_Alphanumeric_Symbols"),
    SuttonSignWriting("Sutton_SignWriting"),
    LatinExtendedG("Latin_Extended_G"),
    GlagoliticSupplement("Glagolitic_Supplement"),
    CyrillicExtendedD("Cyrillic_Extended_D"),
    NyiakengPuachueHmong("Nyiakeng_Puachue_Hmong"),
    Toto("Toto"),
    Wancho("Wancho"),
    NagMundari("Nag_Mundari"),
    EthiopicExtendedB("Ethiopic_Extended_B"),
    MendeKikakui("Mende_Kikakui"),
    Adlam("Adlam"),
    IndicSiyaqNumbers("Indic_Siyaq_Numbers"),
    OttomanSiyaqNumbers("Ottoman_Siyaq_Numbers"),
    ArabicMathematicalAlphabeticSymbols("Arabic_Mathematical_Alphabetic_Symbols"),
    MahjongTiles("Mahjong_Tiles"),
    DominoTiles("Domino_Tiles"),
    PlayingCards("Playing_Cards"),
    EnclosedAlphanumericSupplement("Enclosed_Alphanumeric_Supplement"),
    EnclosedIdeographicSupplement("Enclosed_Ideographic_Supplement"),
    MiscellaneousSymbolsandPictographs("Miscellaneous_Symbols_and_Pictographs"),
    Emoticons("Emoticons"),
    OrnamentalDingbats("Ornamental_Dingbats"),
    TransportandMapSymbols("Transport_and_Map_Symbols"),
    AlchemicalSymbols("Alchemical_Symbols"),
    GeometricShapesExtended("Geometric_Shapes_Extended"),
    SupplementalArrowsC("Supplemental_Arrows_C"),
    SupplementalSymbolsandPictographs("Supplemental_Symbols_and_Pictographs"),
    ChessSymbols("Chess_Symbols"),
    SymbolsandPictographsExtendedA("Symbols_and_Pictographs_Extended_A"),
    SymbolsforLegacyComputing("Symbols_for_Legacy_Computing"),
    CJKUnifiedIdeographsExtensionB("CJK_Unified_Ideographs_Extension_B"),
    CJKUnifiedIdeographsExtensionC("CJK_Unified_Ideographs_Extension_C"),
    CJKUnifiedIdeographsExtensionD("CJK_Unified_Ideographs_Extension_D"),
    CJKUnifiedIdeographsExtensionE("CJK_Unified_Ideographs_Extension_E"),
    CJKUnifiedIdeographsExtensionF("CJK_Unified_Ideographs_Extension_F"),
    CJKCompatibilityIdeographsSupplement("CJK_Compatibility_Ideographs_Supplement"),
    CJKUnifiedIdeographsExtensionG("CJK_Unified_Ideographs_Extension_G"),
    CJKUnifiedIdeographsExtensionH("CJK_Unified_Ideographs_Extension_H"),
    Tags("Tags"),
    VariationSelectorsSupplement("Variation_Selectors_Supplement"),
    SupplementaryPrivateUseAreaA("Supplementary_Private_Use_Area_A"),
    SupplementaryPrivateUseAreaB("Supplementary_Private_Use_Area_B"),
    ;

    companion object {
        private val byName: Map<String, CodeBlock> by lazy {
            buildMap {
                put("Basic_Latin", BasicLatin)
                put("Latin_1_Supplement", Latin1Supplement)
                put("Latin_Extended_A", LatinExtendedA)
                put("Latin_Extended_B", LatinExtendedB)
                put("IPA_Extensions", IPAExtensions)
                put("Spacing_Modifier_Letters", SpacingModifierLetters)
                put("Combining_Diacritical_Marks", CombiningDiacriticalMarks)
                put("Greek_and_Coptic", GreekandCoptic)
                put("Cyrillic", Cyrillic)
                put("Cyrillic_Supplement", CyrillicSupplement)
                put("Armenian", Armenian)
                put("Hebrew", Hebrew)
                put("Arabic", Arabic)
                put("Syriac", Syriac)
                put("Arabic_Supplement", ArabicSupplement)
                put("Thaana", Thaana)
                put("NKo", NKo)
                put("Samaritan", Samaritan)
                put("Mandaic", Mandaic)
                put("Syriac_Supplement", SyriacSupplement)
                put("Arabic_Extended_B", ArabicExtendedB)
                put("Arabic_Extended_A", ArabicExtendedA)
                put("Devanagari", Devanagari)
                put("Bengali", Bengali)
                put("Gurmukhi", Gurmukhi)
                put("Gujarati", Gujarati)
                put("Oriya", Oriya)
                put("Tamil", Tamil)
                put("Telugu", Telugu)
                put("Kannada", Kannada)
                put("Malayalam", Malayalam)
                put("Sinhala", Sinhala)
                put("Thai", Thai)
                put("Lao", Lao)
                put("Tibetan", Tibetan)
                put("Myanmar", Myanmar)
                put("Georgian", Georgian)
                put("Hangul_Jamo", HangulJamo)
                put("Ethiopic", Ethiopic)
                put("Ethiopic_Supplement", EthiopicSupplement)
                put("Cherokee", Cherokee)
                put("Unified_Canadian_Aboriginal_Syllabics", UnifiedCanadianAboriginalSyllabics)
                put("Ogham", Ogham)
                put("Runic", Runic)
                put("Tagalog", Tagalog)
                put("Hanunoo", Hanunoo)
                put("Buhid", Buhid)
                put("Tagbanwa", Tagbanwa)
                put("Khmer", Khmer)
                put("Mongolian", Mongolian)
                put("Unified_Canadian_Aboriginal_Syllabics_Extended", UnifiedCanadianAboriginalSyllabicsExtended)
                put("Limbu", Limbu)
                put("Tai_Le", TaiLe)
                put("New_Tai_Lue", NewTaiLue)
                put("Khmer_Symbols", KhmerSymbols)
                put("Buginese", Buginese)
                put("Tai_Tham", TaiTham)
                put("Combining_Diacritical_Marks_Extended", CombiningDiacriticalMarksExtended)
                put("Balinese", Balinese)
                put("Sundanese", Sundanese)
                put("Batak", Batak)
                put("Lepcha", Lepcha)
                put("Ol_Chiki", OlChiki)
                put("Cyrillic_Extended_C", CyrillicExtendedC)
                put("Georgian_Extended", GeorgianExtended)
                put("Sundanese_Supplement", SundaneseSupplement)
                put("Vedic_Extensions", VedicExtensions)
                put("Phonetic_Extensions", PhoneticExtensions)
                put("Phonetic_Extensions_Supplement", PhoneticExtensionsSupplement)
                put("Combining_Diacritical_Marks_Supplement", CombiningDiacriticalMarksSupplement)
                put("Latin_Extended_Additional", LatinExtendedAdditional)
                put("Greek_Extended", GreekExtended)
                put("General_Punctuation", GeneralPunctuation)
                put("Superscripts_and_Subscripts", SuperscriptsandSubscripts)
                put("Currency_Symbols", CurrencySymbols)
                put("Combining_Diacritical_Marks_for_Symbols", CombiningDiacriticalMarksforSymbols)
                put("Letterlike_Symbols", LetterlikeSymbols)
                put("Number_Forms", NumberForms)
                put("Arrows", Arrows)
                put("Mathematical_Operators", MathematicalOperators)
                put("Miscellaneous_Technical", MiscellaneousTechnical)
                put("Control_Pictures", ControlPictures)
                put("Optical_Character_Recognition", OpticalCharacterRecognition)
                put("Enclosed_Alphanumerics", EnclosedAlphanumerics)
                put("Box_Drawing", BoxDrawing)
                put("Block_Elements", BlockElements)
                put("Geometric_Shapes", GeometricShapes)
                put("Miscellaneous_Symbols", MiscellaneousSymbols)
                put("Dingbats", Dingbats)
                put("Miscellaneous_Mathematical_Symbols_A", MiscellaneousMathematicalSymbolsA)
                put("Supplemental_Arrows_A", SupplementalArrowsA)
                put("Braille_Patterns", BraillePatterns)
                put("Supplemental_Arrows_B", SupplementalArrowsB)
                put("Miscellaneous_Mathematical_Symbols_B", MiscellaneousMathematicalSymbolsB)
                put("Supplemental_Mathematical_Operators", SupplementalMathematicalOperators)
                put("Miscellaneous_Symbols_and_Arrows", MiscellaneousSymbolsandArrows)
                put("Glagolitic", Glagolitic)
                put("Latin_Extended_C", LatinExtendedC)
                put("Coptic", Coptic)
                put("Georgian_Supplement", GeorgianSupplement)
                put("Tifinagh", Tifinagh)
                put("Ethiopic_Extended", EthiopicExtended)
                put("Cyrillic_Extended_A", CyrillicExtendedA)
                put("Supplemental_Punctuation", SupplementalPunctuation)
                put("CJK_Radicals_Supplement", CJKRadicalsSupplement)
                put("Kangxi_Radicals", KangxiRadicals)
                put("Ideographic_Description_Characters", IdeographicDescriptionCharacters)
                put("CJK_Symbols_and_Punctuation", CJKSymbolsandPunctuation)
                put("Hiragana", Hiragana)
                put("Katakana", Katakana)
                put("Bopomofo", Bopomofo)
                put("Hangul_Compatibility_Jamo", HangulCompatibilityJamo)
                put("Kanbun", Kanbun)
                put("Bopomofo_Extended", BopomofoExtended)
                put("CJK_Strokes", CJKStrokes)
                put("Katakana_Phonetic_Extensions", KatakanaPhoneticExtensions)
                put("Enclosed_CJK_Letters_and_Months", EnclosedCJKLettersandMonths)
                put("CJK_Compatibility", CJKCompatibility)
                put("CJK_Unified_Ideographs_Extension_A", CJKUnifiedIdeographsExtensionA)
                put("Yijing_Hexagram_Symbols", YijingHexagramSymbols)
                put("CJK_Unified_Ideographs", CJKUnifiedIdeographs)
                put("Yi_Syllables", YiSyllables)
                put("Yi_Radicals", YiRadicals)
                put("Lisu", Lisu)
                put("Vai", Vai)
                put("Cyrillic_Extended_B", CyrillicExtendedB)
                put("Bamum", Bamum)
                put("Modifier_Tone_Letters", ModifierToneLetters)
                put("Latin_Extended_D", LatinExtendedD)
                put("Syloti_Nagri", SylotiNagri)
                put("Common_Indic_Number_Forms", CommonIndicNumberForms)
                put("Phags_pa", Phagspa)
                put("Saurashtra", Saurashtra)
                put("Devanagari_Extended", DevanagariExtended)
                put("Kayah_Li", KayahLi)
                put("Rejang", Rejang)
                put("Hangul_Jamo_Extended_A", HangulJamoExtendedA)
                put("Javanese", Javanese)
                put("Myanmar_Extended_B", MyanmarExtendedB)
                put("Cham", Cham)
                put("Myanmar_Extended_A", MyanmarExtendedA)
                put("Tai_Viet", TaiViet)
                put("Meetei_Mayek_Extensions", MeeteiMayekExtensions)
                put("Ethiopic_Extended_A", EthiopicExtendedA)
                put("Latin_Extended_E", LatinExtendedE)
                put("Cherokee_Supplement", CherokeeSupplement)
                put("Meetei_Mayek", MeeteiMayek)
                put("Hangul_Syllables", HangulSyllables)
                put("Hangul_Jamo_Extended_B", HangulJamoExtendedB)
                put("High_Surrogates", HighSurrogates)
                put("High_Private_Use_Surrogates", HighPrivateUseSurrogates)
                put("Low_Surrogates", LowSurrogates)
                put("Private_Use_Area", PrivateUseArea)
                put("CJK_Compatibility_Ideographs", CJKCompatibilityIdeographs)
                put("Alphabetic_Presentation_Forms", AlphabeticPresentationForms)
                put("Arabic_Presentation_Forms_A", ArabicPresentationFormsA)
                put("Variation_Selectors", VariationSelectors)
                put("Vertical_Forms", VerticalForms)
                put("Combining_Half_Marks", CombiningHalfMarks)
                put("CJK_Compatibility_Forms", CJKCompatibilityForms)
                put("Small_Form_Variants", SmallFormVariants)
                put("Arabic_Presentation_Forms_B", ArabicPresentationFormsB)
                put("Halfwidth_and_Fullwidth_Forms", HalfwidthandFullwidthForms)
                put("Specials", Specials)
                put("Linear_B_Syllabary", LinearBSyllabary)
                put("Linear_B_Ideograms", LinearBIdeograms)
                put("Aegean_Numbers", AegeanNumbers)
                put("Ancient_Greek_Numbers", AncientGreekNumbers)
                put("Ancient_Symbols", AncientSymbols)
                put("Phaistos_Disc", PhaistosDisc)
                put("Lycian", Lycian)
                put("Carian", Carian)
                put("Coptic_Epact_Numbers", CopticEpactNumbers)
                put("Old_Italic", OldItalic)
                put("Gothic", Gothic)
                put("Old_Permic", OldPermic)
                put("Ugaritic", Ugaritic)
                put("Old_Persian", OldPersian)
                put("Deseret", Deseret)
                put("Shavian", Shavian)
                put("Osmanya", Osmanya)
                put("Osage", Osage)
                put("Elbasan", Elbasan)
                put("Caucasian_Albanian", CaucasianAlbanian)
                put("Vithkuqi", Vithkuqi)
                put("Linear_A", LinearA)
                put("Latin_Extended_F", LatinExtendedF)
                put("Cypriot_Syllabary", CypriotSyllabary)
                put("Imperial_Aramaic", ImperialAramaic)
                put("Palmyrene", Palmyrene)
                put("Nabataean", Nabataean)
                put("Hatran", Hatran)
                put("Phoenician", Phoenician)
                put("Lydian", Lydian)
                put("Meroitic_Hieroglyphs", MeroiticHieroglyphs)
                put("Meroitic_Cursive", MeroiticCursive)
                put("Kharoshthi", Kharoshthi)
                put("Old_South_Arabian", OldSouthArabian)
                put("Old_North_Arabian", OldNorthArabian)
                put("Manichaean", Manichaean)
                put("Avestan", Avestan)
                put("Inscriptional_Parthian", InscriptionalParthian)
                put("Inscriptional_Pahlavi", InscriptionalPahlavi)
                put("Psalter_Pahlavi", PsalterPahlavi)
                put("Old_Turkic", OldTurkic)
                put("Old_Hungarian", OldHungarian)
                put("Hanifi_Rohingya", HanifiRohingya)
                put("Rumi_Numeral_Symbols", RumiNumeralSymbols)
                put("Yezidi", Yezidi)
                put("Arabic_Extended_C", ArabicExtendedC)
                put("Old_Sogdian", OldSogdian)
                put("Sogdian", Sogdian)
                put("Old_Uyghur", OldUyghur)
                put("Chorasmian", Chorasmian)
                put("Elymaic", Elymaic)
                put("Brahmi", Brahmi)
                put("Kaithi", Kaithi)
                put("Sora_Sompeng", SoraSompeng)
                put("Chakma", Chakma)
                put("Mahajani", Mahajani)
                put("Sharada", Sharada)
                put("Sinhala_Archaic_Numbers", SinhalaArchaicNumbers)
                put("Khojki", Khojki)
                put("Multani", Multani)
                put("Khudawadi", Khudawadi)
                put("Grantha", Grantha)
                put("Newa", Newa)
                put("Tirhuta", Tirhuta)
                put("Siddham", Siddham)
                put("Modi", Modi)
                put("Mongolian_Supplement", MongolianSupplement)
                put("Takri", Takri)
                put("Ahom", Ahom)
                put("Dogra", Dogra)
                put("Warang_Citi", WarangCiti)
                put("Dives_Akuru", DivesAkuru)
                put("Nandinagari", Nandinagari)
                put("Zanabazar_Square", ZanabazarSquare)
                put("Soyombo", Soyombo)
                put("Unified_Canadian_Aboriginal_Syllabics_Extended_A", UnifiedCanadianAboriginalSyllabicsExtendedA)
                put("Pau_Cin_Hau", PauCinHau)
                put("Devanagari_Extended_A", DevanagariExtendedA)
                put("Bhaiksuki", Bhaiksuki)
                put("Marchen", Marchen)
                put("Masaram_Gondi", MasaramGondi)
                put("Gunjala_Gondi", GunjalaGondi)
                put("Makasar", Makasar)
                put("Kawi", Kawi)
                put("Lisu_Supplement", LisuSupplement)
                put("Tamil_Supplement", TamilSupplement)
                put("Cuneiform", Cuneiform)
                put("Cuneiform_Numbers_and_Punctuation", CuneiformNumbersandPunctuation)
                put("Early_Dynastic_Cuneiform", EarlyDynasticCuneiform)
                put("Cypro_Minoan", CyproMinoan)
                put("Egyptian_Hieroglyphs", EgyptianHieroglyphs)
                put("Egyptian_Hieroglyph_Format_Controls", EgyptianHieroglyphFormatControls)
                put("Anatolian_Hieroglyphs", AnatolianHieroglyphs)
                put("Bamum_Supplement", BamumSupplement)
                put("Mro", Mro)
                put("Tangsa", Tangsa)
                put("Bassa_Vah", BassaVah)
                put("Pahawh_Hmong", PahawhHmong)
                put("Medefaidrin", Medefaidrin)
                put("Miao", Miao)
                put("Ideographic_Symbols_and_Punctuation", IdeographicSymbolsandPunctuation)
                put("Tangut", Tangut)
                put("Tangut_Components", TangutComponents)
                put("Khitan_Small_Script", KhitanSmallScript)
                put("Tangut_Supplement", TangutSupplement)
                put("Kana_Extended_B", KanaExtendedB)
                put("Kana_Supplement", KanaSupplement)
                put("Kana_Extended_A", KanaExtendedA)
                put("Small_Kana_Extension", SmallKanaExtension)
                put("Nushu", Nushu)
                put("Duployan", Duployan)
                put("Shorthand_Format_Controls", ShorthandFormatControls)
                put("Znamenny_Musical_Notation", ZnamennyMusicalNotation)
                put("Byzantine_Musical_Symbols", ByzantineMusicalSymbols)
                put("Musical_Symbols", MusicalSymbols)
                put("Ancient_Greek_Musical_Notation", AncientGreekMusicalNotation)
                put("Kaktovik_Numerals", KaktovikNumerals)
                put("Mayan_Numerals", MayanNumerals)
                put("Tai_Xuan_Jing_Symbols", TaiXuanJingSymbols)
                put("Counting_Rod_Numerals", CountingRodNumerals)
                put("Mathematical_Alphanumeric_Symbols", MathematicalAlphanumericSymbols)
                put("Sutton_SignWriting", SuttonSignWriting)
                put("Latin_Extended_G", LatinExtendedG)
                put("Glagolitic_Supplement", GlagoliticSupplement)
                put("Cyrillic_Extended_D", CyrillicExtendedD)
                put("Nyiakeng_Puachue_Hmong", NyiakengPuachueHmong)
                put("Toto", Toto)
                put("Wancho", Wancho)
                put("Nag_Mundari", NagMundari)
                put("Ethiopic_Extended_B", EthiopicExtendedB)
                put("Mende_Kikakui", MendeKikakui)
                put("Adlam", Adlam)
                put("Indic_Siyaq_Numbers", IndicSiyaqNumbers)
                put("Ottoman_Siyaq_Numbers", OttomanSiyaqNumbers)
                put("Arabic_Mathematical_Alphabetic_Symbols", ArabicMathematicalAlphabeticSymbols)
                put("Mahjong_Tiles", MahjongTiles)
                put("Domino_Tiles", DominoTiles)
                put("Playing_Cards", PlayingCards)
                put("Enclosed_Alphanumeric_Supplement", EnclosedAlphanumericSupplement)
                put("Enclosed_Ideographic_Supplement", EnclosedIdeographicSupplement)
                put("Miscellaneous_Symbols_and_Pictographs", MiscellaneousSymbolsandPictographs)
                put("Emoticons", Emoticons)
                put("Ornamental_Dingbats", OrnamentalDingbats)
                put("Transport_and_Map_Symbols", TransportandMapSymbols)
                put("Alchemical_Symbols", AlchemicalSymbols)
                put("Geometric_Shapes_Extended", GeometricShapesExtended)
                put("Supplemental_Arrows_C", SupplementalArrowsC)
                put("Supplemental_Symbols_and_Pictographs", SupplementalSymbolsandPictographs)
                put("Chess_Symbols", ChessSymbols)
                put("Symbols_and_Pictographs_Extended_A", SymbolsandPictographsExtendedA)
                put("Symbols_for_Legacy_Computing", SymbolsforLegacyComputing)
                put("CJK_Unified_Ideographs_Extension_B", CJKUnifiedIdeographsExtensionB)
                put("CJK_Unified_Ideographs_Extension_C", CJKUnifiedIdeographsExtensionC)
                put("CJK_Unified_Ideographs_Extension_D", CJKUnifiedIdeographsExtensionD)
                put("CJK_Unified_Ideographs_Extension_E", CJKUnifiedIdeographsExtensionE)
                put("CJK_Unified_Ideographs_Extension_F", CJKUnifiedIdeographsExtensionF)
                put("CJK_Compatibility_Ideographs_Supplement", CJKCompatibilityIdeographsSupplement)
                put("CJK_Unified_Ideographs_Extension_G", CJKUnifiedIdeographsExtensionG)
                put("CJK_Unified_Ideographs_Extension_H", CJKUnifiedIdeographsExtensionH)
                put("Tags", Tags)
                put("Variation_Selectors_Supplement", VariationSelectorsSupplement)
                put("Supplementary_Private_Use_Area_A", SupplementaryPrivateUseAreaA)
                put("Supplementary_Private_Use_Area_B", SupplementaryPrivateUseAreaB)
            }
        }

        fun fromName(name: String): CodeBlock? = byName[name]

        /** Additional short/alias names for each block (from the Unicode NameAliases.txt database). */
        val shortNames: Map<String, CodeBlock> by lazy {
            buildMap {
                put("Greek", GreekandCoptic)
                put("Combining_Diacritical_Marks_For_Symbols", CombiningDiacriticalMarksforSymbols)
                put("High_Surrogates", HighSurrogates)
                put("High_Private_Use_Surrogates", HighPrivateUseSurrogates)
                put("Low_Surrogates", LowSurrogates)
                put("Private_Use_Area", PrivateUseArea)
                put("Private_Use", PrivateUseArea)
                put("Combining_Half_Marks", CombiningHalfMarks)
            }
        }
    }
}
