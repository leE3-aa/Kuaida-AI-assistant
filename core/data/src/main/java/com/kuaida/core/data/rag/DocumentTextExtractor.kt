package com.kuaida.core.data.rag

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.text.HtmlCompat
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun extract(uri: Uri, mimeType: String): ExtractedDocument {
        val contentResolver = context.contentResolver
        val name = queryName(uri)
        val size = querySize(uri)
        val text = when {
            mimeType.contains("pdf", ignoreCase = true) || name.endsWith(".pdf", true) -> extractPdf(uri)
            mimeType.contains("wordprocessingml", ignoreCase = true) || name.endsWith(".docx", true) -> extractDocx(uri)
            else -> extractPlainText(uri)
        }.trim()
        require(text.isNotBlank()) { "文件解析后为空，请检查内容格式。" }
        return ExtractedDocument(name = name, mimeType = mimeType.ifBlank { "text/plain" }, sizeBytes = size, text = text)
    }

    private fun extractPlainText(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun extractPdf(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: ByteArray(0)
        return PDDocument.load(bytes).use { document ->
            PDFTextStripper().getText(document)
        }
    }

    private fun extractDocx(uri: Uri): String {
        val output = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }
                    .firstOrNull { it.name == "word/document.xml" }
                    ?.let {
                        zip.copyTo(output)
                    }
            }
        }
        val xml = output.toString()
        return HtmlCompat.fromHtml(
            xml.replace("</w:p>", "\n").replace(Regex("<[^>]+>"), ""),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        ).toString()
    }

    private fun queryName(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else "未命名文件"
        } ?: "未命名文件"
    }

    private fun querySize(uri: Uri): Long {
        val projection = arrayOf(OpenableColumns.SIZE)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
        } ?: 0L
    }
}

data class ExtractedDocument(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val text: String,
)
