package com.officeestimator.app.report

import com.officeestimator.app.calc.Estimate
import com.officeestimator.app.calc.Q
import com.officeestimator.app.data.OfficeInfo
import com.officeestimator.app.data.Project
import java.util.Locale

object Fmt {
    fun money(v: Double): String = String.format(Locale.US, "%,.0f", v)

    fun qty(v: Double): String = when {
        v == 0.0 -> "0"
        kotlin.math.abs(v) >= 1000 -> String.format(Locale.US, "%,.0f", v)
        kotlin.math.abs(v) >= 100 -> String.format(Locale.US, "%.0f", v)
        kotlin.math.abs(v) >= 10 -> String.format(Locale.US, "%.1f", v)
        else -> String.format(Locale.US, "%.2f", v)
    }

    fun short(v: Double): String = when {
        kotlin.math.abs(v) >= 1_000_000 -> String.format(Locale.US, "%.2f مليون", v / 1_000_000)
        else -> money(v)
    }
}

object ReportBuilder {

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun html(
        p: Project, e: Estimate, office: OfficeInfo,
        countryName: String, currency: String, date: String, logoBase64: String?,
        fxNote: String = ""
    ): String {
        val sb = StringBuilder()
        sb.append(
            """<!DOCTYPE html><html lang="ar" dir="rtl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
body{font-family:sans-serif;direction:rtl;text-align:right;color:#16302d;margin:16px;font-size:13px}
h1{font-size:20px;margin:0 0 4px} h2{font-size:15px;margin:22px 0 6px;color:#0f766e;border-bottom:2px solid #0f766e;padding-bottom:4px}
table{width:100%;border-collapse:collapse;direction:rtl;margin-bottom:6px}
th,td{border:1px solid #cfdad8;padding:6px 8px;text-align:right;vertical-align:top}
th{background:#e6f2f0} td.n{white-space:nowrap;font-weight:bold;direction:ltr;text-align:right}
tr.t td{background:#f3ead7;font-weight:bold}
.head{display:flex;align-items:center;gap:12px;border-bottom:3px solid #0f766e;padding-bottom:10px;margin-bottom:10px}
.head img{max-height:64px;max-width:120px} .muted{color:#5d716e;font-size:12px}
.note{color:#5d716e;font-size:11px} .foot{margin-top:24px;font-size:11px;color:#5d716e;border-top:1px solid #cfdad8;padding-top:8px}
.warn{background:#fdecea;border:1px solid #e6b0aa;padding:8px;margin:8px 0}
</style></head><body>"""
        )
        sb.append("<div class=\"head\">")
        if (!logoBase64.isNullOrEmpty()) sb.append("<img src=\"data:image/png;base64,$logoBase64\">")
        sb.append("<div><h1>${esc(office.officeName.ifBlank { "تقرير تقدير تكلفة" })}</h1>")
        val contact = listOf(office.engineer, office.phone, office.email).filter { it.isNotBlank() }
        if (contact.isNotEmpty()) sb.append("<div class=\"muted\">${esc(contact.joinToString(" — "))}</div>")
        sb.append("</div></div>")

        sb.append("<h2>بيانات المشروع</h2><table>")
        fun kv(k: String, v: String) = sb.append("<tr><th style=\"width:35%\">$k</th><td>${esc(v)}</td></tr>")
        kv("المشروع", p.name)
        if (p.client.isNotBlank()) kv("العميل", p.client)
        kv("الدولة والعملة", "$countryName — $currency")
        if (fxNote.isNotBlank()) kv("سعر الصرف", fxNote)
        kv("التاريخ", date)
        kv("مساحة الأرض", "${Fmt.qty(p.landArea)} م²")
        kv("مسطح الدور × عدد الأدوار", "${Fmt.qty(p.footprint)} م² × ${p.floors}")
        kv("إجمالي المسطح المبني", "${Fmt.qty(e.built)} م²")
        kv("نوع السقف / الأساسات", "${p.slab.ar} / ${p.foundation.ar}")
        kv("درجة التشطيب", p.finish.ar)
        sb.append("</table>")

        if (e.missingPrices.isNotEmpty()) {
            sb.append("<div class=\"warn\">أسعار غير مدخلة (محسوبة بصفر): ${esc(e.missingPrices.joinToString("، "))}</div>")
        }

        sb.append("<h2>ملخص التكلفة</h2><table><tr><th>البند</th><th>القيمة ($currency)</th><th>ملاحظة</th></tr>")
        for (c in e.costs) sb.append("<tr><td>${esc(c.label)}</td><td class=\"n\">${Fmt.money(c.amount)}</td><td class=\"note\">${esc(c.note)}</td></tr>")
        sb.append("<tr class=\"t\"><td>تكلفة التنفيذ</td><td class=\"n\">${Fmt.money(e.execution)}</td><td>${Fmt.money(e.costPerM2)} $currency/م²</td></tr>")
        if (e.landCost > 0 || e.otherCost > 0) {
            sb.append("<tr><td>الأرض</td><td class=\"n\">${Fmt.money(e.landCost)}</td><td></td></tr>")
            sb.append("<tr><td>مصاريف أخرى</td><td class=\"n\">${Fmt.money(e.otherCost)}</td><td class=\"note\">${Fmt.qty(p.otherPct)}% من التنفيذ</td></tr>")
            sb.append("<tr class=\"t\"><td>إجمالي تكلفة المشروع</td><td class=\"n\">${Fmt.money(e.total)}</td><td></td></tr>")
        }
        sb.append("</table>")

        if (e.revenue > 0) {
            sb.append("<h2>البيع والربح</h2><table>")
            sb.append("<tr><td>المسطح القابل للبيع</td><td class=\"n\">${Fmt.qty(e.sellable)} م²</td></tr>")
            sb.append("<tr><td>إيراد البيع</td><td class=\"n\">${Fmt.money(e.revenue)}</td></tr>")
            sb.append("<tr class=\"t\"><td>صافي الربح</td><td class=\"n\">${Fmt.money(e.profit)}</td></tr>")
            sb.append("<tr><td>العائد على التكلفة</td><td class=\"n\">${Fmt.qty(e.roiPct)}%</td></tr></table>")
        }

        fun section(title: String, rows: List<Q>) {
            sb.append("<h2>$title</h2><table><tr><th>البند</th><th>الكمية</th><th>الوحدة</th><th>ملاحظة</th></tr>")
            for (q in rows) sb.append("<tr><td>${esc(q.label)}</td><td class=\"n\">${Fmt.qty(q.qty)}</td><td>${esc(q.unit)}</td><td class=\"note\">${esc(q.note)}</td></tr>")
            sb.append("</table>")
        }
        section("كميات الهيكل الخرساني", e.structure)
        section("حديد التسليح", e.steel)
        section("الأساسات", e.foundation)
        section("الحفر والردم", e.earth)
        section("المباني", e.masonry)
        section("التشطيبات", e.finishes)
        if (e.insulation.isNotEmpty()) section("العزل", e.insulation)
        section("إجمالي الخامات", e.materials)

        sb.append("<h2>الجدول الزمني (${e.durationDays} يوم ≈ ${Fmt.qty(e.durationDays / 30.0)} شهر)</h2>")
        sb.append("<table><tr><th>المرحلة</th><th>البداية (يوم)</th><th>المدة (يوم)</th><th>التكلفة ($currency)</th></tr>")
        for (ph in e.phases) sb.append("<tr><td>${esc(ph.name)}</td><td class=\"n\">${ph.start + 1}</td><td class=\"n\">${ph.days}</td><td class=\"n\">${Fmt.money(ph.cost)}</td></tr>")
        sb.append("</table>")

        sb.append("<h2>التدفق النقدي الشهري</h2><table><tr><th>الشهر</th><th>الصرف ($currency)</th><th>التراكمي ($currency)</th></tr>")
        var cum = 0.0
        e.months.forEachIndexed { i, m ->
            cum += m
            sb.append("<tr><td>${i + 1}</td><td class=\"n\">${Fmt.money(m)}</td><td class=\"n\">${Fmt.money(cum)}</td></tr>")
        }
        sb.append("</table>")

        sb.append("<div class=\"foot\">هذا تقدير أولي مبني على معدلات وأسعار استرشادية، وليس حصر كميات من المخططات ولا عرض سعر ملزماً. تتغير القيم حسب التصميم الإنشائي وأسعار السوق وقت التنفيذ.</div>")
        sb.append("</body></html>")
        return sb.toString()
    }

