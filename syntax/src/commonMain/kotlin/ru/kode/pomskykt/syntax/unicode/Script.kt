package ru.kode.pomskykt.syntax.unicode

/** Unicode script (sc). */
enum class Script(val fullName: String) {
    Adlam("Adlam"),
    CaucasianAlbanian("Caucasian_Albanian"),
    Ahom("Ahom"),
    Arabic("Arabic"),
    ImperialAramaic("Imperial_Aramaic"),
    Armenian("Armenian"),
    Avestan("Avestan"),
    Balinese("Balinese"),
    Bamum("Bamum"),
    BassaVah("Bassa_Vah"),
    Batak("Batak"),
    Bengali("Bengali"),
    Bhaiksuki("Bhaiksuki"),
    Bopomofo("Bopomofo"),
    Brahmi("Brahmi"),
    Braille("Braille"),
    Buginese("Buginese"),
    Buhid("Buhid"),
    Chakma("Chakma"),
    CanadianAboriginal("Canadian_Aboriginal"),
    Carian("Carian"),
    Cham("Cham"),
    Cherokee("Cherokee"),
    Chorasmian("Chorasmian"),
    Coptic("Coptic"),
    CyproMinoan("Cypro_Minoan"),
    Cypriot("Cypriot"),
    Cyrillic("Cyrillic"),
    Devanagari("Devanagari"),
    DivesAkuru("Dives_Akuru"),
    Dogra("Dogra"),
    Deseret("Deseret"),
    Duployan("Duployan"),
    EgyptianHieroglyphs("Egyptian_Hieroglyphs"),
    Elbasan("Elbasan"),
    Elymaic("Elymaic"),
    Ethiopic("Ethiopic"),
    Georgian("Georgian"),
    Glagolitic("Glagolitic"),
    GunjalaGondi("Gunjala_Gondi"),
    MasaramGondi("Masaram_Gondi"),
    Gothic("Gothic"),
    Grantha("Grantha"),
    Greek("Greek"),
    Gujarati("Gujarati"),
    Gurmukhi("Gurmukhi"),
    Hangul("Hangul"),
    Han("Han"),
    Hanunoo("Hanunoo"),
    Hatran("Hatran"),
    Hebrew("Hebrew"),
    Hiragana("Hiragana"),
    AnatolianHieroglyphs("Anatolian_Hieroglyphs"),
    PahawhHmong("Pahawh_Hmong"),
    NyiakengPuachueHmong("Nyiakeng_Puachue_Hmong"),
    OldHungarian("Old_Hungarian"),
    OldItalic("Old_Italic"),
    Javanese("Javanese"),
    KayahLi("Kayah_Li"),
    Katakana("Katakana"),
    Kawi("Kawi"),
    Kharoshthi("Kharoshthi"),
    Khmer("Khmer"),
    Khojki("Khojki"),
    KhitanSmallScript("Khitan_Small_Script"),
    Kannada("Kannada"),
    Kaithi("Kaithi"),
    TaiTham("Tai_Tham"),
    Lao("Lao"),
    Latin("Latin"),
    Lepcha("Lepcha"),
    Limbu("Limbu"),
    LinearA("Linear_A"),
    LinearB("Linear_B"),
    Lisu("Lisu"),
    Lycian("Lycian"),
    Lydian("Lydian"),
    Mahajani("Mahajani"),
    Makasar("Makasar"),
    Mandaic("Mandaic"),
    Manichaean("Manichaean"),
    Marchen("Marchen"),
    Medefaidrin("Medefaidrin"),
    MendeKikakui("Mende_Kikakui"),
    MeroiticCursive("Meroitic_Cursive"),
    MeroiticHieroglyphs("Meroitic_Hieroglyphs"),
    Malayalam("Malayalam"),
    Modi("Modi"),
    Mongolian("Mongolian"),
    Mro("Mro"),
    MeeteiMayek("Meetei_Mayek"),
    Multani("Multani"),
    Myanmar("Myanmar"),
    NagMundari("Nag_Mundari"),
    Nandinagari("Nandinagari"),
    OldNorthArabian("Old_North_Arabian"),
    Nabataean("Nabataean"),
    Newa("Newa"),
    Nko("Nko"),
    Nushu("Nushu"),
    Ogham("Ogham"),
    OlChiki("Ol_Chiki"),
    OldTurkic("Old_Turkic"),
    Oriya("Oriya"),
    Osage("Osage"),
    Osmanya("Osmanya"),
    OldUyghur("Old_Uyghur"),
    Palmyrene("Palmyrene"),
    PauCinHau("Pau_Cin_Hau"),
    OldPermic("Old_Permic"),
    PhagsPa("Phags_Pa"),
    InscriptionalPahlavi("Inscriptional_Pahlavi"),
    PsalterPahlavi("Psalter_Pahlavi"),
    Phoenician("Phoenician"),
    Miao("Miao"),
    InscriptionalParthian("Inscriptional_Parthian"),
    Rejang("Rejang"),
    HanifiRohingya("Hanifi_Rohingya"),
    Runic("Runic"),
    Samaritan("Samaritan"),
    OldSouthArabian("Old_South_Arabian"),
    Saurashtra("Saurashtra"),
    SignWriting("SignWriting"),
    Shavian("Shavian"),
    Sharada("Sharada"),
    Siddham("Siddham"),
    Khudawadi("Khudawadi"),
    Sinhala("Sinhala"),
    Sogdian("Sogdian"),
    OldSogdian("Old_Sogdian"),
    SoraSompeng("Sora_Sompeng"),
    Soyombo("Soyombo"),
    Sundanese("Sundanese"),
    SylotiNagri("Syloti_Nagri"),
    Syriac("Syriac"),
    Tagbanwa("Tagbanwa"),
    Takri("Takri"),
    TaiLe("Tai_Le"),
    NewTaiLue("New_Tai_Lue"),
    Tamil("Tamil"),
    Tangut("Tangut"),
    TaiViet("Tai_Viet"),
    Telugu("Telugu"),
    Tifinagh("Tifinagh"),
    Tagalog("Tagalog"),
    Thaana("Thaana"),
    Thai("Thai"),
    Tibetan("Tibetan"),
    Tirhuta("Tirhuta"),
    Tangsa("Tangsa"),
    Toto("Toto"),
    Ugaritic("Ugaritic"),
    Vai("Vai"),
    Vithkuqi("Vithkuqi"),
    WarangCiti("Warang_Citi"),
    Wancho("Wancho"),
    OldPersian("Old_Persian"),
    Cuneiform("Cuneiform"),
    Yezidi("Yezidi"),
    Yi("Yi"),
    ZanabazarSquare("Zanabazar_Square"),
    Inherited("Inherited"),
    Common("Common"),
    Unknown("Unknown"),
    ;

