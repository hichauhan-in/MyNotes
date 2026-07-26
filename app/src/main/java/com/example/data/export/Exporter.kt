package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Base64
import com.example.data.attachments.AttachmentStore
import com.example.domain.model.Checklist
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.domain.model.NoteType
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A file format a note or book can be exported to. */
enum class ExportFormat(val label: String, val ext: String, val mime: String) {
    TXT("Plain text", "txt", "text/plain"),
    MD("Markdown", "md", "text/markdown"),
    HTML("Web page", "html", "text/html"),
    PDF("PDF document", "pdf", "application/pdf"),
}

/**
 * Converts decrypted [Note]s into shareable files. Notes only ever reach this object already
 * decrypted (as [Note] objects), so nothing here touches ciphertext or keys.
 */
object Exporter {

    private val IMG = Regex("""^!\[([^\]]*)]\(attachment://([^)\s?]+)(?:\?w=(\d+))?\)$""")
    private val TABLE = Regex("""^\[\[table:([A-Za-z0-9+/=]+)]]$""")
    private val CALLOUT = Regex("""^\[\[callout:([A-Za-z0-9+/=]+)]]$""")
    private val SCRIBBLE = Regex("""^\[\[scribble:([A-Za-z0-9+/=]+)]]$""")

    // ---- Public API -------------------------------------------------------------

    /** The bytes of [note] rendered in [format]. */
    fun noteBytes(note: Note, format: ExportFormat): ByteArray = when (format) {
        ExportFormat.TXT -> plainDoc(note).toByteArray(Charsets.UTF_8)
        ExportFormat.MD -> markdownDoc(note).toByteArray(Charsets.UTF_8)
        ExportFormat.HTML -> htmlDoc(note).toByteArray(Charsets.UTF_8)
        ExportFormat.PDF -> pdfBytes(note)
    }

    /** A filesystem-safe base file name (no extension) for [note]. */
    fun noteFileBase(note: Note): String = safe(note.title.ifBlank { "Untitled" })

    /** A filesystem-safe fragment for a file/folder name. */
    fun safe(name: String): String =
        name.trim()
            .replace(Regex("""[\\/:*?"<>|\r\n\t]+"""), "_")
            .trim('_', '.', ' ')
            .ifBlank { "Untitled" }
            .take(80)

