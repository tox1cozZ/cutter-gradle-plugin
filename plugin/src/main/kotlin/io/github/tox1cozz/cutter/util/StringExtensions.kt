package io.github.tox1cozz.cutter.util

@Suppress("NOTHING_TO_INLINE")
inline fun String.capitalized() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }