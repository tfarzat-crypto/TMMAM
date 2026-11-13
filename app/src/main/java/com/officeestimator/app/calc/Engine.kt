package com.officeestimator.app.calc

import com.officeestimator.app.data.FinishLevel
import com.officeestimator.app.data.FoundationType
import com.officeestimator.app.data.Prices
import com.officeestimator.app.data.Project
import com.officeestimator.app.data.Rates
import com.officeestimator.app.data.SlabType
import com.officeestimator.app.data.WallUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** بند كمية. */
data class Q(val label: String, val qty: Double, val unit: String, val note: String = "")

/** بند تكلفة. */
data class C(val label: String, val amount: Double, val note: String = "")

data class Phase(val name: String, val start: Int, val days: Int, val cost: Double) {
    val end: Int get() = start + days
}

class Estimate(
    val built: Double,
    val footprint: Double,
    val perimeter: Double,
    val height: Double,
    val rc: Double,
    val steelTon: Double,
    val pc: Double,
    val structure: List<Q>,
    val steel: List<Q>,
    val foundation: List<Q>,
    val earth: List<Q>,
    val masonry: List<Q>,
    val finishes: List<Q>,
    val insulation: List<Q>,
    val materials: List<Q>,
    val quick: List<Q>,
    val costs: List<C>,
    val execution: Double,
    val costPerM2: Double,
    val landCost: Double,
    val otherCost: Double,
    val total: Double,
    val sellable: Double,
    val revenue: Double,
    val profit: Double,
    val roiPct: Double,
    val phases: List<Phase>,
    val months: List<Double>,
    val durationDays: Int,
    val missingPrices: List<String>
)

object Engine {

