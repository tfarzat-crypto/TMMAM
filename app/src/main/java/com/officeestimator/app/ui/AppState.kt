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

    val currency: String
        get() = if (country.code == Countries.OTHER) office.customCurrency.ifBlank { "عملة" }
        else country.currency

    val countryPrices: Map<String, Double>
        get() = prices[project.countryCode] ?: Prices.defaultsFor(project.countryCode)

    val estimate: Estimate get() = Engine.compute(project, rates, countryPrices)

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
        val code = project.countryCode
        prices = prices + (code to (countryPrices + (key to v)))
        store.savePrices(prices)
    }

    fun resetPrices(fromCountry: String = project.countryCode) {
        prices = prices + (project.countryCode to Prices.defaultsFor(fromCountry))
        store.savePrices(prices)
    }

    fun changeOffice(o: OfficeInfo) {
        office = o
        store.saveOffice(o)
    }

    fun toggleDark() {
        dark = !dark
        store.saveDark(dark)
    }
}
