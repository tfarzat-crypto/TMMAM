package com.officeestimator.app.data

/** معدل هندسي قابل للتعديل (واحد لكل الدول). */
data class RateDef(
    val key: String,
    val label: String,
    val unit: String,
    val group: String,
    val default: Double
)

object Rates {
    const val G_STRUCT = "الهيكل الخرساني"
    const val G_FOUND = "الأساسات"
    const val G_STEEL = "معدلات الحديد"
    const val G_MIX = "مكونات الخلطة"
    const val G_WALL = "المباني"
    const val G_FIN = "التشطيبات"
    const val G_INS = "العزل"
    const val G_EARTH = "الحفر والردم"
    const val G_TIME = "مدة التنفيذ"
    const val G_QUICK = "التحقق السريع"

    val defs: List<RateDef> = listOf(
        RateDef("slab_solid", "خرسانة السقف المصمت مع الكمرات", "م³/م²", G_STRUCT, 0.20),
        RateDef("slab_hordi", "خرسانة سقف الهوردي", "م³/م²", G_STRUCT, 0.175),
        RateDef("slab_flat", "خرسانة الفلات سلاب", "م³/م²", G_STRUCT, 0.24),
        RateDef("col", "خرسانة الأعمدة", "م³/م²", G_STRUCT, 0.04),
        RateDef("stair", "خرسانة السلم", "م³/دور", G_STRUCT, 3.5),
        RateDef("hordi_blocks", "بلوك الهوردي", "بلوك/م²", G_STRUCT, 5.0),

        RateDef("found_iso_floor", "قواعد منفصلة لكل دور", "م³/م²", G_FOUND, 0.09),
        RateDef("tie_beams", "الميدات ورقاب الأعمدة", "م³/م²", G_FOUND, 0.06),
        RateDef("strip_factor", "معامل القواعد الشريطية", "×", G_FOUND, 1.3),
        RateDef("raft_base", "سمك اللبشة الأساسي", "م", G_FOUND, 0.30),
        RateDef("raft_per_floor", "زيادة سمك اللبشة لكل دور", "م", G_FOUND, 0.10),
        RateDef("blinding_thk", "سمك خرسانة النظافة", "م", G_FOUND, 0.10),
        RateDef("sog_thk", "سمك خرسانة الأرضية", "م", G_FOUND, 0.12),

        RateDef("st_found", "حديد القواعد والميدات", "كجم/م³", G_STEEL, 90.0),
        RateDef("st_raft", "حديد اللبشة", "كجم/م³", G_STEEL, 110.0),
        RateDef("st_col", "حديد الأعمدة", "كجم/م³", G_STEEL, 170.0),
        RateDef("st_solid", "حديد السقف المصمت", "كجم/م³", G_STEEL, 100.0),
        RateDef("st_hordi", "حديد سقف الهوردي", "كجم/م³", G_STEEL, 110.0),
        RateDef("st_flat", "حديد الفلات سلاب", "كجم/م³", G_STEEL, 130.0),
        RateDef("st_stair", "حديد السلم", "كجم/م³", G_STEEL, 110.0),
        RateDef("st_waste", "هالك الحديد", "%", G_STEEL, 3.0),

        RateDef("mix_cement", "أسمنت الخرسانة المسلحة", "شكارة/م³", G_MIX, 7.0),
        RateDef("pc_cement", "أسمنت الخرسانة العادية", "شكارة/م³", G_MIX, 5.0),
        RateDef("mix_sand", "الرمل", "م³/م³", G_MIX, 0.4),
        RateDef("mix_gravel", "الركام (الزلط)", "م³/م³", G_MIX, 0.8),

        RateDef("open_ext", "فتحات الحوائط الخارجية", "%", G_WALL, 20.0),
        RateDef("parapet_h", "ارتفاع دروة السطح", "م", G_WALL, 1.2),
        RateDef("int_wall", "حوائط داخلية لكل م² من الدور", "م²/م²", G_WALL, 1.1),
        RateDef("block_m2", "عدد البلوك", "بلوك/م²", G_WALL, 12.5),
        RateDef("brick_full", "طوب حائط طوبة كاملة", "طوبة/م²", G_WALL, 116.0),
        RateDef("brick_half", "طوب حائط نصف طوبة", "طوبة/م²", G_WALL, 58.0),
        RateDef("wall_waste", "هالك المباني", "%", G_WALL, 5.0),
        RateDef("mortar_cement", "أسمنت مونة المباني", "شكارة/م²", G_WALL, 0.2),
        RateDef("mortar_sand", "رمل مونة المباني", "م³/م²", G_WALL, 0.03),

        RateDef("plaster_cement", "أسمنت اللياسة", "شكارة/م²", G_FIN, 0.15),
        RateDef("plaster_sand", "رمل اللياسة", "م³/م²", G_FIN, 0.025),
        RateDef("tile_waste", "هالك البلاط", "%", G_FIN, 5.0),
        RateDef("wall_tile", "بلاط حوائط لكل م² مبني", "م²/م²", G_FIN, 0.2),
        RateDef("facade_open", "فتحات الواجهة الرئيسية", "%", G_FIN, 25.0),

        RateDef("wp_roof_f", "معامل عزل السطح (مع الرفعات)", "×", G_INS, 1.15),
        RateDef("wet_ratio", "نسبة المناطق الرطبة", "%", G_INS, 10.0),
        RateDef("wet_upturn", "معامل رفعات عزل الحمامات", "×", G_INS, 1.4),
        RateDef("bitumen_f", "معامل عزل الأساسات", "×", G_INS, 1.5),

        RateDef("work_space", "مسافة العمل حول الحفر", "م", G_EARTH, 1.0),
        RateDef("plinth", "ارتفاع الردم الداخلي", "م", G_EARTH, 0.5),

        RateDef("d_exc", "تجهيز الموقع والحفر", "يوم", G_TIME, 15.0),
        RateDef("d_found", "الأساسات", "يوم", G_TIME, 30.0),
        RateDef("d_struct", "هيكل الدور الواحد", "يوم/دور", G_TIME, 21.0),
        RateDef("d_masonry", "مباني الدور الواحد", "يوم/دور", G_TIME, 10.0),
        RateDef("finish_rate", "إنتاجية التشطيب", "م²/يوم", G_TIME, 8.0),
        RateDef("d_ext", "الواجهات والموقع العام", "يوم", G_TIME, 45.0),

        RateDef("q_conc", "القانون السريع للخرسانة", "م³/م²", G_QUICK, 0.30),
        RateDef("q_steel", "القانون السريع للحديد", "كجم/م²", G_QUICK, 30.0)
    )

