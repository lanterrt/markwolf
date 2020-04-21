package com.develangel.markwolf.util

fun widthFor(codePoint: Int): Int {
  return Wcwidth.of(codePoint)
}

fun widthFor(str: String): Int {
  return str.codePoints().map { widthFor(it) }.sum()
}

fun countExtended(str: String): Int {
  return str.codePoints().map { widthFor(it) -1 }.sum()
}
