package com.officeestimator.app.data

enum class SlabType(val ar: String) {
    SOLID("مصمت بكمرات"), HORDI("هوردي"), FLAT("فلات سلاب")
}

enum class FoundationType(val ar: String) {
    ISOLATED("قواعد منفصلة + ميدات"), STRIP("قواعد شريطية"), RAFT("لبشة")
}

enum class FinishLevel(val ar: String) {
    NONE("عظم"), ECONOMY("اقتصادي"), MEDIUM("متوسط"), LUXURY("فاخر")
}

enum class WallUnit(val ar: String) {
    BLOCK("بلوك 20×20×40"), BRICK("طوب 25×12×6")
}

data class Country(val code: String, val name: String, val currency: String)

object Countries {
    const val OTHER = "XX"
    val all = listOf(
        Country("SA", "السعودية", "ريال"),
        Country("EG", "مصر", "جنيه"),
        Country("AE", "الإمارات", "درهم"),
        Country("KW", "الكويت", "دينار"),
        Country("QA", "قطر", "ريال"),
        Country("BH", "البحرين", "دينار"),
        Country("OM", "عُمان", "ريال"),
        Country("JO", "الأردن", "دينار"),
        Country(OTHER, "دولة أخرى", "")
    )

    fun byCode(code: String): Country = all.firstOrNull { it.code == code } ?: all.first()
}

data class Project(
    val id: String,
    val name: String = "مشروع جديد",
    val client: String = "",
    val countryCode: String = "SA",
    val landArea: Double = 400.0,
    val footprint: Double = 200.0,
    val floors: Int = 2,
    val annexPct: Double = 0.0,
    val floorHeight: Double = 3.2,
    val perimeter: Double = 0.0,
    val facadeWidth: Double = 15.0,
    val slab: SlabType = SlabType.HORDI,
    val foundation: FoundationType = FoundationType.ISOLATED,
    val finish: FinishLevel = FinishLevel.MEDIUM,
    val wallUnit: WallUnit = WallUnit.BLOCK,
    val excDepth: Double = 1.5,
    val replThickness: Double = 0.0,
    val roofTiling: Boolean = false,
    val includeLandscape: Boolean = false,
    val landscapeArea: Double = 0.0,
    val contingencyPct: Double = 5.0,
    val landPricePerM2: Double = 0.0,
    val otherPct: Double = 10.0,
    val sellPct: Double = 85.0,
    val salePricePerM2: Double = 0.0,
    val createdAt: Long = 0L
)

data class OfficeInfo(
    val officeName: String = "",
    val engineer: String = "",
    val phone: String = "",
    val email: String = "",
    val logoPath: String = "",
    val customCurrency: String = ""
)