    companion object {
        private val byName: Map<String, Script> by lazy {
            buildMap {
                put("Adlm", Adlam)
                put("Adlam", Adlam)
                put("Aghb", CaucasianAlbanian)
                put("Caucasian_Albanian", CaucasianAlbanian)
                put("Ahom", Ahom)
                put("Ahom", Ahom)
                put("Arab", Arabic)
                put("Arabic", Arabic)
                put("Armi", ImperialAramaic)
                put("Imperial_Aramaic", ImperialAramaic)
                put("Armn", Armenian)
                put("Armenian", Armenian)
                put("Avst", Avestan)
                put("Avestan", Avestan)
                put("Bali", Balinese)
                put("Balinese", Balinese)
                put("Bamu", Bamum)
                put("Bamum", Bamum)
                put("Bass", BassaVah)
                put("Bassa_Vah", BassaVah)
                put("Batk", Batak)
                put("Batak", Batak)
                put("Beng", Bengali)
                put("Bengali", Bengali)
                put("Bhks", Bhaiksuki)
                put("Bhaiksuki", Bhaiksuki)
                put("Bopo", Bopomofo)
                put("Bopomofo", Bopomofo)
                put("Brah", Brahmi)
                put("Brahmi", Brahmi)
                put("Brai", Braille)
                put("Braille", Braille)
                put("Bugi", Buginese)
                put("Buginese", Buginese)
                put("Buhd", Buhid)
                put("Buhid", Buhid)
                put("Cakm", Chakma)
                put("Chakma", Chakma)
                put("Cans", CanadianAboriginal)
                put("Canadian_Aboriginal", CanadianAboriginal)
                put("Cari", Carian)
                put("Carian", Carian)
                put("Cham", Cham)
                put("Cham", Cham)
                put("Cher", Cherokee)
                put("Cherokee", Cherokee)
                put("Chrs", Chorasmian)
                put("Chorasmian", Chorasmian)
                put("Copt", Coptic)
                put("Coptic", Coptic)
                put("Qaac", Coptic)
                put("Cpmn", CyproMinoan)
                put("Cypro_Minoan", CyproMinoan)
                put("Cprt", Cypriot)
                put("Cypriot", Cypriot)
                put("Cyrl", Cyrillic)
                put("Cyrillic", Cyrillic)
                put("Deva", Devanagari)
                put("Devanagari", Devanagari)
                put("Diak", DivesAkuru)
                put("Dives_Akuru", DivesAkuru)
                put("Dogr", Dogra)
                put("Dogra", Dogra)
                put("Dsrt", Deseret)
                put("Deseret", Deseret)
                put("Dupl", Duployan)
                put("Duployan", Duployan)
                put("Egyp", EgyptianHieroglyphs)
                put("Egyptian_Hieroglyphs", EgyptianHieroglyphs)
                put("Elba", Elbasan)
                put("Elbasan", Elbasan)
                put("Elym", Elymaic)
                put("Elymaic", Elymaic)
                put("Ethi", Ethiopic)
                put("Ethiopic", Ethiopic)
                put("Geor", Georgian)
                put("Georgian", Georgian)
                put("Glag", Glagolitic)
                put("Glagolitic", Glagolitic)
                put("Gong", GunjalaGondi)
                put("Gunjala_Gondi", GunjalaGondi)
                put("Gonm", MasaramGondi)
                put("Masaram_Gondi", MasaramGondi)
                put("Goth", Gothic)
                put("Gothic", Gothic)
                put("Gran", Grantha)
                put("Grantha", Grantha)
                put("Grek", Greek)
                put("Greek", Greek)
                put("Gujr", Gujarati)
                put("Gujarati", Gujarati)
                put("Guru", Gurmukhi)
                put("Gurmukhi", Gurmukhi)
                put("Hang", Hangul)
                put("Hangul", Hangul)
                put("Hani", Han)
                put("Han", Han)
                put("Hano", Hanunoo)
                put("Hanunoo", Hanunoo)
                put("Hatr", Hatran)
                put("Hatran", Hatran)
                put("Hebr", Hebrew)
                put("Hebrew", Hebrew)
                put("Hira", Hiragana)
                put("Hiragana", Hiragana)
                put("Hluw", AnatolianHieroglyphs)
                put("Anatolian_Hieroglyphs", AnatolianHieroglyphs)
                put("Hmng", PahawhHmong)
                put("Pahawh_Hmong", PahawhHmong)
                put("Hmnp", NyiakengPuachueHmong)
                put("Nyiakeng_Puachue_Hmong", NyiakengPuachueHmong)
                put("Hung", OldHungarian)
                put("Old_Hungarian", OldHungarian)
                put("Ital", OldItalic)
                put("Old_Italic", OldItalic)
                put("Java", Javanese)
                put("Javanese", Javanese)
                put("Kali", KayahLi)
                put("Kayah_Li", KayahLi)
                put("Kana", Katakana)
                put("Katakana", Katakana)
                put("Kawi", Kawi)
                put("Kawi", Kawi)
                put("Khar", Kharoshthi)
                put("Kharoshthi", Kharoshthi)
                put("Khmr", Khmer)
                put("Khmer", Khmer)
                put("Khoj", Khojki)
                put("Khojki", Khojki)
                put("Kits", KhitanSmallScript)
                put("Khitan_Small_Script", KhitanSmallScript)
                put("Knda", Kannada)
                put("Kannada", Kannada)
                put("Kthi", Kaithi)
                put("Kaithi", Kaithi)
                put("Lana", TaiTham)
                put("Tai_Tham", TaiTham)
                put("Laoo", Lao)
                put("Lao", Lao)
                put("Latn", Latin)
                put("Latin", Latin)
                put("Lepc", Lepcha)
                put("Lepcha", Lepcha)
                put("Limb", Limbu)
                put("Limbu", Limbu)
                put("Lina", LinearA)
                put("Linear_A", LinearA)
                put("Linb", LinearB)
                put("Linear_B", LinearB)
                put("Lisu", Lisu)
                put("Lisu", Lisu)
                put("Lyci", Lycian)
                put("Lycian", Lycian)
                put("Lydi", Lydian)
                put("Lydian", Lydian)
                put("Mahj", Mahajani)
                put("Mahajani", Mahajani)
                put("Maka", Makasar)
                put("Makasar", Makasar)
                put("Mand", Mandaic)
                put("Mandaic", Mandaic)
                put("Mani", Manichaean)
                put("Manichaean", Manichaean)
                put("Marc", Marchen)
                put("Marchen", Marchen)
                put("Medf", Medefaidrin)
                put("Medefaidrin", Medefaidrin)
                put("Mend", MendeKikakui)
                put("Mende_Kikakui", MendeKikakui)
                put("Merc", MeroiticCursive)
                put("Meroitic_Cursive", MeroiticCursive)
                put("Mero", MeroiticHieroglyphs)
                put("Meroitic_Hieroglyphs", MeroiticHieroglyphs)
                put("Mlym", Malayalam)
                put("Malayalam", Malayalam)
                put("Modi", Modi)
                put("Modi", Modi)
                put("Mong", Mongolian)
                put("Mongolian", Mongolian)
                put("Mroo", Mro)
                put("Mro", Mro)
                put("Mtei", MeeteiMayek)
                put("Meetei_Mayek", MeeteiMayek)
                put("Mult", Multani)
                put("Multani", Multani)
                put("Mymr", Myanmar)
                put("Myanmar", Myanmar)
                put("Nagm", NagMundari)
                put("Nag_Mundari", NagMundari)
                put("Nand", Nandinagari)
                put("Nandinagari", Nandinagari)
                put("Narb", OldNorthArabian)
                put("Old_North_Arabian", OldNorthArabian)
                put("Nbat", Nabataean)
                put("Nabataean", Nabataean)
                put("Newa", Newa)
                put("Newa", Newa)
                put("Nkoo", Nko)
                put("Nko", Nko)
                put("Nshu", Nushu)
                put("Nushu", Nushu)
                put("Ogam", Ogham)
                put("Ogham", Ogham)
                put("Olck", OlChiki)
                put("Ol_Chiki", OlChiki)
                put("Orkh", OldTurkic)
                put("Old_Turkic", OldTurkic)
                put("Orya", Oriya)
                put("Oriya", Oriya)
                put("Osge", Osage)
                put("Osage", Osage)
                put("Osma", Osmanya)
                put("Osmanya", Osmanya)
                put("Ougr", OldUyghur)
                put("Old_Uyghur", OldUyghur)
                put("Palm", Palmyrene)
                put("Palmyrene", Palmyrene)
                put("Pauc", PauCinHau)
                put("Pau_Cin_Hau", PauCinHau)
                put("Perm", OldPermic)
                put("Old_Permic", OldPermic)
                put("Phag", PhagsPa)
                put("Phags_Pa", PhagsPa)
                put("Phli", InscriptionalPahlavi)
                put("Inscriptional_Pahlavi", InscriptionalPahlavi)
                put("Phlp", PsalterPahlavi)
                put("Psalter_Pahlavi", PsalterPahlavi)
                put("Phnx", Phoenician)
                put("Phoenician", Phoenician)
                put("Plrd", Miao)
                put("Miao", Miao)
                put("Prti", InscriptionalParthian)
                put("Inscriptional_Parthian", InscriptionalParthian)
                put("Rjng", Rejang)
                put("Rejang", Rejang)
                put("Rohg", HanifiRohingya)
                put("Hanifi_Rohingya", HanifiRohingya)
                put("Runr", Runic)
                put("Runic", Runic)
                put("Samr", Samaritan)
                put("Samaritan", Samaritan)
                put("Sarb", OldSouthArabian)
                put("Old_South_Arabian", OldSouthArabian)
                put("Saur", Saurashtra)
                put("Saurashtra", Saurashtra)
                put("Sgnw", SignWriting)
                put("SignWriting", SignWriting)
                put("Shaw", Shavian)
                put("Shavian", Shavian)
                put("Shrd", Sharada)
                put("Sharada", Sharada)
                put("Sidd", Siddham)
                put("Siddham", Siddham)
                put("Sind", Khudawadi)
                put("Khudawadi", Khudawadi)
                put("Sinh", Sinhala)
                put("Sinhala", Sinhala)
                put("Sogd", Sogdian)
                put("Sogdian", Sogdian)
                put("Sogo", OldSogdian)
                put("Old_Sogdian", OldSogdian)
                put("Sora", SoraSompeng)
                put("Sora_Sompeng", SoraSompeng)
                put("Soyo", Soyombo)
                put("Soyombo", Soyombo)
                put("Sund", Sundanese)
                put("Sundanese", Sundanese)
                put("Sylo", SylotiNagri)
                put("Syloti_Nagri", SylotiNagri)
                put("Syrc", Syriac)
                put("Syriac", Syriac)
                put("Tagb", Tagbanwa)
                put("Tagbanwa", Tagbanwa)
                put("Takr", Takri)
                put("Takri", Takri)
                put("Tale", TaiLe)
                put("Tai_Le", TaiLe)
                put("Talu", NewTaiLue)
                put("New_Tai_Lue", NewTaiLue)
                put("Taml", Tamil)
                put("Tamil", Tamil)
                put("Tang", Tangut)
                put("Tangut", Tangut)
                put("Tavt", TaiViet)
                put("Tai_Viet", TaiViet)
                put("Telu", Telugu)
                put("Telugu", Telugu)
                put("Tfng", Tifinagh)
                put("Tifinagh", Tifinagh)
                put("Tglg", Tagalog)
                put("Tagalog", Tagalog)
                put("Thaa", Thaana)
                put("Thaana", Thaana)
                put("Thai", Thai)
                put("Thai", Thai)
                put("Tibt", Tibetan)
                put("Tibetan", Tibetan)
                put("Tirh", Tirhuta)
                put("Tirhuta", Tirhuta)
                put("Tnsa", Tangsa)
                put("Tangsa", Tangsa)
                put("Toto", Toto)
                put("Toto", Toto)
                put("Ugar", Ugaritic)
                put("Ugaritic", Ugaritic)
                put("Vaii", Vai)
                put("Vai", Vai)
                put("Vith", Vithkuqi)
                put("Vithkuqi", Vithkuqi)
                put("Wara", WarangCiti)
                put("Warang_Citi", WarangCiti)
                put("Wcho", Wancho)
                put("Wancho", Wancho)
                put("Xpeo", OldPersian)
                put("Old_Persian", OldPersian)
                put("Xsux", Cuneiform)
                put("Cuneiform", Cuneiform)
                put("Yezi", Yezidi)
                put("Yezidi", Yezidi)
                put("Yiii", Yi)
                put("Yi", Yi)
                put("Zanb", ZanabazarSquare)
                put("Zanabazar_Square", ZanabazarSquare)
                put("Zinh", Inherited)
                put("Inherited", Inherited)
                put("Qaai", Inherited)
                put("Zyyy", Common)
                put("Common", Common)
                put("Zzzz", Unknown)
                put("Unknown", Unknown)
            }
        }

        fun fromName(name: String): Script? = byName[name]

        fun aliases(script: Script): List<String> = when (script) {
            Adlam -> listOf("Adlm", "Adlam")
            CaucasianAlbanian -> listOf("Aghb", "Caucasian_Albanian")
            Ahom -> listOf("Ahom", "Ahom")
            Arabic -> listOf("Arab", "Arabic")
            ImperialAramaic -> listOf("Armi", "Imperial_Aramaic")
            Armenian -> listOf("Armn", "Armenian")
            Avestan -> listOf("Avst", "Avestan")
            Balinese -> listOf("Bali", "Balinese")
            Bamum -> listOf("Bamu", "Bamum")
            BassaVah -> listOf("Bass", "Bassa_Vah")
            Batak -> listOf("Batk", "Batak")
            Bengali -> listOf("Beng", "Bengali")
            Bhaiksuki -> listOf("Bhks", "Bhaiksuki")
            Bopomofo -> listOf("Bopo", "Bopomofo")
            Brahmi -> listOf("Brah", "Brahmi")
            Braille -> listOf("Brai", "Braille")
            Buginese -> listOf("Bugi", "Buginese")
            Buhid -> listOf("Buhd", "Buhid")
            Chakma -> listOf("Cakm", "Chakma")
            CanadianAboriginal -> listOf("Cans", "Canadian_Aboriginal")
            Carian -> listOf("Cari", "Carian")
            Cham -> listOf("Cham", "Cham")
            Cherokee -> listOf("Cher", "Cherokee")
            Chorasmian -> listOf("Chrs", "Chorasmian")
            Coptic -> listOf("Copt", "Coptic", "Qaac")
            CyproMinoan -> listOf("Cpmn", "Cypro_Minoan")
            Cypriot -> listOf("Cprt", "Cypriot")
            Cyrillic -> listOf("Cyrl", "Cyrillic")
            Devanagari -> listOf("Deva", "Devanagari")
            DivesAkuru -> listOf("Diak", "Dives_Akuru")
            Dogra -> listOf("Dogr", "Dogra")
            Deseret -> listOf("Dsrt", "Deseret")
            Duployan -> listOf("Dupl", "Duployan")
            EgyptianHieroglyphs -> listOf("Egyp", "Egyptian_Hieroglyphs")
            Elbasan -> listOf("Elba", "Elbasan")
            Elymaic -> listOf("Elym", "Elymaic")
            Ethiopic -> listOf("Ethi", "Ethiopic")
            Georgian -> listOf("Geor", "Georgian")
            Glagolitic -> listOf("Glag", "Glagolitic")
            GunjalaGondi -> listOf("Gong", "Gunjala_Gondi")
            MasaramGondi -> listOf("Gonm", "Masaram_Gondi")
            Gothic -> listOf("Goth", "Gothic")
            Grantha -> listOf("Gran", "Grantha")
            Greek -> listOf("Grek", "Greek")
            Gujarati -> listOf("Gujr", "Gujarati")
            Gurmukhi -> listOf("Guru", "Gurmukhi")
            Hangul -> listOf("Hang", "Hangul")
            Han -> listOf("Hani", "Han")
            Hanunoo -> listOf("Hano", "Hanunoo")
            Hatran -> listOf("Hatr", "Hatran")
            Hebrew -> listOf("Hebr", "Hebrew")
            Hiragana -> listOf("Hira", "Hiragana")
            AnatolianHieroglyphs -> listOf("Hluw", "Anatolian_Hieroglyphs")
            PahawhHmong -> listOf("Hmng", "Pahawh_Hmong")
            NyiakengPuachueHmong -> listOf("Hmnp", "Nyiakeng_Puachue_Hmong")
            OldHungarian -> listOf("Hung", "Old_Hungarian")
            OldItalic -> listOf("Ital", "Old_Italic")
            Javanese -> listOf("Java", "Javanese")
            KayahLi -> listOf("Kali", "Kayah_Li")
            Katakana -> listOf("Kana", "Katakana")
            Kawi -> listOf("Kawi", "Kawi")
            Kharoshthi -> listOf("Khar", "Kharoshthi")
            Khmer -> listOf("Khmr", "Khmer")
            Khojki -> listOf("Khoj", "Khojki")
            KhitanSmallScript -> listOf("Kits", "Khitan_Small_Script")
            Kannada -> listOf("Knda", "Kannada")
            Kaithi -> listOf("Kthi", "Kaithi")
            TaiTham -> listOf("Lana", "Tai_Tham")
            Lao -> listOf("Laoo", "Lao")
            Latin -> listOf("Latn", "Latin")
            Lepcha -> listOf("Lepc", "Lepcha")
            Limbu -> listOf("Limb", "Limbu")
            LinearA -> listOf("Lina", "Linear_A")
            LinearB -> listOf("Linb", "Linear_B")
            Lisu -> listOf("Lisu", "Lisu")
            Lycian -> listOf("Lyci", "Lycian")
            Lydian -> listOf("Lydi", "Lydian")
            Mahajani -> listOf("Mahj", "Mahajani")
            Makasar -> listOf("Maka", "Makasar")
            Mandaic -> listOf("Mand", "Mandaic")
            Manichaean -> listOf("Mani", "Manichaean")
            Marchen -> listOf("Marc", "Marchen")
            Medefaidrin -> listOf("Medf", "Medefaidrin")
            MendeKikakui -> listOf("Mend", "Mende_Kikakui")
            MeroiticCursive -> listOf("Merc", "Meroitic_Cursive")
            MeroiticHieroglyphs -> listOf("Mero", "Meroitic_Hieroglyphs")
            Malayalam -> listOf("Mlym", "Malayalam")
            Modi -> listOf("Modi", "Modi")
            Mongolian -> listOf("Mong", "Mongolian")
            Mro -> listOf("Mroo", "Mro")
            MeeteiMayek -> listOf("Mtei", "Meetei_Mayek")
            Multani -> listOf("Mult", "Multani")
            Myanmar -> listOf("Mymr", "Myanmar")
            NagMundari -> listOf("Nagm", "Nag_Mundari")
            Nandinagari -> listOf("Nand", "Nandinagari")
            OldNorthArabian -> listOf("Narb", "Old_North_Arabian")
            Nabataean -> listOf("Nbat", "Nabataean")
            Newa -> listOf("Newa", "Newa")
            Nko -> listOf("Nkoo", "Nko")
            Nushu -> listOf("Nshu", "Nushu")
            Ogham -> listOf("Ogam", "Ogham")
            OlChiki -> listOf("Olck", "Ol_Chiki")
            OldTurkic -> listOf("Orkh", "Old_Turkic")
            Oriya -> listOf("Orya", "Oriya")
            Osage -> listOf("Osge", "Osage")
            Osmanya -> listOf("Osma", "Osmanya")
            OldUyghur -> listOf("Ougr", "Old_Uyghur")
            Palmyrene -> listOf("Palm", "Palmyrene")
            PauCinHau -> listOf("Pauc", "Pau_Cin_Hau")
            OldPermic -> listOf("Perm", "Old_Permic")
            PhagsPa -> listOf("Phag", "Phags_Pa")
            InscriptionalPahlavi -> listOf("Phli", "Inscriptional_Pahlavi")
            PsalterPahlavi -> listOf("Phlp", "Psalter_Pahlavi")
            Phoenician -> listOf("Phnx", "Phoenician")
            Miao -> listOf("Plrd", "Miao")
            InscriptionalParthian -> listOf("Prti", "Inscriptional_Parthian")
            Rejang -> listOf("Rjng", "Rejang")
            HanifiRohingya -> listOf("Rohg", "Hanifi_Rohingya")
            Runic -> listOf("Runr", "Runic")
            Samaritan -> listOf("Samr", "Samaritan")
            OldSouthArabian -> listOf("Sarb", "Old_South_Arabian")
            Saurashtra -> listOf("Saur", "Saurashtra")
            SignWriting -> listOf("Sgnw", "SignWriting")
            Shavian -> listOf("Shaw", "Shavian")
            Sharada -> listOf("Shrd", "Sharada")
            Siddham -> listOf("Sidd", "Siddham")
            Khudawadi -> listOf("Sind", "Khudawadi")
            Sinhala -> listOf("Sinh", "Sinhala")
            Sogdian -> listOf("Sogd", "Sogdian")
            OldSogdian -> listOf("Sogo", "Old_Sogdian")
            SoraSompeng -> listOf("Sora", "Sora_Sompeng")
            Soyombo -> listOf("Soyo", "Soyombo")
            Sundanese -> listOf("Sund", "Sundanese")
            SylotiNagri -> listOf("Sylo", "Syloti_Nagri")
            Syriac -> listOf("Syrc", "Syriac")
            Tagbanwa -> listOf("Tagb", "Tagbanwa")
            Takri -> listOf("Takr", "Takri")
            TaiLe -> listOf("Tale", "Tai_Le")
            NewTaiLue -> listOf("Talu", "New_Tai_Lue")
            Tamil -> listOf("Taml", "Tamil")
            Tangut -> listOf("Tang", "Tangut")
            TaiViet -> listOf("Tavt", "Tai_Viet")
            Telugu -> listOf("Telu", "Telugu")
            Tifinagh -> listOf("Tfng", "Tifinagh")
            Tagalog -> listOf("Tglg", "Tagalog")
            Thaana -> listOf("Thaa", "Thaana")
            Thai -> listOf("Thai", "Thai")
            Tibetan -> listOf("Tibt", "Tibetan")
            Tirhuta -> listOf("Tirh", "Tirhuta")
            Tangsa -> listOf("Tnsa", "Tangsa")
            Toto -> listOf("Toto", "Toto")
            Ugaritic -> listOf("Ugar", "Ugaritic")
            Vai -> listOf("Vaii", "Vai")
            Vithkuqi -> listOf("Vith", "Vithkuqi")
            WarangCiti -> listOf("Wara", "Warang_Citi")
            Wancho -> listOf("Wcho", "Wancho")
            OldPersian -> listOf("Xpeo", "Old_Persian")
            Cuneiform -> listOf("Xsux", "Cuneiform")
            Yezidi -> listOf("Yezi", "Yezidi")
            Yi -> listOf("Yiii", "Yi")
            ZanabazarSquare -> listOf("Zanb", "Zanabazar_Square")
            Inherited -> listOf("Zinh", "Inherited", "Qaai")
            Common -> listOf("Zyyy", "Common")
            Unknown -> listOf("Zzzz", "Unknown")
        }
    }
}
