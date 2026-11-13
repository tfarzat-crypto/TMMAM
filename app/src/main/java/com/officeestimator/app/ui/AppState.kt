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
    DASHBOARD("المشروع", true),
    REPORT("التقرير", true),
    PRICES("الأسعار", true),
    SETTINGS("المكتب", true),
    PROJECT_DATA("بيانات المشروع"),
    QUANTITIES("الكميات التقديرية"),
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

    /** وضع الخطوات: بيانات ← كميات ← أسعار ← تقرير. */
    var wizard by mutableStateOf(false)
        private set

    init {
        if (projects.isNotEmpty() && projects.none { it.id == currentId }) {
            currentId = projects.first().id
        }
    }

    val hasProject: Boolean get() = projects.isNotEmpty()

    /** مشروع فارغ يُستخدم فقط قبل إنشاء أول مشروع؛ لا يُحفظ. */
    private val blank = Project(id = "")

    val project: Project
        get() = projects.firstOrNull { it.id == currentId } ?: projects.firstOrNull() ?: blank

    val wizardStep: Int
        get() = when (screen) {
            Screen.PROJECT_DATA -> 1
            Screen.QUANTITIES -> 2
            Screen.PRICES -> 3
            Screen.REPORT -> 4
            else -> 0
        }

    /** مشروع جديد يبدأ من خطوة البيانات. */
    fun startNew() {
        addProject()
        wizard = true
        screen = Screen.PROJECT_DATA
    }

    /** إعادة المرور على خطوات مشروع موجود. */
    fun edit(id: String) {
        select(id)
        wizard = true
        screen = Screen.PROJECT_DATA
    }

    fun open(id: String) {
        select(id)
        goTab(Screen.DASHBOARD)
    }

    fun goTab(s: Screen) {
        wizard = false
        screen = s
    }

    fun step(s: Screen) {
        screen = s
    }

    fun back() {
        if (wizard) {
            when (screen) {
                Screen.QUANTITIES -> screen = Screen.PROJECT_DATA
                Screen.PRICES -> screen = Screen.QUANTITIES
                Screen.REPORT -> screen = Screen.PRICES
                else -> goTab(Screen.HOME)
            }
        } else if (screen.isTab || !hasProject) {
            screen = Screen.HOME
        } else {
            screen = Screen.DASHBOARD
        }
    }

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
        projects = projects.filter { it.id != id }
        store.saveProjects(projects)
        if (currentId == id) select(projects.firstOrNull()?.id ?: "")
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