    fun csv(p: Project, e: Estimate, currency: String): String {
        val sb = StringBuilder("﻿")
        fun row(vararg cells: String) {
            sb.append(cells.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" }).append("\r\n")
        }
        row("المشروع", p.name)
        row("المسطح المبني (م²)", Fmt.qty(e.built).replace(",", ""))
        row("")
        row("القسم", "البند", "القيمة", "الوحدة", "ملاحظة")
        for (c in e.costs) row("التكلفة", c.label, String.format(Locale.US, "%.0f", c.amount), currency, c.note)
        row("التكلفة", "تكلفة التنفيذ", String.format(Locale.US, "%.0f", e.execution), currency, "")
        row("التكلفة", "الأرض", String.format(Locale.US, "%.0f", e.landCost), currency, "")
        row("التكلفة", "مصاريف أخرى", String.format(Locale.US, "%.0f", e.otherCost), currency, "")
        row("التكلفة", "إجمالي تكلفة المشروع", String.format(Locale.US, "%.0f", e.total), currency, "")
        fun sec(name: String, rows: List<Q>) {
            for (q in rows) row(name, q.label, String.format(Locale.US, "%.2f", q.qty), q.unit, q.note)
        }
        sec("الهيكل", e.structure)
        sec("الحديد", e.steel)
        sec("الأساسات", e.foundation)
        sec("الحفر والردم", e.earth)
        sec("المباني", e.masonry)
        sec("التشطيبات", e.finishes)
        if (e.insulation.isNotEmpty()) sec("العزل", e.insulation)
        sec("الخامات", e.materials)
        for (ph in e.phases) row("الجدول الزمني", ph.name, ph.days.toString(), "يوم", "يبدأ يوم ${ph.start + 1}")
        e.months.forEachIndexed { i, m -> row("التدفق النقدي", "الشهر ${i + 1}", String.format(Locale.US, "%.0f", m), currency, "") }
        return sb.toString()
    }
}
