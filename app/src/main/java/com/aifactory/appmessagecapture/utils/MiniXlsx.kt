package com.aifactory.appmessagecapture.utils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 单元格值约定（写方向）：
 * - String  → 文本单元格（inlineStr）
 * - Double / Long / Int → 数字单元格（可参与 Excel 求和/透视）
 * - null → 空单元格
 * 读方向统一返回文本（数字即其字面量），由调用方自行解析。
 */
typealias MiniRow = List<Any?>

data class MiniSheet(val name: String, val rows: List<MiniRow>)

/**
 * 轻量 xlsx（OOXML SpreadsheetML）读写器，无第三方依赖。
 *
 * 写出：内联字符串 + 数字单元格 + 最小 styles.xml，Excel/WPS 均可直接打开；
 * 读取：兼容 Excel/WPS 重存后的标准结构（sharedStrings、富文本 <r><t> 合并、
 * 单元格稀疏排布、命名空间前缀差异）。日期一律按文本处理，不做序列号转换。
 */
object MiniXlsx {

    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_DOC_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    // ------------------------------------------------------------------
    // 写
    // ------------------------------------------------------------------

    fun write(sheets: List<MiniSheet>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putEntryAndWrite("[Content_Types].xml", contentTypesXml(sheets.size))
            zip.putEntryAndWrite("_rels/.rels", ROOT_RELS)
            zip.putEntryAndWrite("xl/workbook.xml", workbookXml(sheets))
            zip.putEntryAndWrite("xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            zip.putEntryAndWrite("xl/styles.xml", STYLES_XML)
            sheets.forEachIndexed { i, sheet ->
                zip.putEntryAndWrite("xl/worksheets/sheet${i + 1}.xml", sheetXml(sheet))
            }
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.putEntryAndWrite(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypesXml(count: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..count) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="$NS_DOC_REL/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private fun workbookXml(sheets: List<MiniSheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="$NS_MAIN" xmlns:r="$NS_DOC_REL"><sheets>""")
        sheets.forEachIndexed { i, sheet ->
            val name = xmlEscape(sheet.name).take(31) // Excel 表名上限 31 字符
            append("""<sheet name="$name" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRelsXml(count: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..count) {
            append("""<Relationship Id="rId$i" Type="$NS_DOC_REL/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${count + 1}" Type="$NS_DOC_REL/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="$NS_MAIN"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs></styleSheet>"""

    private fun sheetXml(sheet: MiniSheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="$NS_MAIN"><sheetData>""")
        sheet.rows.forEachIndexed { r, row ->
            append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, cell ->
                val ref = "${colLetters(c)}${r + 1}"
                when (cell) {
                    null -> {}
                    is Number -> append("""<c r="$ref"><v>${numberText(cell.toDouble())}</v></c>""")
                    else -> append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${xmlEscape(cell.toString())}</t></is></c>""")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** 数字去尾零且无科学计数法：30.38→"30.38"，8500.0→"8500"（valueOf 走最短字符串表示，避免二进制精度尾巴） */
    internal fun numberText(value: Double): String =
        java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    internal fun xmlEscape(s: String): String {
        val sb = StringBuilder(s.length)
        s.forEach { ch ->
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch == '\'' -> sb.append("&apos;")
                ch == '\t' || ch == '\n' || ch == '\r' -> sb.append(ch)
                ch.code < 0x20 -> {} // 丢弃 XML 非法控制字符
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    internal fun colLetters(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + i % 26))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    // ------------------------------------------------------------------
    // 读
    // ------------------------------------------------------------------

    /** 解析工作簿；结构缺失时抛 [IllegalArgumentException]，带中文原因。 */
    fun read(bytes: ByteArray): List<MiniSheet> = try {
        readInternal(bytes)
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        throw IllegalArgumentException("无法解析工作簿文件（不是有效的 xlsx？）", e)
    }

    private fun readInternal(bytes: ByteArray): List<MiniSheet> {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val parser = newParser()

        // workbook.xml：有序 (表名, rId)
        val workbookBytes = entries["xl/workbook.xml"]
            ?: throw IllegalArgumentException("缺少 xl/workbook.xml")
        val sheetRefs = ArrayList<Pair<String, String>>() // name to rid
        parser.setInput(ByteArrayInputStream(workbookBytes), null)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = attr(parser, "name") ?: continue
                val rid = attr(parser, "id") ?: continue
                sheetRefs.add(name to rid)
            }
        }

        // rels：rId → 文件路径
        val targets = HashMap<String, String>()
        entries["xl/_rels/workbook.xml.rels"]?.let { rels ->
            parser.setInput(ByteArrayInputStream(rels), null)
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                    val id = attr(parser, "Id") ?: continue
                    val target = attr(parser, "Target") ?: continue
                    targets[id] = target
                }
            }
        }

        // sharedStrings（Excel/WPS 重存后字符串集中于此）
        val shared = ArrayList<String>()
        entries["xl/sharedStrings.xml"]?.let { sst ->
            parser.setInput(ByteArrayInputStream(sst), null)
            val sb = StringBuilder()
            var inSi = false
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> if (parser.name == "si") { inSi = true; sb.clear() }
                    XmlPullParser.TEXT -> if (inSi) sb.append(parser.text)
                    XmlPullParser.END_TAG -> if (parser.name == "si") { shared.add(sb.toString()); inSi = false }
                }
            }
        }

