package com.officeestimator.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.officeestimator.app.calc.Engine
import com.officeestimator.app.calc.Estimate
import com.officeestimator.app.data.Countries
import com.officeestimator.app.data.Country
import com.officeestimator.app.data.OfficeInfo
import com.officeestimator.app.data.Prices
import com.officeestimator.app.data.Project
import com.officeestimator.app.data.Rates
import com.officeestimator.app.data.Store
import java.util.UUID

enum class Screen(val title: String, val isTab: Boolean = false) {
    HOME("الرئيسية", true),
    PROJECTS("المشاريع", true),
    REPORT("التقرير", true),
    PRICES("الأسعار", true),
    SETTINGS("المكتب", true),
    PROJECT_DATA("بيانات المشروع"),
    COST("تكلفة المبنى"),
    LAND("الأرض والربح والبيع"),
    STRUCTURE("الهيكل الخرساني"),
    FOUNDATION("الأساسات والحفر"),
    MASONRY("المباني والتشطيبات"),
    INSULATION("العزل"),
    SCHEDULE("الجدول الزمني والتدفق النقدي"),
    FACADE("واجهة المبنى"),
    MATERIALS("إجمالي الخامات"),
    RATES("المعدلات")
}

class AppState(private val store: Store) {
    var projects by mutableStateOf(store.loadProjects())
        private set
    var currentId by mutableStateOf(store.currentId())
        private set
    var rates by mutableStateOf(store.loadRates())
        private set
    var prices by mutableStateOf(store.loadPrices())
        private set
    var office by mutableStateOf(store.loadOffice())
        private set
    var dark by mutableStateOf(store.dark())
        private set
    var screen by mutableStateOf(Screen.HOME)

    init {
        if (projects.isEmpty()) {
            val p = newProject("مشروعي الأول")
            projects = listOf(p)
            currentId = p.id
            store.saveProjects(projects)
            store.saveCurrentId(p.id)
        } else if (projects.none { it.id == currentId }) {
            currentId = projects.first().id
        }
    }

    val project: Project
        get() = projects.firstOrNull { it.id == currentId } ?: projects.first()

    val country: Country get() = Countries.byCode(project.countryCode)

    /** عملة إدخال الأسعار وسعر الأرض والبيع (سوريا تُدخل بالدولار دائماً). */
    val inputCurrency: String
        get() = when (country.code) {
            Countries.OTHER -> office.customCurrency.ifBlank { "عملة" }
            Countries.SYRIA_SYP -> Countries.USD
            else -> country.currency
        }

    val isSypCountry: Boolean get() = project.countryCode == Countries.SYRIA_SYP
    val isSyria: Boolean get() = isSypCountry || project.countryCode == Countries.SYRIA

    /** «سوريا ليرة» و«سوريا دولار» تتشاركان قائمة أسعار واحدة بالدولار. */
    val priceCode: String get() = if (isSypCountry) Countries.SYRIA else project.countryCode

    /** العرض بالليرة السورية مفعّل: الدولة «سوريا ليرة» وسعر الصرف مُدخل. */
    val sypActive: Boolean get() = isSypCountry && office.sypRate > 0.0

    /** معامل تحويل المبالغ من عملة الإدخال إلى عملة العرض. */
    val fx: Double get() = if (sypActive) office.sypRate else 1.0

    /** عملة عرض النتائج والتقرير. */
    val currency: String get() = if (sypActive) Countries.SYP else inputCurrency

    val countryPrices: Map<String, Double>
        get() = prices[priceCode] ?: Prices.defaultsFor(priceCode)

    val estimate: Estimate
        get() {
            val f = fx
            if (f == 1.0) return Engine.compute(project, rates, countryPrices)
            val p = project
            return Engine.compute(
                p.copy(landPricePerM2 = p.landPricePerM2 * f, salePricePerM2 = p.salePricePerM2 * f),
                rates, countryPrices.mapValues { it.value * f }
            )
        }

    private fun newProject(name: String) =
        Project(id = UUID.randomUUID().toString(), name = name, createdAt = System.currentTimeMillis())

    fun update(change: (Project) -> Project) {
        val id = project.id
        projects = projects.map { if (it.id == id) change(it) else it }
        store.saveProjects(projects)
    }

    fun select(id: String) {
        currentId = id
        store.saveCurrentId(id)
    }

    fun addProject() {
        val p = newProject("مشروع ${projects.size + 1}").copy(countryCode = project.countryCode)
        projects = projects + p
        store.saveProjects(projects)
        select(p.id)
    }

    fun duplicate(src: Project) {
        val p = src.copy(
            id = UUID.randomUUID().toString(), name = src.name + " (نسخة)",
            createdAt = System.currentTimeMillis()
        )
        projects = projects + p
        store.saveProjects(projects)
        select(p.id)
    }

    fun delete(id: String) {
        if (projects.size <= 1) return
        projects = projects.filter { it.id != id }
        store.saveProjects(projects)
        if (currentId == id) select(projects.first().id)
    }

    fun setRate(key: String, v: Double) {
        rates = rates + (key to v)
        store.saveRates(rates)
    }

    fun resetRates() {
        rates = Rates.defaults()
        store.saveRates(rates)
    }

    fun setPrice(key: String, v: Double) {
        val code = priceCode
        prices = prices + (code to (countryPrices + (key to v)))
        store.savePrices(prices)
    }

    fun resetPrices(fromCountry: String = priceCode) {
        prices = prices + (priceCode to Prices.defaultsFor(fromCountry))
        store.savePrices(prices)
    }

    fun setSypRate(v: Double) = changeOffice(office.copy(sypRate = if (v > 0.0) v else 0.0))

    fun changeOffice(o: OfficeInfo) {
        office = o
        store.saveOffice(o)
    }

    fun toggleDark() {
        dark = !dark
        store.saveDark(dark)
    }
}
