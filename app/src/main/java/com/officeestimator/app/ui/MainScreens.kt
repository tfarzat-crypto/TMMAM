package com.officeestimator.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.officeestimator.app.data.Countries
import com.officeestimator.app.data.FinishLevel
import com.officeestimator.app.data.FoundationType
import com.officeestimator.app.data.Project
import com.officeestimator.app.data.SlabType
import com.officeestimator.app.data.WallUnit
import com.officeestimator.app.report.Fmt

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = content
    )
}

private data class Tile(val icon: String, val label: String, val screen: Screen)

// ======================= الرئيسية =======================

@Composable
fun HomeScreen(app: AppState) {
    val p = app.project
    val e = app.estimate
    val cur = app.currency
    ScreenColumn {
        // بطاقة الحسبة السريعة
        Card(
            modifier = Modifier.fillMaxWidth().clickable { app.screen = Screen.COST },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "حسبة سريعة — ${Fmt.qty(e.built)} م² مبني (${p.floors} دور) — ${app.country.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "إجمالي تكلفة المشروع · $cur",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    Fmt.short(e.total), fontSize = 38.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "تنفيذ ${Fmt.short(e.execution)} + الأرض ${Fmt.short(e.landCost)} + مصاريف أخرى ${Fmt.short(e.otherCost)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "تشطيب: ${p.finish.ar} · ${Fmt.money(e.costPerM2)} $cur/م²",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Fmt.qty(e.rc), "الخرسانة · م³", Modifier.weight(1f))
                    StatBox(Fmt.qty(e.steelTon), "الحديد · طن", Modifier.weight(1f))
                    StatBox("${e.durationDays}", "المدة · يوم", Modifier.weight(1f))
                }
            }
        }

        if (e.missingPrices.isNotEmpty()) {
            WarningBox("توجد ${e.missingPrices.size} أسعار غير مدخلة لدولة ${app.country.name} وتُحسب بصفر. أدخلها من تبويب «الأسعار».")
        }

        SectionCard("اختياراتك الأساسية", "هذه التي تغيّر السعر") {
            NumField("مسطح الدور", p.footprint, "م²") { v -> app.update { it.copy(footprint = v) } }
            NumField("عدد الأدوار", p.floors.toDouble(), "دور") { v ->
                app.update { it.copy(floors = v.toInt().coerceIn(0, 60)) }
            }
            ChoiceRow("درجة التشطيب", FinishLevel.values().toList(), p.finish, { it.ar }) { v ->
                app.update { it.copy(finish = v) }
            }
            SwitchRow("حديقة (لاندسكيب) تُحسب في التكلفة", p.includeLandscape) { v ->
                app.update { it.copy(includeLandscape = v) }
            }
            if (p.includeLandscape) {
                NumField("مساحة اللاندسكيب", p.landscapeArea, "م²") { v -> app.update { it.copy(landscapeArea = v) } }
            }
        }

        TileGroup(
            app, "المشروع", listOf(
                Tile("📋", "بيانات المشروع", Screen.PROJECT_DATA),
                Tile("🏠", "تكلفة المبنى", Screen.COST),
                Tile("💰", "الأرض والربح والبيع", Screen.LAND)
            )
        )
        TileGroup(
            app, "الهيكل والأساسات", listOf(
                Tile("🏗️", "الهيكل الخرساني", Screen.STRUCTURE),
                Tile("⛏️", "الأساسات والحفر", Screen.FOUNDATION)
            )
        )
        TileGroup(
            app, "التشطيب والعزل", listOf(
                Tile("🧱", "المباني والتشطيبات", Screen.MASONRY),
                Tile("💧", "العزل", Screen.INSULATION)
            )
        )
        TileGroup(
            app, "الجدول والواجهة والتقرير", listOf(
                Tile("📅", "الجدول الزمني والتدفق", Screen.SCHEDULE),
                Tile("🏢", "واجهة المبنى", Screen.FACADE),
                Tile("📄", "التقرير", Screen.REPORT)
            )
        )
        TileGroup(
            app, "الخامات", listOf(
                Tile("📦", "إجمالي الخامات", Screen.MATERIALS),
                Tile("📐", "المعدلات", Screen.RATES)
            )
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TileGroup(app: AppState, title: String, tiles: List<Tile>) {
    GroupTitle(title)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0 until 3) {
            val t = tiles.getOrNull(i)
            if (t == null) {
                Spacer(Modifier.weight(1f))
            } else {
                Card(
                    modifier = Modifier.weight(1f).height(118.dp).clickable { app.screen = t.screen },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text(t.icon, fontSize = 22.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t.label, style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center, maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

// ======================= المشاريع =======================

@Composable
fun ProjectsScreen(app: AppState) {
    var toDelete by remember { mutableStateOf<Project?>(null) }
    ScreenColumn {
        Button(onClick = { app.addProject(); app.screen = Screen.PROJECT_DATA }, modifier = Modifier.fillMaxWidth()) {
            Text("+ مشروع جديد")
        }
        app.projects.forEach { p ->
            val current = p.id == app.currentId
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { app.select(p.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (current) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val sub = buildString {
                        if (p.client.isNotBlank()) append("العميل: ${p.client} · ")
                        append("${Countries.byCode(p.countryCode).name} · ${Fmt.qty(p.footprint)} م² × ${p.floors} أدوار")
                    }
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { app.select(p.id); app.screen = Screen.HOME }) {
                            Text(if (current) "المشروع الحالي" else "فتح")
                        }
                        TextButton(onClick = { app.duplicate(p) }) { Text("نسخ") }
                        if (app.projects.size > 1) {
                            TextButton(onClick = { toDelete = p }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
    val target = toDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("حذف المشروع") },
            text = { Text("سيُحذف «${target.name}» نهائياً ولا يمكن استرجاعه.") },
            confirmButton = {
                TextButton(onClick = { app.delete(target.id); toDelete = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("إلغاء") } }
        )
    }
}

// ======================= بيانات المشروع =======================

@Composable
fun ProjectDataScreen(app: AppState) {
    val p = app.project
    var pickCountry by remember { mutableStateOf(false) }
    ScreenColumn {
        SectionCard("التعريف") {
            TextInput("اسم المشروع", p.name) { v -> app.update { it.copy(name = v) } }
            TextInput("اسم العميل", p.client) { v -> app.update { it.copy(client = v) } }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الدولة والعملة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${app.country.name} — ${app.currency}", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { pickCountry = true }) { Text("تغيير") }
            }
        }
        SectionCard("المساحات والأدوار") {
            NumField("مساحة الأرض", p.landArea, "م²") { v -> app.update { it.copy(landArea = v) } }
            NumField("مسطح الدور", p.footprint, "م²") { v -> app.update { it.copy(footprint = v) } }
            NumField("عدد الأدوار", p.floors.toDouble(), "دور") { v ->
                app.update { it.copy(floors = v.toInt().coerceIn(0, 60)) }
            }
            NumField("ملحق السطح (نسبة من مسطح الدور)", p.annexPct, "%") { v -> app.update { it.copy(annexPct = v) } }
            NumField("ارتفاع الدور", p.floorHeight, "م") { v -> app.update { it.copy(floorHeight = v) } }
            NumField("محيط المبنى (صفر = تقدير تلقائي)", p.perimeter, "م") { v -> app.update { it.copy(perimeter = v) } }
            NumField("عرض الواجهة الرئيسية", p.facadeWidth, "م") { v -> app.update { it.copy(facadeWidth = v) } }
        }
        SectionCard("النظام الإنشائي") {
            ChoiceRow("نوع السقف", SlabType.values().toList(), p.slab, { it.ar }) { v -> app.update { it.copy(slab = v) } }
            ChoiceRow("نوع الأساسات", FoundationType.values().toList(), p.foundation, { it.ar }) { v ->
                app.update { it.copy(foundation = v) }
            }
            NumField("عمق الحفر", p.excDepth, "م") { v -> app.update { it.copy(excDepth = v) } }
            NumField("سمك تربة الإحلال", p.replThickness, "م") { v -> app.update { it.copy(replThickness = v) } }
        }
        SectionCard("المباني والتشطيب") {
            ChoiceRow("نوع الطوب", WallUnit.values().toList(), p.wallUnit, { it.ar }) { v -> app.update { it.copy(wallUnit = v) } }
            ChoiceRow("درجة التشطيب", FinishLevel.values().toList(), p.finish, { it.ar }) { v -> app.update { it.copy(finish = v) } }
            SwitchRow("تبليط السطح", p.roofTiling) { v -> app.update { it.copy(roofTiling = v) } }
            SwitchRow("لاندسكيب يُحسب في التكلفة", p.includeLandscape) { v -> app.update { it.copy(includeLandscape = v) } }
            if (p.includeLandscape) {
                NumField("مساحة اللاندسكيب", p.landscapeArea, "م²") { v -> app.update { it.copy(landscapeArea = v) } }
            }
            NumField("احتياطي ونثريات", p.contingencyPct, "%") { v -> app.update { it.copy(contingencyPct = v) } }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (pickCountry) {
        AlertDialog(
            onDismissRequest = { pickCountry = false },
            title = { Text("اختر الدولة") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Countries.all.forEach { c ->
                        Text(
                            if (c.currency.isBlank()) c.name else "${c.name} — ${c.currency}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    app.update { it.copy(countryCode = c.code) }
                                    pickCountry = false
                                }
                                .padding(vertical = 12.dp),
                            fontWeight = if (c.code == p.countryCode) FontWeight.Bold else FontWeight.Normal
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickCountry = false }) { Text("إغلاق") } }
        )
    }
}

// ======================= تكلفة المبنى =======================

@Composable
fun CostScreen(app: AppState) {
    val e = app.estimate
    val cur = app.currency
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(Fmt.qty(e.rc), "الخرسانة · م³", Modifier.weight(1f))
            StatBox(Fmt.qty(e.steelTon), "الحديد · طن", Modifier.weight(1f))
            StatBox(Fmt.money(e.costPerM2), "$cur/م²", Modifier.weight(1f))
        }
        if (e.missingPrices.isNotEmpty()) {
            WarningBox("أسعار غير مدخلة وتُحسب بصفر: ${e.missingPrices.joinToString("، ")}")
        }
        SectionCard("التكلفة التقريبية", "القيم بالـ$cur") {
            e.costs.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ValueRow(c.label, Fmt.money(c.amount), cur, c.note)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ValueRow("تكلفة التنفيذ", Fmt.money(e.execution), cur, strong = true)
            ValueRow("تكلفة المتر المربع", Fmt.money(e.costPerM2), "$cur/م²", "تكلفة التنفيذ ÷ ${Fmt.qty(e.built)} م² مبني")
        }
        SectionCard("تحقق بطرق ثانية", "للمقارنة فقط") { QuantityList(e.quick) }
        Text(
            "تقدير أولي مبني على معدلات وأسعار استرشادية قابلة للتعديل، وليس حصراً من المخططات.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// ======================= الأرض والربح =======================

@Composable
fun LandScreen(app: AppState) {
    val p = app.project
    val e = app.estimate
    val cur = app.currency
    ScreenColumn {
        SectionCard("الأرض والمصاريف") {
            NumField("مساحة الأرض", p.landArea, "م²") { v -> app.update { it.copy(landArea = v) } }
            NumField("سعر متر الأرض", p.landPricePerM2, cur) { v -> app.update { it.copy(landPricePerM2 = v) } }
            NumField("مصاريف أخرى (تصميم وتراخيص وإشراف)", p.otherPct, "% من التنفيذ") { v ->
                app.update { it.copy(otherPct = v) }
            }
        }
        SectionCard("البيع") {
            NumField("نسبة المسطح القابل للبيع", p.sellPct, "%") { v -> app.update { it.copy(sellPct = v) } }
            NumField("سعر بيع المتر", p.salePricePerM2, cur) { v -> app.update { it.copy(salePricePerM2 = v) } }
        }
        SectionCard("النتيجة") {
            ValueRow("تكلفة التنفيذ", Fmt.money(e.execution), cur)
            ValueRow("تكلفة الأرض", Fmt.money(e.landCost), cur)
            ValueRow("مصاريف أخرى", Fmt.money(e.otherCost), cur)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ValueRow("إجمالي تكلفة المشروع", Fmt.money(e.total), cur, strong = true)
            ValueRow("المسطح القابل للبيع", Fmt.qty(e.sellable), "م²")
            ValueRow("إيراد البيع", Fmt.money(e.revenue), cur)
            ValueRow("صافي الربح", Fmt.money(e.profit), cur, strong = true)
            ValueRow("العائد على التكلفة", Fmt.qty(e.roiPct), "%")
            if (e.sellable > 0) {
                ValueRow("سعر التعادل للمتر", Fmt.money(e.total / e.sellable), cur, "أقل سعر بيع يغطي التكلفة")
            }
        }
    }
}
