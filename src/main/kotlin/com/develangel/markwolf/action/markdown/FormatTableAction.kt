package com.develangel.markwolf.action.markdown

import com.develangel.markwolf.domain.OwlDocument
import com.develangel.markwolf.util.countExtended
import com.develangel.markwolf.util.widthFor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import kotlin.math.max

const val MIN_WIDTH = 5
const val MIN_DASHES = 3

fun trimBorder(line: String): String {
  var s = 0;
  var e = line.length;
  if (line[0] == '|') s++;
  if (e > s && line[e - 1] == '|') e--;
  return line.substring(s, e);
}

fun isBar(value: String): Boolean {
  var vs = value.trim(':')
  if (value.length < MIN_DASHES) return false
  return vs == "-".repeat(vs.length) || vs == "=".repeat(vs.length)
}

class Cell(content: String, private val real: Cell? = null, var col: Int = 1) {
  private val content = content.trim()
  private var reservedWidth: Int = widthFor(content)
  private var childrenWidth: Int = 0
  val isBar: Boolean = if (real != null) { false } else { isBar(content.trim()) }
  var width: Int
    get() {
      if (this.real != null || isBar) {
        return MIN_WIDTH
      }
      val sw = max(widthFor(content) +2, reservedWidth)
      return max(sw - this.childrenWidth, MIN_WIDTH)
    }
    set(value) {
      if (this.real != null) {
        this.real.childrenWidth += value
      }
      this.reservedWidth = value
    }

  private fun makeContent(): String {
    val width = reservedWidth + childrenWidth - 2 - countExtended(content)
    return " %-${width}s ".format(content)
  }

  private fun makeBar(): String {
    val prefix = if (content.startsWith(":")) ":" else "-"
    val suffix = if (content.endsWith(":")) ":" else "-"
    var vs = content.trim(':').substring(0, 1)
    return prefix + (vs.repeat(reservedWidth-2)) + suffix
  }

  val align: String?
    get() {
      if (isBar) {
        if (content.endsWith(":")) {
          if (content.startsWith(":")) {
            return "center"
          }
          return "right"
        }
      }
      return null
    }

  val text: String
    get() {
      return when {
        col != 1 -> ""
        isBar -> {
          this.makeBar()
        }
        real != null -> {
          real.makeContent()
        }
        else -> {
          this.makeContent()
        }
      }
    }

  fun getHTML(tag: String, align: String?): String {
    if (this.real != null) {
      return ""
    }
    val params = mutableListOf<String>()
    if (col>1) {
      params.add("colspan=\"${col}\"")
    }
    if (align!=null) {
      params.add("align=\"${align}\"")
    }
    val param = if (params.size>0) " ${params.joinToString(" ")}" else ""
    return "<${tag}${param}>${content}</${tag}>"
  }
}

fun lineToCells(line: String, trim: Boolean): List<Cell> {
  val words = trimBorder(line).split("|")
    .map { if (it.isEmpty()) null else if (trim) it.trim() else it }
  val cells = mutableListOf<Cell>()
  for ((idx, word) in words.withIndex()) {
    if (word == null && idx > 0) {
      val real = cells[idx - 1]
      val cell = Cell("", real, real.col)
      cells[idx-1] = cell;
      real.col += 1
      cells.add(real)
    } else {
      cells.add(Cell(word.orEmpty()))
    }
  }
  return cells.toList()
}

fun toCells(tableStr: String, trim: Boolean): List<List<Cell>> {
  return tableStr.split("\n").map { lineToCells(it, trim) }
}

fun inverseCells(rows: List<List<Cell>>): List<List<Cell>> {
  val colSize = rows.map { it.size }.max() ?: 0
  val columns = MutableList<MutableList<Cell>>(colSize) { mutableListOf<Cell>() }
  for (row in rows) {
    for ((colIndex, cell) in row.withIndex()) {
      columns[colIndex].add(cell)
    }
  }
  return columns
}

fun formatTable(tableStr: String, trim: Boolean): String {
  val rows = toCells(tableStr, trim)
  val columns = inverseCells(rows)
  for ( col in columns ) {
    val maxWidth = col.map { cell -> cell.width }.max() ?: MIN_WIDTH
    for ( v in col ) v.width = maxWidth
  }
  return rows.joinToString("\n") { row -> "|${row.joinToString("|") { it.text }}|" }
}

fun convertTable(tableStr: String): String {
  val rows = toCells(tableStr, true)
  var tag = "th"
  val aligns = rows.find { it.first().isBar }?.map { it.align }
  return "<table>\n" + rows.mapNotNull { cells ->
    if (cells.first().isBar) {
      tag = "td"
      null
    } else {
      "<tr>" + cells.mapIndexed { idx, cell ->
        cell.getHTML(tag, aligns?.get(idx))
      }.joinToString("") + "</tr>"
    }
  }.joinToString("\n") + "\n</table>"
}

fun getRangeAsTable(doc: OwlDocument): TextRange {
  var currentLine = doc.currentCaret.caretModel.logicalPosition.line
  val startTableLine = (currentLine downTo 0)
    .find { doc.getTextByLine(it).isEmpty() }?.plus(1) ?: 0
  val endTableLine = (currentLine..doc.lastLine)
    .find { doc.getTextByLine(it).isEmpty() }?.minus(1) ?: doc.lastLine

  return TextRange(doc.getLineStartOffset(startTableLine), doc.getLineEndOffset(endTableLine))
}

class FormatTableAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val doc = OwlDocument(e)

        val tableRange = getRangeAsTable(doc)
        doc.safeReplace(tableRange, formatTable(doc.getTextByRange(tableRange), false))
    }
}

class ReformatTableAction : AnAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val doc = OwlDocument(e)

    val tableRange = getRangeAsTable(doc)
    doc.safeReplace(tableRange, formatTable(doc.getTextByRange(tableRange), true))
  }
}

class ConvertTableAction : AnAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val doc = OwlDocument(e)

    val tableRange = getRangeAsTable(doc)
    doc.safeReplace(tableRange, convertTable(doc.getTextByRange(tableRange)))
  }
}