    /**
     * Streams a single note into [out] as a ZIP: the note file in [format] plus every image /
     * voice attachment in an `attachments/` folder, so a note with media stays self-contained.
     */
    fun writeNoteZip(context: Context, note: Note, format: ExportFormat, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            val base = noteFileBase(note)
            zip.putNextEntry(ZipEntry("$base.${format.ext}"))
            zip.write(noteBytes(note, format))
            zip.closeEntry()
            note.attachments.forEach { att ->
                runCatching {
                    val bytes = AttachmentStore.readDecrypted(context, att)
                    if (bytes != null) {
                        zip.putNextEntry(ZipEntry("attachments/${AttachmentStore.fileFor(context, att).name}"))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /**
     * Streams a book (the folder [rootId] and its whole subtree) into [out] as a ZIP: real
     * subfolders mirror the book hierarchy, each note is a file in [format], and every image /
     * voice attachment is copied into an `attachments/` folder beside its note.
     */
    fun writeBookZip(
        context: Context,
        rootId: String,
        folders: List<Folder>,
        notes: List<Note>,
        format: ExportFormat,
        out: OutputStream,
    ) {
        val active = folders.filter { !it.isTrashed }
        val childrenOf = active.groupBy { it.parentId }
        val notesByFolder = notes.filter { !it.isTrashed && !it.isArchived }.groupBy { it.folderId }
        val root = folders.firstOrNull { it.id == rootId } ?: return
        ZipOutputStream(out).use { zip ->
            fun put(path: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry()
            }
            fun walk(folderId: String, folderName: String, prefix: String) {
                val dirPath = prefix + safe(folderName) + "/"
                zip.putNextEntry(ZipEntry(dirPath)); zip.closeEntry()
                val used = HashSet<String>()
                notesByFolder[folderId].orEmpty().forEach { note ->
                    val base = noteFileBase(note)
                    var name = base
                    var i = 2
                    while (!used.add(name.lowercase())) { name = "$base ($i)"; i++ }
                    put("$dirPath$name.${format.ext}", noteBytes(note, format))
                    note.attachments.forEach { att ->
                        runCatching {
                            val bytes = AttachmentStore.readDecrypted(context, att)
                            if (bytes != null) put("${dirPath}attachments/${AttachmentStore.fileFor(context, att).name}", bytes)
                        }
                    }
                }
                childrenOf[folderId].orEmpty().sortedBy { it.name.lowercase() }.forEach { child ->
                    walk(child.id, child.name, dirPath)
                }
            }
            walk(root.id, root.name, "")
        }
    }

    // ---- Whole-document assembly ------------------------------------------------

    private fun plainDoc(note: Note): String = buildString {
        val t = note.title.ifBlank { "Untitled" }
        appendLine(t)
        appendLine("=".repeat(t.length.coerceIn(3, 50)))
        if (note.tags.isNotEmpty()) appendLine(note.tags.joinToString(" ") { "#$it" })
        appendLine()
        append(bodyText(note))
    }

    private fun markdownDoc(note: Note): String = buildString {
        appendLine("# " + note.title.ifBlank { "Untitled" })
        appendLine()
        if (note.tags.isNotEmpty()) {
            appendLine(note.tags.joinToString(" ") { "#$it" })
            appendLine()
        }
        append(bodyMd(note))
    }

    private fun htmlDoc(note: Note): String = buildString {
        val t = esc(note.title.ifBlank { "Untitled" })
        append("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">")
        append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
        append("<title>").append(t).append("</title><style>")
        append("body{font-family:system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;max-width:720px;margin:32px auto;padding:0 20px;line-height:1.6;color:#1b1b1f}")
        append("h1{margin:0 0 4px}.tags{color:#6b6b70;margin:0 0 20px}")
        append("img{max-width:100%;height:auto;border-radius:10px;margin:8px 0}")
        append("table{border-collapse:collapse;margin:10px 0}td,th{border:1px solid #d0d0d5;padding:6px 10px;text-align:left}")
        append("blockquote{border-left:3px solid #b0b0b8;margin:10px 0;padding:6px 14px;background:#f5f5f8;border-radius:0 8px 8px 0}")
        append("hr{border:none;border-top:1px solid #ddd;margin:18px 0}code{background:#f0f0f3;padding:1px 5px;border-radius:5px}")
        append("ul.tasks{list-style:none;padding-left:0}</style></head><body>")
        append("<h1>").append(t).append("</h1>")
        if (note.tags.isNotEmpty()) {
            append("<div class=\"tags\">").append(note.tags.joinToString(" ") { "#" + esc(it) }).append("</div>")
        }
        append(bodyHtml(note))
        append("</body></html>")
    }

    // ---- Body by note type ------------------------------------------------------

    private fun bodyText(note: Note): String = when (note.type) {
        NoteType.CHECKLIST -> Checklist.parse(note.content)
            .joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
        NoteType.SHEET -> tableToText(sheetCells(note.content))
        NoteType.EXPENSE -> expenseLines(note.content).joinToString("\n")
        NoteType.SCRIBBLE -> scribbleToText(note.content)
        else -> textNoteToPlain(note.content)
    }

    private fun bodyMd(note: Note): String = when (note.type) {
        NoteType.CHECKLIST -> Checklist.parse(note.content)
            .joinToString("\n") { "- [${if (it.checked) "x" else " "}] " + it.text }
        NoteType.SHEET -> tableToMd(sheetCells(note.content))
        NoteType.EXPENSE -> expenseMd(note.content)
        NoteType.SCRIBBLE -> scribbleToText(note.content)
        else -> textNoteToMd(note.content)
    }

    private fun bodyHtml(note: Note): String = when (note.type) {
        NoteType.CHECKLIST -> checklistHtml(note.content)
        NoteType.SHEET -> tableToHtml(sheetCells(note.content))
        NoteType.EXPENSE -> expenseHtml(note.content)
        NoteType.SCRIBBLE -> "<p>" + esc(scribbleToText(note.content)).replace("\n", "<br>") + "</p>"
        else -> textNoteToHtml(note.content)
    }

    // ---- Text notes -------------------------------------------------------------

    private fun textNoteToPlain(content: String): String = buildString {
        content.split("\n").forEach { raw ->
            val l = raw.trim()
            val img = IMG.matchEntire(l)
            val tbl = TABLE.matchEntire(l)
            val cal = CALLOUT.matchEntire(l)
            when {
                img != null -> appendLine(
                    if (img.groupValues[1] == "audio") "[Voice note: ${img.groupValues[2]}]"
                    else "[Image: ${img.groupValues[2]}]",
                )
                tbl != null -> appendLine(tableToText(decodeCells(tbl.groupValues[1])))
                cal != null -> { val (e, t) = decodeCallout(cal.groupValues[1]); appendLine("$e $t".trim()) }
                SCRIBBLE.matches(l) -> appendLine("[Sketch]")
                else -> appendLine(raw)
            }
        }
    }.trimEnd()

    private fun textNoteToMd(content: String): String = buildString {
        content.split("\n").forEach { raw ->
            val l = raw.trim()
            val img = IMG.matchEntire(l)
            val tbl = TABLE.matchEntire(l)
            val cal = CALLOUT.matchEntire(l)
            when {
                img != null -> {
                    val f = img.groupValues[2]
                    appendLine(
                        if (img.groupValues[1] == "audio") "[🔊 Voice note](attachments/$f)"
                        else "![image](attachments/$f)",
                    )
                }
                tbl != null -> { appendLine(tableToMd(decodeCells(tbl.groupValues[1]))) }
                cal != null -> { val (e, t) = decodeCallout(cal.groupValues[1]); appendLine("> $e $t".trim()) }
                SCRIBBLE.matches(l) -> appendLine("_[Sketch]_")
                else -> appendLine(raw)
            }
        }
    }.trimEnd()

    private fun textNoteToHtml(content: String): String {
        val sb = StringBuilder()
        val lines = content.split("\n")
        val para = StringBuilder()
        fun flush() {
            if (para.isNotBlank()) {
                val html = para.toString().trim().split("\n").joinToString("<br>") { inlineMd(it) }
                sb.append("<p>").append(html).append("</p>")
            }
            para.setLength(0)
        }
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val l = raw.trim()
            val img = IMG.matchEntire(l)
            val tbl = TABLE.matchEntire(l)
            val cal = CALLOUT.matchEntire(l)
            when {
                l.isEmpty() -> flush()
                img != null -> {
                    flush()
                    val f = esc(img.groupValues[2])
                    if (img.groupValues[1] == "audio") {
                        sb.append("<p>🔊 <a href=\"attachments/$f\">Voice note</a></p>")
                    } else {
                        sb.append("<img src=\"attachments/$f\" alt=\"image\">")
                    }
                }
                tbl != null -> { flush(); sb.append(tableToHtml(decodeCells(tbl.groupValues[1]))) }
                cal != null -> {
                    flush()
                    val (e, t) = decodeCallout(cal.groupValues[1])
                    sb.append("<blockquote>").append(esc(e)).append(" ").append(inlineMd(t)).append("</blockquote>")
                }
                SCRIBBLE.matches(l) -> { flush(); sb.append("<p><em>[Sketch]</em></p>") }
                l.startsWith("### ") -> { flush(); sb.append("<h3>").append(inlineMd(l.removePrefix("### "))).append("</h3>") }
                l.startsWith("## ") -> { flush(); sb.append("<h2>").append(inlineMd(l.removePrefix("## "))).append("</h2>") }
                l.startsWith("# ") -> { flush(); sb.append("<h2>").append(inlineMd(l.removePrefix("# "))).append("</h2>") }
                l.startsWith("> ") -> { flush(); sb.append("<blockquote>").append(inlineMd(l.removePrefix("> "))).append("</blockquote>") }
                Regex("^-{3,}$").matches(l) -> { flush(); sb.append("<hr>") }
                l.startsWith("- ") || l.startsWith("* ") -> {
                    flush()
                    sb.append("<ul>")
                    var j = i
                    while (j < lines.size) {
                        val lj = lines[j].trim()
                        if (lj.startsWith("- ") || lj.startsWith("* ")) {
                            sb.append("<li>").append(inlineMd(lj.substring(2))).append("</li>"); j++
                        } else break
                    }
                    sb.append("</ul>"); i = j - 1
                }
                Regex("""^\d+\. """).containsMatchIn(l) -> {
                    flush()
                    sb.append("<ol>")
                    var j = i
                    while (j < lines.size) {
                        val m = Regex("""^\d+\. (.*)$""").matchEntire(lines[j].trim())
                        if (m != null) { sb.append("<li>").append(inlineMd(m.groupValues[1])).append("</li>"); j++ } else break
                    }
                    sb.append("</ol>"); i = j - 1
                }
                else -> para.append(raw).append("\n")
            }
            i++
        }
        flush()
        return sb.toString()
    }

    private fun inlineMd(s: String): String {
        var r = esc(s)
        r = Regex("""\*\*(.+?)\*\*""").replace(r) { "<strong>${it.groupValues[1]}</strong>" }
        r = Regex("""(?<!\*)\*(?!\*)(.+?)\*""").replace(r) { "<em>${it.groupValues[1]}</em>" }
        r = Regex("""~~(.+?)~~""").replace(r) { "<del>${it.groupValues[1]}</del>" }
        r = Regex("""==(.+?)==""").replace(r) { "<mark>${it.groupValues[1]}</mark>" }
        r = Regex("""`([^`]+?)`""").replace(r) { "<code>${it.groupValues[1]}</code>" }
        return r
    }

    // ---- Checklist / sheet / expense / scribble ---------------------------------

    private fun checklistHtml(content: String): String {
        val items = Checklist.parse(content)
        if (items.isEmpty()) return ""
        return "<ul class=\"tasks\">" + items.joinToString("") {
            "<li>" + (if (it.checked) "☑ " else "☐ ") + esc(it.text) + "</li>"
        } + "</ul>"
    }

    private fun sheetCells(content: String): List<List<String>> = runCatching {
        val c = JSONObject(content).optJSONArray("c") ?: return emptyList()
        (0 until c.length()).map { r ->
            val row = c.getJSONArray(r)
            (0 until row.length()).map { row.optString(it) }
        }
    }.getOrDefault(emptyList())

    private fun decodeCells(b64: String): List<List<String>> {
        val c = decodeJson(b64)?.optJSONArray("c") ?: return emptyList()
        return (0 until c.length()).map { r ->
            val row = c.getJSONArray(r)
            (0 until row.length()).map { row.optString(it) }
        }
    }

    private fun tableToText(cells: List<List<String>>): String =
        cells.joinToString("\n") { row -> row.joinToString(" | ") }.ifBlank { "" }

    private fun tableToMd(cells: List<List<String>>): String {
        if (cells.isEmpty()) return ""
        val cols = cells.maxOf { it.size }.coerceAtLeast(1)
        fun row(r: List<String>) = (0 until cols).joinToString(" | ") { (r.getOrNull(it) ?: "").replace("|", "\\|").ifBlank { " " } }
        return buildString {
            appendLine("| ${row(cells.first())} |")
            appendLine("| ${(0 until cols).joinToString(" | ") { "---" }} |")
            cells.drop(1).forEach { appendLine("| ${row(it)} |") }
        }.trimEnd()
    }

    private fun tableToHtml(cells: List<List<String>>): String {
        if (cells.isEmpty()) return ""
        return buildString {
            append("<table>")
            cells.forEachIndexed { i, row ->
                append("<tr>")
                row.forEach { cell ->
                    val tag = if (i == 0) "th" else "td"
                    append("<$tag>").append(esc(cell)).append("</$tag>")
                }
                append("</tr>")
            }
            append("</table>")
        }
    }

    private fun expenseLines(content: String): List<String> {
        val o = decodeJsonRaw(content) ?: return listOf("(empty budget)")
        val out = mutableListOf<String>()
        out.add("Income: ₹${money(o.optDouble("income", 0.0))}")
        val secs = o.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until secs.length()) {
            val s = secs.getJSONObject(i)
            val items = s.optJSONArray("items") ?: JSONArray()
            var total = 0.0
            val itemLines = (0 until items.length()).map { j ->
                val it = items.getJSONObject(j)
                total += it.optDouble("amount", 0.0)
                "  - ${it.optString("name").ifBlank { "(unnamed)" }}: ₹${money(it.optDouble("amount", 0.0))}"
            }
            out.add("")
            out.add("${s.optString("name").ifBlank { "Section" }} — ₹${money(total)}")
            out.addAll(itemLines)
        }
        return out
    }

    private fun expenseMd(content: String): String {
        val o = decodeJsonRaw(content) ?: return "_(empty budget)_"
        return buildString {
            appendLine("**Income:** ₹${money(o.optDouble("income", 0.0))}")
            val secs = o.optJSONArray("sections") ?: JSONArray()
            for (i in 0 until secs.length()) {
                val s = secs.getJSONObject(i)
                val items = s.optJSONArray("items") ?: JSONArray()
                var total = 0.0
                val lines = (0 until items.length()).map { j ->
                    val it = items.getJSONObject(j)
                    total += it.optDouble("amount", 0.0)
                    "- ${it.optString("name").ifBlank { "(unnamed)" }}: ₹${money(it.optDouble("amount", 0.0))}"
                }
                appendLine()
                appendLine("### ${s.optString("name").ifBlank { "Section" }} (₹${money(total)})")
                lines.forEach { appendLine(it) }
            }
        }.trimEnd()
    }

    private fun expenseHtml(content: String): String {
        val o = decodeJsonRaw(content) ?: return "<p><em>(empty budget)</em></p>"
        return buildString {
            append("<p><strong>Income:</strong> ₹${money(o.optDouble("income", 0.0))}</p>")
            val secs = o.optJSONArray("sections") ?: JSONArray()
            for (i in 0 until secs.length()) {
                val s = secs.getJSONObject(i)
                val items = s.optJSONArray("items") ?: JSONArray()
                var total = 0.0
                val lis = (0 until items.length()).map { j ->
                    val it = items.getJSONObject(j)
                    total += it.optDouble("amount", 0.0)
                    "<li>${esc(it.optString("name").ifBlank { "(unnamed)" })}: ₹${money(it.optDouble("amount", 0.0))}</li>"
                }
                append("<h3>${esc(s.optString("name").ifBlank { "Section" })} (₹${money(total)})</h3>")
                if (lis.isNotEmpty()) append("<ul>").also { lis.forEach { append(it) } }.also { append("</ul>") }
            }
        }
    }

    private fun scribbleToText(content: String): String {
        val o = decodeJsonRaw(content) ?: return "[Whiteboard]"
        val texts = o.optJSONArray("t") ?: JSONArray()
        val notes = (0 until texts.length()).map { texts.getJSONObject(it).optString("t") }.filter { it.isNotBlank() }
        return buildString {
            append("[Whiteboard drawing]")
            if (notes.isNotEmpty()) {
                append("\n\nNotes on the board:\n")
                append(notes.joinToString("\n") { "- $it" })
            }
        }
    }

    // ---- PDF --------------------------------------------------------------------

    private fun pdfBytes(note: Note): ByteArray {
        val text = plainDoc(note)
        val pageW = 595
        val pageH = 842
        val margin = 42
        val lineH = 18f
        val maxW = (pageW - 2 * margin).toFloat()
        val paint = Paint().apply { textSize = 12f; color = 0xFF1B1B1F.toInt() }

        val wrapped = mutableListOf<String>()
        text.split("\n").forEach { p ->
            if (p.isEmpty()) wrapped.add("") else wrapLine(p, paint, maxW, wrapped)
        }

        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        var canvas = page.canvas
        var y = margin.toFloat()
        wrapped.forEach { line ->
            if (y + lineH > pageH - margin) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                canvas = page.canvas
                y = margin.toFloat()
            }
            if (line.isNotEmpty()) canvas.drawText(line, margin.toFloat(), y + lineH, paint)
            y += lineH
        }
        doc.finishPage(page)
        val bos = ByteArrayOutputStream()
        doc.writeTo(bos)
        doc.close()
        return bos.toByteArray()
    }

    private fun wrapLine(text: String, paint: Paint, maxW: Float, out: MutableList<String>) {
        var line = ""
        text.split(" ").forEach { w ->
            val trial = if (line.isEmpty()) w else "$line $w"
            if (line.isEmpty() || paint.measureText(trial) <= maxW) {
                line = trial
            } else {
                out.add(line); line = w
            }
        }
        out.add(line)
    }

    // ---- Small helpers ----------------------------------------------------------

    private fun decodeJson(b64: String): JSONObject? = runCatching {
        JSONObject(String(Base64.decode(b64, Base64.NO_WRAP), Charsets.UTF_8))
    }.getOrNull()

    private fun decodeJsonRaw(content: String): JSONObject? = runCatching { JSONObject(content) }.getOrNull()

    private fun decodeCallout(b64: String): Pair<String, String> {
        val o = decodeJson(b64) ?: return "💡" to ""
        return o.optString("e", "💡").ifBlank { "💡" } to o.optString("t")
    }

    private fun money(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.2f", v)

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;")
}

/** Small Storage-Access-Framework helpers for writing exports to a picked location or a saved folder. */
object ExportIO {

    /** Writes [bytes] to the document [uri] (from a create-document picker). Returns success. */
    fun writeBytes(context: Context, uri: Uri, bytes: ByteArray): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
    }.getOrDefault(false)

    /** Streams into the document [uri] via [block]. Returns success. */
    fun writeStream(context: Context, uri: Uri, block: (OutputStream) -> Unit): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { block(it); true } ?: false
    }.getOrDefault(false)

    /**
     * Creates a new document named [displayName] with [mime] inside the persisted tree [treeUriStr]
     * (from ACTION_OPEN_DOCUMENT_TREE). Returns its Uri, or null if the folder is unavailable.
     */
    fun createInTree(context: Context, treeUriStr: String, displayName: String, mime: String): Uri? = runCatching {
        val tree = Uri.parse(treeUriStr)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        DocumentsContract.createDocument(context.contentResolver, parent, mime, displayName)
    }.getOrNull()

    /** The public folder new exports land in by default (visible in the Files/Downloads app). */
    const val DOWNLOADS_SUBFOLDER = "MyNotes"

    /** True when we can silently write to the public Downloads folder without any permission. */
    val supportsDownloadsExport: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Writes [bytes] into Downloads/[DOWNLOADS_SUBFOLDER] via MediaStore. Needs no permission on
     * Android 10+ (scoped storage); duplicate names are auto-numbered by the system. Returns the
     * new item's Uri, or null on older versions / failure so the caller can fall back to a picker.
     */
    fun writeToDownloads(context: Context, displayName: String, mime: String, bytes: ByteArray): Uri? =
        writeToDownloads(context, displayName, mime) { it.write(bytes) }

    /** Streaming variant of [writeToDownloads]. */
    fun writeToDownloads(
        context: Context,
        displayName: String,
        mime: String,
        block: (OutputStream) -> Unit,
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + DOWNLOADS_SUBFOLDER
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        val ok = runCatching {
            resolver.openOutputStream(uri)?.use { block(it) } ?: return@runCatching false
            true
        }.getOrDefault(false)
        if (!ok) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
        runCatching {
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }
}
