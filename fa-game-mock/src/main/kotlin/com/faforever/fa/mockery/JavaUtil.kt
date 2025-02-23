package com.faforever.fa.mockery

object JavaUtil {
    val CLASSPATH_SEPARATOR: String = System.getProperty("path.separator")
    val CLASS_PATH_LIST: List<String> by lazy {
        val classPath = System.getProperty("java.class.path")
        listOf(
            *classPath.split(CLASSPATH_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        )
    }
}