        return sheetRefs.map { (name, rid) ->
            val target = targets[rid]
                ?: throw IllegalArgumentException("工作表「$name」缺少关系定义")
            val path = when {
                target.startsWith("/xl/") -> target.removePrefix("/")
                target.startsWith("xl/") -> target
                else -> "xl/$target"
            }
            val sheetBytes = entries[path]
                ?: throw IllegalArgumentException("缺少工作表文件 $path")
            MiniSheet(name, parseSheet(sheetBytes, shared, parser))
        }
    }

    /** 逐行解析 sheetData；单元格稀疏时按 r 属性定位，空位补 null。 */
    private fun parseSheet(bytes: ByteArray, shared: List<String>, parser: XmlPullParser): List<List<String?>> {
        parser.setInput(ByteArrayInputStream(bytes), null)
        val rows = ArrayList<List<String?>>()
        var cells: MutableList<String?>? = null
        var cellType: String? = null
        var vText: StringBuilder? = null
        var inlineText: StringBuilder? = null
        var cellCol = 0
        var colCursor = 0

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        cells = ArrayList()
                        colCursor = 0
                    }
                    "c" -> {
                        cellType = attr(parser, "t")
                        val ref = attr(parser, "r")
                        cellCol = if (ref != null) colIndexFromRef(ref) else colCursor
                        vText = null
                        inlineText = null
                    }
                    "is" -> inlineText = StringBuilder()
                    "v" -> if (vText == null) vText = StringBuilder()
                }
                XmlPullParser.TEXT -> {
                    if (vText != null) vText.append(parser.text)
                    if (inlineText != null) inlineText.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val value = when (cellType) {
                            "s" -> vText?.toString()?.trim()?.toIntOrNull()?.let { shared.getOrNull(it) }
                            "inlineStr" -> inlineText?.toString()
                            else -> vText?.toString()
                        }
                        if (cells != null) {
                            while (cells.size <= cellCol) cells.add(null)
                            cells[cellCol] = value
                        }
                        colCursor = cellCol + 1
                        cellType = null
                        vText = null
                        inlineText = null
                    }
                    "row" -> {
                        cells?.let {
                            val rowNum = rows.size + 1
                            while (rows.size < rowNum - 1) rows.add(emptyList())
                            rows.add(it)
                        }
                        cells = null
                    }
                }
            }
        }
        return rows
    }

    private fun newParser(): XmlPullParser =
        XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()

    /** 按本地名取属性（忽略前缀差异：r:id / id 都能取到） */
    private fun attr(parser: XmlPullParser, localName: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i) == localName) return parser.getAttributeValue(i)
        }
        return null
    }

    /** "C5" → 2（0 基列号） */
    internal fun colIndexFromRef(ref: String): Int {
        var index = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') index = index * 26 + (ch - 'A' + 1)
            else if (ch in 'a'..'z') index = index * 26 + (ch - 'a' + 1)
            else break
        }
        return index - 1
    }
}
