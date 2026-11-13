package com.officeestimator.app.ui

import android.graphics.BitmapFactory
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.officeestimator.app.data.Countries
import com.officeestimator.app.data.FoundationType
import com.officeestimator.app.data.Prices
import com.officeestimator.app.data.Rates
import com.officeestimator.app.data.SlabType
import com.officeestimator.app.data.WallUnit
import com.officeestimator.app.report.Exporter
import com.officeestimator.app.report.Fmt
import com.officeestimator.app.report.ReportBuilder
import kotlin.math.max
import kotlin.math.min

// ======================= الهيكل الخرساني =======================

@Composable
fun StructureScreen(app: AppState) {
    val p = app.project
    val e = app.estimate
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(Fmt.qty(e.rc), "خرسانة مسلحة · م³", Modifier.weight(1f))
            StatBox(Fmt.qty(e.steelTon), "حديد · طن", Modifier.weight(1f))
            StatBox(Fmt.qty(e.pc), "خرسانة عادية · م³", Modifier.weight(1f))
        }
        SectionCard("الاختيارات") {
            ChoiceRow("نوع السقف", SlabType.values().toList(), p.slab, { it.ar }) { v -> app.update { it.copy(slab = v) } }
            ChoiceRow("نوع الأساسات", FoundationType.values().toList(), p.foundation, { it.ar }) { v ->
                app.update { it.copy(foundation = v) }
            }
        }
        SectionCard("كميات الخرسانة", "محسوبة عنصراً بعنصر من المعدلات") { QuantityList(e.structure) }
        SectionCard("حديد التسليح") { QuantityList(e.steel) }
        SectionCard("تحقق بطرق ثانية", "للمقارنة فقط") { QuantityList(e.quick) }
    }
}

// ======================= الأساسات والحفر =======================