    fun compute(p: Project, rates: Map<String, Double>, prices: Map<String, Double>): Estimate {
        fun r(k: String): Double = rates[k] ?: Rates.default(k)
        val missing = LinkedHashSet<String>()
        fun cost(k: String, qty: Double): Double {
            val unit = prices[k] ?: 0.0
            if (unit <= 0.0 && qty > 0.0) missing.add(Prices.label(k))
            return unit * qty
        }

        // ---------- الهندسة ----------
        val fp = max(0.0, p.footprint)
        val floors = max(1, p.floors)
        val annex = fp * max(0.0, p.annexPct) / 100.0
        val built = fp * floors + annex
        val perim = if (p.perimeter > 0) p.perimeter else 4 * sqrt(fp)
        val height = floors * p.floorHeight
        val raft = p.foundation == FoundationType.RAFT

        // ---------- الهيكل ----------
        val slabRate = when (p.slab) {
            SlabType.SOLID -> r("slab_solid")
            SlabType.HORDI -> r("slab_hordi")
            SlabType.FLAT -> r("slab_flat")
        }
        val slabSteel = when (p.slab) {
            SlabType.SOLID -> r("st_solid")
            SlabType.HORDI -> r("st_hordi")
            SlabType.FLAT -> r("st_flat")
        }
        val slabs = built * slabRate
        val cols = built * r("col")
        val stairs = floors * r("stair")
        val iso = fp * (r("found_iso_floor") * floors + r("tie_beams"))
        val raftThk = r("raft_base") + r("raft_per_floor") * floors
        val found = when (p.foundation) {
            FoundationType.ISOLATED -> iso
            FoundationType.STRIP -> iso * r("strip_factor")
            FoundationType.RAFT -> fp * 1.05 * raftThk
        }
        val rc = slabs + cols + stairs + found
        val blinding = fp * (if (raft) 1.1 else 0.6) * r("blinding_thk")
        val sog = fp * r("sog_thk")
        val pc = blinding + sog
        val hordiBlocks = if (p.slab == SlabType.HORDI) built * r("hordi_blocks") else 0.0

        val foundSteelRate = if (raft) r("st_raft") else r("st_found")
        val kgFound = found * foundSteelRate
        val kgCols = cols * r("st_col")
        val kgSlabs = slabs * slabSteel
        val kgStairs = stairs * r("st_stair")
        val wasteF = 1 + r("st_waste") / 100.0
        val steelTon = (kgFound + kgCols + kgSlabs + kgStairs) * wasteF / 1000.0

        val structure = mutableListOf(
            Q("الأسقف والكمرات", slabs, "م³", "${p.slab.ar} — ${trim(slabRate)} م³/م²"),
            Q("الأعمدة", cols, "م³", "${trim(r("col"))} م³/م²"),
            Q("السلالم", stairs, "م³", "${trim(r("stair"))} م³ لكل دور"),
            Q("الأساسات", found, "م³", p.foundation.ar),
            Q("إجمالي الخرسانة المسلحة", rc, "م³")
        )
        if (hordiBlocks > 0) structure.add(Q("بلوك الهوردي", hordiBlocks, "بلوك"))

        val steel = listOf(
            Q("حديد الأساسات", kgFound * wasteF / 1000, "طن", "${trim(foundSteelRate)} كجم/م³"),
            Q("حديد الأعمدة", kgCols * wasteF / 1000, "طن", "${trim(r("st_col"))} كجم/م³"),
            Q("حديد الأسقف", kgSlabs * wasteF / 1000, "طن", "${trim(slabSteel)} كجم/م³"),
            Q("حديد السلالم", kgStairs * wasteF / 1000, "طن", "${trim(r("st_stair"))} كجم/م³"),
            Q("إجمالي الحديد", steelTon, "طن", "شامل هالك ${trim(r("st_waste"))}%")
        )

        val foundation = mutableListOf(
            Q("خرسانة الأساسات المسلحة", found, "م³", p.foundation.ar),
            Q("خرسانة النظافة", blinding, "م³", "سمك ${trim(r("blinding_thk"))} م"),
            Q("خرسانة الأرضية", sog, "م³", "سمك ${trim(r("sog_thk"))} م"),
            Q("حديد الأساسات", kgFound * wasteF / 1000, "طن")
        )
        if (raft) foundation.add(1, Q("سمك اللبشة التقديري", raftThk, "م"))

        // ---------- الحفر والردم ----------
        val ws = r("work_space")
        val excArea = fp + perim * ws + 4 * ws * ws
        val replVol = excArea * max(0.0, p.replThickness)
        val excVol = excArea * max(0.0, p.excDepth) + replVol
        val outerFill = max(0.0, excArea * p.excDepth - found - blinding)
        val innerFill = fp * r("plinth")
        val backfill = outerFill + innerFill
        val earth = listOf(
            Q("مسطح الحفر", excArea, "م²", "مسافة عمل ${trim(ws)} م حول المبنى"),
            Q("حجم الحفر", excVol, "م³", "عمق ${trim(p.excDepth)} م" +
                if (replVol > 0) " + إحلال ${trim(p.replThickness)} م" else ""),
            Q("تربة الإحلال", replVol, "م³"),
            Q("الردم حول الأساسات", outerFill, "م³"),
            Q("الردم الداخلي", innerFill, "م³", "ارتفاع ${trim(r("plinth"))} م"),
            Q("إجمالي الردم", backfill, "م³")
        )

        // ---------- المباني ----------
        val extWall = perim * height * (1 - r("open_ext") / 100.0) + perim * r("parapet_h")
        val intWall = built * r("int_wall")
        val walls = extWall + intWall
        val wallWaste = 1 + r("wall_waste") / 100.0
        val units = when (p.wallUnit) {
            WallUnit.BLOCK -> walls * r("block_m2")
            WallUnit.BRICK -> extWall * r("brick_full") + intWall * r("brick_half")
        } * wallWaste
        val unitName = if (p.wallUnit == WallUnit.BLOCK) "بلوك" else "طوبة"
        val masonry = listOf(
            Q("حوائط خارجية", extWall, "م²", "بعد خصم فتحات ${trim(r("open_ext"))}% + دروة السطح"),
            Q("حوائط داخلية", intWall, "م²", "${trim(r("int_wall"))} م² لكل م² مبني"),
            Q("إجمالي مسطح الحوائط", walls, "م²"),
            Q("عدد ${p.wallUnit.ar}", units, unitName, "شامل هالك ${trim(r("wall_waste"))}%"),
            Q("أسمنت المونة", walls * r("mortar_cement"), "شكارة"),
            Q("رمل المونة", walls * r("mortar_sand"), "م³")
        )

        // ---------- التشطيبات ----------
        val intPlaster = intWall * 2 + extWall + built
        val cladding = if (p.facadeWidth > 0)
            p.facadeWidth * height * (1 - r("facade_open") / 100.0) else 0.0
        val extPlaster = max(0.0, extWall - cladding)
        val floorTiles = built * (1 + r("tile_waste") / 100.0)
        val wallTiles = built * r("wall_tile")
        val roofTiles = if (p.roofTiling) fp else 0.0
        val finishes = mutableListOf(
            Q("لياسة داخلية (حوائط وأسقف)", intPlaster, "م²"),
            Q("دهانات داخلية", intPlaster - wallTiles, "م²"),
            Q("لياسة ودهان خارجي", extPlaster, "م²"),
            Q("تكسية الواجهة الرئيسية", cladding, "م²", "عرض ${trim(p.facadeWidth)} م × ارتفاع ${trim(height)} م"),
            Q("بلاط الأرضيات", floorTiles, "م²", "شامل هالك ${trim(r("tile_waste"))}%"),
            Q("بلاط الحوائط", wallTiles, "م²"),
            Q("أسمنت اللياسة", (intPlaster + extPlaster) * r("plaster_cement"), "شكارة"),
            Q("رمل اللياسة", (intPlaster + extPlaster) * r("plaster_sand"), "م³")
        )
        if (roofTiles > 0) finishes.add(Q("تبليط السطح", roofTiles, "م²"))

        // ---------- العزل ----------
        val ins = if (p.includeInsulation) 1.0 else 0.0
        val wpRoof = fp * r("wp_roof_f") * ins
        val thermal = fp * ins
        val wpWet = built * r("wet_ratio") / 100.0 * r("wet_upturn") * ins
        val bitumen = fp * r("bitumen_f") * ins
        val insulation = if (!p.includeInsulation) emptyList() else listOf(
            Q("عزل مائي للسطح", wpRoof, "م²", "شامل الرفعات"),
            Q("عزل حراري للسطح", thermal, "م²"),
            Q("عزل الحمامات والمطابخ", wpWet, "م²", "${trim(r("wet_ratio"))}% من المسطح"),
            Q("عزل الأساسات", bitumen, "م²")
        )

        // ---------- الخامات ----------
        val cementBags = rc * r("mix_cement") + pc * r("pc_cement") +
            walls * r("mortar_cement") + (intPlaster + extPlaster) * r("plaster_cement")
        val sand = (rc + pc) * r("mix_sand") + walls * r("mortar_sand") +
            (intPlaster + extPlaster) * r("plaster_sand")
        val gravel = (rc + pc) * r("mix_gravel")
        val materials = mutableListOf(
            Q("الخرسانة المسلحة", rc, "م³"),
            Q("الخرسانة العادية", pc, "م³", "نظافة وأرضيات"),
            Q("حديد التسليح", steelTon, "طن"),
            Q("الأسمنت", cementBags, "شكارة", "خرسانة ومونة ولياسة"),
            Q("الأسمنت بالطن", cementBags * 50 / 1000, "طن"),
            Q("الرمل", sand, "م³"),
            Q("الركام", gravel, "م³"),
            Q(p.wallUnit.ar, units, unitName),
            Q("بلاط الأرضيات والحوائط", floorTiles + wallTiles + roofTiles, "م²")
        )
        if (hordiBlocks > 0) materials.add(Q("بلوك الهوردي", hordiBlocks, "بلوك"))

        // ---------- التحقق السريع ----------
        val quick = listOf(
            Q("الخرسانة بالقانون السريع", fp * (floors + 1) * r("q_conc"), "م³",
                "مسطح الدور × (الأدوار + 1) × ${trim(r("q_conc"))}"),
            Q("الحديد بالقانون السريع", built * r("q_steel") / 1000, "طن",
                "${trim(r("q_steel"))} كجم/م² مبني"),
            Q("معدل الخرسانة الفعلي", if (built > 0) rc / built else 0.0, "م³/م²"),
            Q("معدل الحديد الفعلي", if (rc > 0) steelTon * 1000 / rc else 0.0, "كجم/م³")
        )

        // ---------- التكلفة ----------
        val cRc = cost("readymix", rc) + cost("struct_labor", rc)
        val cSteel = cost("steel", steelTon)
        val cPc = cost("plain_conc", pc)
        val cHordi = cost("hordi_block", hordiBlocks)
        val cStructure = cRc + cSteel + cPc + cHordi
        val cUnits = cost(if (p.wallUnit == WallUnit.BLOCK) "block_unit" else "brick_unit", units)
        val cMasonry = cUnits + cost("masonry_labor", walls)
        val cFacade = cost("ext_finish", extPlaster) + cost("cladding", cladding)
        val finishKey = when (p.finish) {
            FinishLevel.NONE -> null
            FinishLevel.ECONOMY -> "finish_eco"
            FinishLevel.MEDIUM -> "finish_mid"
            FinishLevel.LUXURY -> "finish_lux"
        }
        val cFinish = if (finishKey == null) 0.0 else cost(finishKey, built)
        val cRoofTile = cost("roof_tile", roofTiles)
        val cInsul = cost("wp_roof", wpRoof) + cost("thermal", thermal) +
            cost("wp_wet", wpWet) + cost("bitumen", bitumen)
        val cEarth = cost("exc", excVol) + cost("backfill", backfill) + cost("repl", replVol)
        val lsArea = if (p.includeLandscape) max(0.0, p.landscapeArea) else 0.0
        val cLandscape = cost("landscape", lsArea)

        val subtotal = cStructure + cMasonry + cFacade + cFinish + cRoofTile + cInsul + cEarth + cLandscape
        val contingency = subtotal * max(0.0, p.contingencyPct) / 100.0
        val execution = subtotal + contingency

        val costs = mutableListOf(
            C("الهيكل الخرساني", cStructure, "خرسانة ${trim(rc)} م³ + حديد ${trim(steelTon)} طن + خرسانة عادية"),
            C("المباني", cMasonry, "${trim(walls)} م² حوائط"),
            C("الواجهات", cFacade, "لياسة ودهان خارجي + تكسية"),
            C("التشطيب الداخلي", cFinish, if (finishKey == null) "عظم — بدون تشطيب"
            else "${p.finish.ar} × ${trim(built)} م²"),
            C("العزل", cInsul, if (p.includeInsulation) "سطح وحمامات وأساسات" else "غير مشمول — باختيار المستخدم"),
            C("الحفر والردم والإحلال", cEarth, "حفر ${trim(excVol)} م³")
        )
        if (roofTiles > 0) costs.add(4, C("تبليط السطح", cRoofTile, "${trim(roofTiles)} م²"))
        if (lsArea > 0) costs.add(C("اللاندسكيب", cLandscape, "${trim(lsArea)} م²"))
        if (contingency > 0) costs.add(C("احتياطي ونثريات", contingency, "${trim(p.contingencyPct)}%"))

        // ---------- الأرض والربح ----------
        val landCost = p.landArea * p.landPricePerM2
        val otherCost = execution * max(0.0, p.otherPct) / 100.0
        val total = execution + landCost + otherCost
        val sellable = built * p.sellPct / 100.0
        val revenue = sellable * p.salePricePerM2
        val profit = revenue - total
        val roi = if (total > 0 && revenue > 0) profit / total * 100 else 0.0

        // ---------- الجدول الزمني ----------
        val scale = if (subtotal > 0) execution / subtotal else 1.0
        val foundShare = if (rc > 0) found / rc else 0.0
        val cFoundPhase = cStructure * foundShare
        val dExc = max(1, r("d_exc").toInt())
        val dFound = max(1, r("d_found").toInt())
        val dFloor = max(1, r("d_struct").toInt())
        val dStruct = floors * dFloor + if (annex > 0) dFloor / 2 else 0
        val sStruct = dExc + dFound
        val eStruct = sStruct + dStruct
        val dMas = floors * max(1, r("d_masonry").toInt())
        var sMas = if (floors >= 3) sStruct + 2 * dFloor else eStruct
        sMas = max(sMas, eStruct + max(1, r("d_masonry").toInt()) - dMas)
        val eMas = sMas + dMas
        val dFin = if (finishKey == null) 30
        else max(60, ceil(built / max(1.0, r("finish_rate"))).toInt())
        val sFin = max(sMas + dMas / 2, eMas + 30 - dFin)
        val eFin = sFin + dFin
        val dExt = max(1, r("d_ext").toInt())
        val sExt = max(eMas, eFin - dExt / 2)
        val phases = listOf(
            Phase("تجهيز الموقع والحفر", 0, dExc, cEarth * scale),
            Phase("الأساسات", dExc, dFound, cFoundPhase * scale),
            Phase("الهيكل الخرساني", sStruct, dStruct, (cStructure - cFoundPhase) * scale),
            Phase("المباني", sMas, dMas, cMasonry * scale),
            Phase("التشطيبات والعزل", sFin, dFin, (cFinish + cRoofTile + cInsul) * scale),
            Phase("الواجهات والموقع العام", sExt, dExt, (cFacade + cLandscape) * scale)
        )
        val duration = phases.maxOf { it.end }
        val monthCount = ceil(duration / 30.0).toInt()
        val months = DoubleArray(monthCount)
        for (ph in phases) {
            val perDay = ph.cost / ph.days
            for (d in ph.start until ph.end) months[d / 30] += perDay
        }

        return Estimate(
            built = built, footprint = fp, perimeter = perim, height = height,
            rc = rc, steelTon = steelTon, pc = pc,
            structure = structure, steel = steel, foundation = foundation, earth = earth,
            masonry = masonry, finishes = finishes, insulation = insulation,
            materials = materials, quick = quick,
            costs = costs, execution = execution,
            costPerM2 = if (built > 0) execution / built else 0.0,
            landCost = landCost, otherCost = otherCost, total = total,
            sellable = sellable, revenue = revenue, profit = profit, roiPct = roi,
            phases = phases, months = months.toList(), durationDays = duration,
            missingPrices = missing.toList()
        )
    }

    /** رقم مختصر بدون أصفار زائدة. */
    fun trim(v: Double): String {
        val s = String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
        return if (s.isEmpty() || s == "-0") "0" else s
    }
}