    private val byKey = defs.associateBy { it.key }
    fun default(key: String): Double = byKey[key]?.default ?: 0.0
    fun defaults(): Map<String, Double> = defs.associate { it.key to it.default }
}

/**
 * سعر وحدة حسب الدولة. [sa] و [eg] قيم بداية فقط.
 * [sy] سعر سوريا بالدولار من مصادر منشورة (2026)؛ إن كان فارغاً فالبداية = سعر السعودية ÷ [Prices.SAR_PER_USD].
 * [refs] الدول التي قيمتها مأخوذة من مصدر منشور؛ غير ذلك افتراضي يجب مراجعته.
 */
data class PriceDef(
    val key: String,
    val label: String,
    val unit: String,
    val group: String,
    val sa: Double,
    val eg: Double,
    val refs: Set<String> = emptySet(),
    val sy: Double? = null
)

object Prices {
    const val G_STRUCT = "الهيكل"
    const val G_WALL = "المباني والواجهات"
    const val G_FIN = "التشطيب الداخلي"
    const val G_INS = "العزل"
    const val G_EARTH = "الحفر والموقع"

    private val SY = setOf("SY")

    val defs: List<PriceDef> = listOf(
        // سوريا: 600–670 $/طن في أغلب المصادر (مايو–يونيو 2026)
        PriceDef("steel", "حديد التسليح", "طن", G_STRUCT, 3300.0, 39500.0, setOf("SA", "EG", "SY"), sy = 660.0),
        // سوريا: بيتون جاهز C30 مع التوصيل 90–115 $/م³ (مايو 2026)
        PriceDef("readymix", "خرسانة جاهزة مسلحة (توريد)", "م³", G_STRUCT, 240.0, 2600.0, SY, sy = 100.0),
        PriceDef("struct_labor", "مصنعيات الهيكل (نجارة وحدادة وصب)", "م³", G_STRUCT, 220.0, 1500.0),
        PriceDef("plain_conc", "خرسانة عادية (توريد وصب)", "م³", G_STRUCT, 260.0, 2300.0),
        PriceDef("hordi_block", "بلوك هوردي", "بلوك", G_STRUCT, 3.5, 12.0),

        // سوريا: بلوك إسمنتي 0.30–0.47 $/قطعة حسب السماكة (مايو 2026)
        PriceDef("block_unit", "بلوك أسمنتي", "بلوك", G_WALL, 2.6, 14.0, SY, sy = 0.40),
        PriceDef("brick_unit", "طوب", "طوبة", G_WALL, 0.6, 2.6),
        // سوريا: بناء جدران البلوك نحو 2.40 $/م² (دمشق، أغسطس 2026)
        PriceDef("masonry_labor", "مصنعية ومونة المباني", "م²", G_WALL, 25.0, 90.0, SY, sy = 2.4),
        PriceDef("ext_finish", "لياسة ودهان خارجي", "م²", G_WALL, 60.0, 250.0),
        PriceDef("cladding", "تكسية الواجهة الرئيسية", "م²", G_WALL, 250.0, 1200.0),

        // سوريا: إكساء اقتصادي 120–180، متوسط 180–300، فاخر 300–500+ $/م² (2026)
        PriceDef("finish_eco", "تشطيب اقتصادي", "م²", G_FIN, 450.0, 3570.0, SY, sy = 135.0),
        PriceDef("finish_mid", "تشطيب متوسط", "م²", G_FIN, 800.0, 5500.0, SY, sy = 215.0),
        PriceDef("finish_lux", "تشطيب فاخر", "م²", G_FIN, 1400.0, 9000.0, SY, sy = 400.0),
        PriceDef("roof_tile", "تبليط السطح", "م²", G_FIN, 70.0, 450.0),

        PriceDef("wp_roof", "عزل مائي للسطح", "م²", G_INS, 35.0, 250.0),
        PriceDef("thermal", "عزل حراري للسطح", "م²", G_INS, 30.0, 220.0),
        PriceDef("wp_wet", "عزل الحمامات والمطابخ", "م²", G_INS, 30.0, 200.0),
        PriceDef("bitumen", "عزل الأساسات (بيتومين)", "م²", G_INS, 12.0, 60.0),

        PriceDef("exc", "الحفر", "م³", G_EARTH, 15.0, 120.0),
        PriceDef("backfill", "الردم والدك", "م³", G_EARTH, 25.0, 180.0),
        PriceDef("repl", "تربة الإحلال (توريد ودك)", "م³", G_EARTH, 60.0, 450.0),
        PriceDef("landscape", "اللاندسكيب", "م²", G_EARTH, 200.0, 1500.0)
    )

    /** سعر ربط الريال السعودي بالدولار، لتحويل أرقام البداية السعودية إلى دولار. */
    const val SAR_PER_USD = 3.75

    private val byKey = defs.associateBy { it.key }
    fun label(key: String): String = byKey[key]?.label ?: key

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    /** أسعار البداية لدولة؛ الدول بدون بيانات تبدأ بصفر حتى يدخلها المستخدم. */
    fun defaultsFor(country: String): Map<String, Double> = defs.associate {
        it.key to when (country) {
            Countries.SYRIA -> it.sy ?: round2(it.sa / SAR_PER_USD)
            "SA" -> it.sa
            "EG" -> it.eg
            else -> 0.0
        }
    }

    fun hasDefaults(country: String) = country == Countries.SYRIA || country == "SA" || country == "EG"
}
