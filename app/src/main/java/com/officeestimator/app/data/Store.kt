package com.officeestimator.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** تخزين محلي بسيط (SharedPreferences + JSON) — يعمل بدون إنترنت. */
class Store(context: Context) {
    private val sp = context.getSharedPreferences("estimator", Context.MODE_PRIVATE)

    // ----- المشاريع -----
    fun loadProjects(): List<Project> {
        val raw = sp.getString("projects", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProjects(list: List<Project>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        sp.edit().putString("projects", arr.toString()).apply()
    }

    fun currentId(): String = sp.getString("current", "") ?: ""
    fun saveCurrentId(id: String) = sp.edit().putString("current", id).apply()

    // ----- المعدلات -----
    fun loadRates(): Map<String, Double> {
        val base = Rates.defaults().toMutableMap()
        val raw = sp.getString("rates", null) ?: return base
        try {
            val o = JSONObject(raw)
            for (k in o.keys()) if (base.containsKey(k)) base[k] = o.getDouble(k)
        } catch (_: Exception) {
        }
        return base
    }

    fun saveRates(m: Map<String, Double>) {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, v) }
        sp.edit().putString("rates", o.toString()).apply()
    }

    // ----- الأسعار حسب الدولة -----
    fun loadPrices(): Map<String, Map<String, Double>> {
        val result = HashMap<String, Map<String, Double>>()
        val saved = try {
            JSONObject(sp.getString("prices", null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        for (c in Countries.all) {
            val base = Prices.defaultsFor(c.code).toMutableMap()
            val o = saved.optJSONObject(c.code)
            if (o != null) for (k in o.keys()) if (base.containsKey(k)) base[k] = o.optDouble(k, 0.0)
            result[c.code] = base
        }
        return result
    }

    fun savePrices(m: Map<String, Map<String, Double>>) {
        val root = JSONObject()
        m.forEach { (country, map) ->
            val o = JSONObject()
            map.forEach { (k, v) -> o.put(k, v) }
            root.put(country, o)
        }
        sp.edit().putString("prices", root.toString()).apply()
    }

    // ----- بيانات المكتب -----
    fun loadOffice(): OfficeInfo = OfficeInfo(
        officeName = Office.NAME,
        engineer = Office.ENGINEER,
        phone = sp.getString("o_phone", "") ?: "",
        email = sp.getString("o_email", "") ?: "",
        logoPath = sp.getString("o_logo", "") ?: "",
        customCurrency = sp.getString("o_cur", "") ?: "",
        sypRate = sp.getFloat("o_syp_rate", 0f).toDouble()
    )

    fun saveOffice(o: OfficeInfo) = sp.edit()
        .putFloat("o_syp_rate", o.sypRate.toFloat())
        .putString("o_phone", o.phone).putString("o_email", o.email)
        .putString("o_logo", o.logoPath).putString("o_cur", o.customCurrency).apply()

    fun dark(): Boolean = sp.getBoolean("dark", false)
    fun saveDark(v: Boolean) = sp.edit().putBoolean("dark", v).apply()

    // ----- JSON -----
    private fun toJson(p: Project): JSONObject = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("client", p.client)
        put("country", p.countryCode); put("useSyp", p.useSyp); put("landArea", p.landArea)
        put("footprint", p.footprint); put("floors", p.floors)
        put("annexPct", p.annexPct); put("floorHeight", p.floorHeight)
        put("perimeter", p.perimeter); put("facadeWidth", p.facadeWidth)
        put("slab", p.slab.name); put("foundation", p.foundation.name)
        put("finish", p.finish.name); put("wallUnit", p.wallUnit.name)
        put("excDepth", p.excDepth); put("replThickness", p.replThickness)
        put("roofTiling", p.roofTiling); put("includeLandscape", p.includeLandscape)
        put("landscapeArea", p.landscapeArea); put("contingencyPct", p.contingencyPct)
        put("landPrice", p.landPricePerM2); put("otherPct", p.otherPct)
        put("sellPct", p.sellPct); put("salePrice", p.salePricePerM2)
        put("createdAt", p.createdAt)
    }

    private fun fromJson(o: JSONObject): Project {
        val d = Project(id = o.optString("id", "p"))
        return Project(
            id = d.id,
            name = o.optString("name", d.name),
            client = o.optString("client", ""),
            countryCode = o.optString("country", d.countryCode),
            useSyp = o.optBoolean("useSyp", false),
            landArea = o.optDouble("landArea", d.landArea),
            footprint = o.optDouble("footprint", d.footprint),
            floors = o.optInt("floors", d.floors),
            annexPct = o.optDouble("annexPct", d.annexPct),
            floorHeight = o.optDouble("floorHeight", d.floorHeight),
            perimeter = o.optDouble("perimeter", d.perimeter),
            facadeWidth = o.optDouble("facadeWidth", d.facadeWidth),
            slab = enumOr(o.optString("slab"), d.slab),
            foundation = enumOr(o.optString("foundation"), d.foundation),
            finish = enumOr(o.optString("finish"), d.finish),
            wallUnit = enumOr(o.optString("wallUnit"), d.wallUnit),
            excDepth = o.optDouble("excDepth", d.excDepth),
            replThickness = o.optDouble("replThickness", d.replThickness),
            roofTiling = o.optBoolean("roofTiling", false),
            includeLandscape = o.optBoolean("includeLandscape", false),
            landscapeArea = o.optDouble("landscapeArea", 0.0),
            contingencyPct = o.optDouble("contingencyPct", d.contingencyPct),
            landPricePerM2 = o.optDouble("landPrice", 0.0),
            otherPct = o.optDouble("otherPct", d.otherPct),
            sellPct = o.optDouble("sellPct", d.sellPct),
            salePricePerM2 = o.optDouble("salePrice", 0.0),
            createdAt = o.optLong("createdAt", 0L)
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        try {
            enumValueOf<T>(name)
        } catch (_: Exception) {
            fallback
        }
}
