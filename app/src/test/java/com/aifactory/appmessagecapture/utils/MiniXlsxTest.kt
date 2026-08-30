package com.aifactory.appmessagecapture.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

class MiniXlsxTest {

    @Test
    fun `写读回环 - 文本数字转义与稀疏单元格`() {
        val sheet = MiniSheet(
            name = "测试表",
            rows = listOf(
                listOf("日期时间", "标题", "金额", "备注"),
                listOf("2026-08-30 22:31:05", "京东支付", 30.38, "含,逗号与\"引号\"&<标签>"),
                listOf<Any?>(null, null, null, "只有最后一列"),
                listOf("8500", "纯文本数字看做字符串", 8500.0, null)
            )
        )
        val bytes = MiniXlsx.write(listOf(sheet, MiniSheet("第二表", listOf(listOf<Any?>("A", 1)))))
        val sheets = MiniXlsx.read(bytes)

        assertEquals(2, sheets.size)
        assertEquals("测试表", sheets[0].name)
        assertEquals("第二表", sheets[1].name)

        val rows = sheets[0].rows
        assertEquals("日期时间", rows[0][0])
        assertEquals("标题", rows[0][1])
        assertEquals("30.38", rows[1][2])
        assertEquals("含,逗号与\"引号\"&<标签>", rows[1][3])
        assertNull(rows[2][0])
        assertNull(rows[2][2])
        assertEquals("只有最后一列", rows[2][3])
        assertEquals("8500", rows[3][2])
    }

    @Test
    fun `读 - 兼容Excel重存结构 sharedStrings与富文本`() {
        // 手工构造一份「Excel 风格」的 xlsx：字符串集中在 sharedStrings，
        // 单元格用 t="s" 引用，并包含富文本 <r><t> 合并片段
        val sharedStrings = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="6">
<si><t>日期时间</t></si>
<si><t>标题</t></si>
<si><r><t>富文本</t></r><r><t>拼接</t></r></si>
</sst>"""
        val sheet = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>
<row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
<row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2"><v>42.5</v></c></row>
</sheetData></worksheet>"""
        val bytes = buildZip(
            mapOf(
                "[Content_Types].xml" to "<Types/>",
                "xl/workbook.xml" to """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="支出账单" sheetId="1" r:id="rId1"/></sheets></workbook>""",
                "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
                "xl/sharedStrings.xml" to sharedStrings,
                "xl/worksheets/sheet1.xml" to sheet
            )
        )

        val sheets = MiniXlsx.read(bytes)
        assertEquals(1, sheets.size)
        assertEquals("支出账单", sheets[0].name)
        val rows = sheets[0].rows
        assertEquals("日期时间", rows[0][0])
        assertEquals("标题", rows[0][1])
        assertEquals("富文本拼接", rows[1][0])
        assertEquals("42.5", rows[1][1])
    }

    @Test
    fun `读 - 缺少workbook时抛出中文异常`() {
        val bytes = buildZip(mapOf("other.txt" to "hi"))
        val ex = assertThrows(IllegalArgumentException::class.java) { MiniXlsx.read(bytes) }
        assert(ex.message!!.contains("workbook.xml"))
    }

    @Test
    fun `列号与列名互转`() {
        assertEquals("A", MiniXlsx.colLetters(0))
        assertEquals("Z", MiniXlsx.colLetters(25))
        assertEquals("AA", MiniXlsx.colLetters(26))
        assertEquals(2, MiniXlsx.colIndexFromRef("C5"))
        assertEquals(26, MiniXlsx.colIndexFromRef("AA1"))
    }

    @Test
    fun `数字格式化 - 去尾零且无科学计数法`() {
        assertEquals("30.38", MiniXlsx.numberText(30.38))
        assertEquals("8500", MiniXlsx.numberText(8500.0))
        assertEquals("0.000001", MiniXlsx.numberText(0.000001))
    }

    private fun buildZip(entries: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