@Composable
fun FoundationScreen(app: AppState) {
    val p = app.project
    val e = app.estimate
    ScreenColumn {
        SectionCard("الاختيارات") {
            ChoiceRow("نوع الأساسات", FoundationType.values().toList(), p.foundation, { it.ar }) { v ->
                app.update { it.copy(foundation = v) }
            }
            NumField("عمق الحفر", p.excDepth, "م") { v -> app.update { it.copy(excDepth = v) } }
            NumField("سمك تربة الإحلال", p.replThickness, "م") { v -> app.update { it.copy(replThickness = v) } }
        }
        SectionCard("الأساسات") { QuantityList(e.foundation) }
        SectionCard("الحفر والردم والإحلال") { QuantityList(e.earth) }
        Text(
            "نوع الأساسات وعمق التأسيس يحددهما تقرير التربة والمصمم الإنشائي؛ الاختيار هنا لأغراض التقدير فقط.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// ======================= المباني والتشطيبات =======================

@Composable
fun MasonryScreen(app: AppState) {
    val p = app.project
    val e = app.estimate
    val units = e.masonry.firstOrNull { it.label.startsWith("عدد") }
    val tiles = e.finishes.filter { it.label.startsWith("بلاط") }.sumOf { it.qty }
    val cladding = e.finishes.firstOrNull { it.label.startsWith("تكسية") }?.qty ?: 0.0
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(Fmt.qty(units?.qty ?: 0.0), "${units?.unit ?: "بلوك"}", Modifier.weight(1f))
            StatBox(Fmt.qty(cladding), "الواجهات · م²", Modifier.weight(1f))
            StatBox(Fmt.qty(tiles), "البلاط · م²", Modifier.weight(1f))
        }
        SectionCard("الاختيارات") {
            ChoiceRow("نوع الطوب", WallUnit.values().toList(), p.wallUnit, { it.ar }) { v -> app.update { it.copy(wallUnit = v) } }
            SwitchRow("تبليط السطح", p.roofTiling) { v -> app.update { it.copy(roofTiling = v) } }
        }
        SectionCard("المباني", "حساب سريع بمسطح الدور والمحيط") { QuantityList(e.masonry) }
        SectionCard("التشطيبات") { QuantityList(e.finishes) }
    }
}

// ======================= العزل =======================

@Composable
fun InsulationScreen(app: AppState) {
    val e = app.estimate
    ScreenColumn {
        SectionCard("كميات العزل") { QuantityList(e.insulation) }
        Text(
            "تُسعَّر بنود العزل من تبويب «الأسعار» وتدخل ضمن تكلفة المبنى.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// ======================= الجدول الزمني =======================

@Composable
fun ScheduleScreen(app: AppState) {
    val e = app.estimate
    val cur = app.currency
    val total = max(1, e.durationDays)
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("${e.durationDays}", "المدة · يوم", Modifier.weight(1f))
            StatBox(Fmt.qty(e.durationDays / 30.0), "شهر", Modifier.weight(1f))
            StatBox(Fmt.short(e.execution), cur, Modifier.weight(1f))
        }
        SectionCard("مراحل التنفيذ", "المراحل متداخلة — الشريط يبدأ من اليمين") {
            e.phases.forEach { ph ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row {
                        Text(ph.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${ph.days} يوم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val before = ph.start.toFloat() / total
                        val length = ph.days.toFloat() / total
                        val after = 1f - before - length
                        if (before > 0.001f) Spacer(Modifier.weight(before))
                        Box(
                            Modifier
                                .weight(max(length, 0.01f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        if (after > 0.001f) Spacer(Modifier.weight(after))
                    }
                    Text(
                        "من اليوم ${ph.start + 1} إلى ${ph.end} · ${Fmt.money(ph.cost)} $cur",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        SectionCard("التدفق النقدي الشهري", "الصرف المتوقع بالـ$cur") {
            val peak = max(1.0, e.months.maxOrNull() ?: 1.0)
            var cum = 0.0
            e.months.forEachIndexed { i, m ->
                cum += m
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row {
                        Text("الشهر ${i + 1}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(Fmt.money(m), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth().height(8.dp)) {
                        val f = (m / peak).toFloat().coerceIn(0.01f, 1f)
                        Box(
                            Modifier
                                .weight(f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        if (f < 0.999f) Spacer(Modifier.weight(1f - f))
                    }
                    Text(
                        "التراكمي ${Fmt.money(cum)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            "المدد من معدلات قابلة للتعديل في «المعدلات ← مدة التنفيذ»، والصرف موزّع بالتساوي على أيام كل مرحلة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// ======================= واجهة المبنى =======================

@Composable
fun FacadeScreen(app: AppState) {
    val p = app.project
    val e = app.estimate
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(Fmt.qty(p.facadeWidth), "عرض الواجهة · م", Modifier.weight(1f))
            StatBox(Fmt.qty(e.height), "الارتفاع · م", Modifier.weight(1f))
            StatBox("${p.floors}", "دور", Modifier.weight(1f))
        }
        SectionCard("واجهة المبنى", "شكل تقريبي للتوضيح فقط — ليس رسماً تنفيذياً") {
            val floors = p.floors.coerceIn(1, 30)
            val realW = max(4.0, p.facadeWidth).toFloat()
            val floorH = max(2.5, p.floorHeight).toFloat()
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                val w = size.width
                val h = size.height
                val groundY = h * 0.86f
                drawRect(Color(0xFFCFE6F5))
                drawRect(Color(0xFF8DB580), topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
                val realH = floors * floorH + 1f
                val scale = min(w * 0.78f / realW, (groundY - h * 0.06f) / realH)
                val bw = realW * scale
                val fh = floorH * scale
                val bh = floors * fh
                val left = (w - bw) / 2f
                val top = groundY - bh
                // الجسم والدروة
                drawRect(Color(0xFFD9C7A8), topLeft = Offset(left, top), size = Size(bw, bh))
                drawRect(Color(0xFFF1E8D6), topLeft = Offset(left - 4f, top - scale), size = Size(bw + 8f, scale))
                val bays = max(2, (realW / 3.5f).toInt())
                val bayW = bw / bays
                val doorBay = bays / 2
                for (f in 0 until floors) {
                    val y = top + f * fh
                    drawRect(Color(0xFFF1E8D6), topLeft = Offset(left, y), size = Size(bw, max(2f, fh * 0.05f)))
                    val ground = f == floors - 1
                    for (b in 0 until bays) {
                        val x = left + b * bayW
                        if (ground && b == doorBay) {
                            drawRect(
                                Color(0xFF5B4632),
                                topLeft = Offset(x + bayW * 0.3f, y + fh * 0.25f),
                                size = Size(bayW * 0.4f, fh * 0.75f)
                            )
                        } else {
                            drawRect(
                                Color(0xFF7FB2D6),
                                topLeft = Offset(x + bayW * 0.25f, y + fh * 0.28f),
                                size = Size(bayW * 0.5f, fh * 0.42f)
                            )
                            drawLine(
                                Color.White,
                                Offset(x + bayW * 0.5f, y + fh * 0.28f),
                                Offset(x + bayW * 0.5f, y + fh * 0.70f),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
                // خطوط الأبعاد
                drawLine(Color(0xFF16302D), Offset(left, groundY + h * 0.07f), Offset(left + bw, groundY + h * 0.07f), strokeWidth = 2f)
                drawLine(Color(0xFF16302D), Offset(left - w * 0.05f, top), Offset(left - w * 0.05f, groundY), strokeWidth = 2f)
            }
            Spacer(Modifier.height(8.dp))
            ValueRow("عرض الواجهة (الخط الأفقي)", Fmt.qty(p.facadeWidth), "م")
            ValueRow("الارتفاع بدون الدروة (الخط الرأسي)", Fmt.qty(e.height), "م")
        }
        SectionCard("تعديل") {
            NumField("عرض الواجهة الرئيسية", p.facadeWidth, "م") { v -> app.update { it.copy(facadeWidth = v) } }
            NumField("ارتفاع الدور", p.floorHeight, "م") { v -> app.update { it.copy(floorHeight = v) } }
            NumField("عدد الأدوار", p.floors.toDouble(), "دور") { v ->
                app.update { it.copy(floors = v.toInt().coerceIn(0, 60)) }
            }
        }
    }
}

// ======================= إجمالي الخامات =======================

@Composable
fun MaterialsScreen(app: AppState) {
    val e = app.estimate
    ScreenColumn {
        SectionCard("إجمالي الخامات", "كميات الخلطة تفيد عند الصب بالموقع؛ مع الخرسانة الجاهزة هي للمعلومية") {
            QuantityList(e.materials)
        }
    }
}

// ======================= المعدلات =======================

@Composable
fun RatesScreen(app: AppState) {
    ScreenColumn {
        Text(
            "هذه معدلات تقديرية شائعة كنقطة بداية. عدّلها لتطابق خبرة مكتبك ومشاريعك المنفذة، وتُطبَّق على كل المشاريع.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp)
        )
        Rates.defs.groupBy { it.group }.forEach { (group, defs) ->
            SectionCard(group) {
                defs.forEach { d ->
                    key(d.key) {
                        NumField(d.label, app.rates[d.key] ?: d.default, d.unit) { v -> app.setRate(d.key, v) }
                    }
                }
            }
        }
        OutlinedButton(onClick = { app.resetRates() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("استرجاع المعدلات الافتراضية")
        }
    }
}

// ======================= الأسعار =======================

@Composable
fun PricesScreen(app: AppState) {
    val code = app.project.countryCode
    val cur = app.inputCurrency
    val prices = app.countryPrices
    val allZero = prices.values.all { it <= 0.0 }
    ScreenColumn {
        Text(
            "أسعار ${app.country.name} بالـ$cur",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(4.dp)
        )
        Text(
            "الدولة تتغير من «بيانات المشروع». الأسعار المعلَّمة «افتراضي» أرقام بداية غير موثّقة — راجعها على أسعار السوق قبل إخراج أي تقرير لعميل.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp)
        )
        if (code == Countries.SYRIA) {
            WarningBox("أسعار سوريا تُدخل بالدولار. أرقام البداية محوَّلة من أسعار السعودية (÷ 3.75) وليست أسعار السوق السوري — راجعها قبل إخراج أي تقرير.")
            if (app.sypActive) {
                Text(
                    "النتائج والتقرير بالليرة السورية بسعر صرف ${Fmt.money(app.office.sypRate)} ل.س للدولار (يُعدَّل من «بيانات المشروع»).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        if (!Prices.hasDefaults(code)) {
            WarningBox("لا توجد أسعار بداية لهذه الدولة. أدخل أسعارها يدوياً، أو انسخ أسعار دولة أخرى ثم عدّلها.")
            if (allZero) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (code == Countries.USA) {
                        // أمريكا بالدولار: النسخ من أرقام سوريا المحوَّلة للدولار لا من أرقام بالريال أو الجنيه
                        OutlinedButton(onClick = { app.resetPrices(Countries.SYRIA) }) { Text("نسخ أرقام سوريا (بالدولار)") }
                    } else {
                        OutlinedButton(onClick = { app.resetPrices("SA") }) { Text("نسخ أرقام السعودية") }
                        OutlinedButton(onClick = { app.resetPrices("EG") }) { Text("نسخ أرقام مصر") }
                    }
                }
            }
        }
        Prices.defs.groupBy { it.group }.forEach { (group, defs) ->
            SectionCard(group) {
                defs.forEach { d ->
                    key(code + d.key) {
                        NumField("${d.label} / ${d.unit}", prices[d.key] ?: 0.0, cur) { v -> app.setPrice(d.key, v) }
                        Text(
                            if (code in d.refs) "مرجعي — من مصدر منشور وقت إعداد التطبيق"
                            else "افتراضي — يحتاج مراجعة",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (code in d.refs) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
        if (Prices.hasDefaults(code)) {
            OutlinedButton(onClick = { app.resetPrices() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("استرجاع أسعار البداية")
            }
        }
    }
}

// ======================= بيانات المكتب =======================

@Composable
fun SettingsScreen(app: AppState) {
    val ctx = LocalContext.current
    val o = app.office
    var logoVersion by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = Exporter.saveLogo(ctx, uri)
            if (path != null) {
                app.changeOffice(app.office.copy(logoPath = path))
                logoVersion++
            }
        }
    }
    ScreenColumn {
        SectionCard("هوية المكتب", "تظهر في رأس التقرير وعلى الصفحة الرئيسية") {
            ValueRow("اسم المكتب", o.officeName)
            ValueRow("المهندس", o.engineer)
            TextInput("الهاتف", o.phone) { v -> app.changeOffice(o.copy(phone = v)) }
            TextInput("البريد الإلكتروني", o.email) { v -> app.changeOffice(o.copy(email = v)) }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val bitmap = remember(o.logoPath, logoVersion) {
                    if (o.logoPath.isBlank()) null else BitmapFactory.decodeFile(o.logoPath)?.asImageBitmap()
                }
                if (bitmap != null) {
                    Image(bitmap, contentDescription = "شعار المكتب", modifier = Modifier.size(64.dp))
                    Spacer(Modifier.size(12.dp))
                }
                Button(onClick = { picker.launch("image/*") }) {
                    Text(if (bitmap == null) "اختيار شعار المكتب" else "تغيير الشعار")
                }
                if (bitmap != null) {
                    TextButton(onClick = { app.changeOffice(o.copy(logoPath = "")) }) { Text("إزالة") }
                }
            }
        }
        SectionCard("سعر صرف الليرة السورية", "يُستخدم لمشاريع سوريا عند اختيار العرض بالليرة") {
            NumField("سعر صرف الدولار", o.sypRate, "ل.س") { v -> app.setSypRate(v) }
        }
        SectionCard("العملة المخصصة", "تُستخدم عند اختيار «دولة أخرى» في بيانات المشروع") {
            TextInput("اسم العملة", o.customCurrency) { v -> app.changeOffice(o.copy(customCurrency = v)) }
        }
        SectionCard("المظهر") {
            SwitchRow("الوضع الليلي", app.dark) { app.toggleDark() }
        }
        SectionCard("أدوات") {
            OutlinedButton(onClick = { app.screen = Screen.RATES }, modifier = Modifier.fillMaxWidth()) { Text("المعدلات الهندسية") }
        }
        Text(
            "نتائج التطبيق تقديرات أولية مبنية على معدلات وأسعار استرشادية، ولا تغني عن حصر الكميات من المخططات المعتمدة وعروض الأسعار الفعلية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// ======================= التقرير =======================

@Composable
fun ReportScreen(app: AppState) {
    val ctx = LocalContext.current
    val html = Exporter.buildHtml(app)
    val name = Exporter.safeName(app.project.name)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { Exporter.print(ctx, html, name) }, modifier = Modifier.weight(1f)) { Text("طباعة / PDF") }
            OutlinedButton(
                onClick = { Exporter.shareFile(ctx, "$name.html", html, "text/html") },
                modifier = Modifier.weight(1f)
            ) { Text("مشاركة") }
            OutlinedButton(
                onClick = {
                    Exporter.shareFile(ctx, "$name.csv", ReportBuilder.csv(app.project, app.estimate, app.currency), "text/csv")
                },
                modifier = Modifier.weight(1f)
            ) { Text("Excel") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AndroidView(
            factory = { WebView(it) },
            update = { web ->
                if (web.tag != html) {
                    web.tag = html
                    web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
