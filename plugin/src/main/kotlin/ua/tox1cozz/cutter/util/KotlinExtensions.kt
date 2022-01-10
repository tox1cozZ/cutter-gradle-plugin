package ua.tox1cozz.cutter.util

object KotlinExtensions {

//    inline fun <T> MutableIterable<T>.removeFirstAndGetOrNull(predicate: (T) -> Boolean): T? {
//        val each = iterator()
//        while (each.hasNext()) {
//            val value = each.next()
//            if (predicate(value)) {
//                each.remove()
//                return value
//            }
//        }
//        return null
//    }
//
//    inline fun <T> MutableIterable<T>.removeFirstAndGet(predicate: (T) -> Boolean): T {
//        return removeFirstAndGetOrNull(predicate) ?: throw NoSuchElementException("Iterable contains no element matching the predicate.")
//    }

    fun String.replaceLast(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
        val index = lastIndexOf(oldValue, ignoreCase = ignoreCase)
        return if (index < 0) this else replaceRange(index, index + oldValue.length, newValue)
    }
}