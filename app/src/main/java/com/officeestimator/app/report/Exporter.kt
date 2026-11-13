package com.officeestimator.app.report

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.officeestimator.app.ui.AppState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {
    // يبقى مرجع الـ WebView حيّاً حتى تنتهي الطباعة
    private var printView: WebView? = null

    fun buildHtml(app: AppState): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return ReportBuilder.html(
            app.project, app.estimate, app.office,
            app.country.name, app.currency, date, logoBase64(app.office.logoPath)
        )
    }

    private fun logoBase64(path: String): String? {
        if (path.isBlank()) return null
        return try {
            val f = File(path)
            if (!f.exists()) null else Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    /** يحفظ الشعار المختار كملف PNG مصغّر داخل التطبيق ويعيد مساره. */
    fun saveLogo(context: Context, uri: Uri): String? {
        return try {
            val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return null
            val maxSide = maxOf(src.width, src.height)
            val scaled = if (maxSide > 512) {
                val k = 512f / maxSide
                Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true)
            } else src
            val out = File(context.filesDir, "logo.png")
            FileOutputStream(out).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            out.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** يفتح نافذة الطباعة — ومنها «حفظ كملف PDF». يجب أن يكون السياق Activity. */
    fun print(activityContext: Context, html: String, jobName: String) {
        val web = WebView(activityContext)
        printView = web
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = activityContext.getSystemService(Context.PRINT_SERVICE) as PrintManager
                pm.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            }
        }
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    fun shareFile(context: Context, fileName: String, content: String, mime: String) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "مشاركة التقرير"))
    }

    fun safeName(name: String): String =
        name.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").ifBlank { "report" }
}
