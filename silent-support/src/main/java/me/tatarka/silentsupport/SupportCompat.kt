package me.tatarka.silentsupport

import java.io.File

data class SupportCompat(val file: File, val version: String) {
    companion object {
        fun fromClasspath(classpath: Collection<File>): SupportCompat? {
            val file = classpath.find { it.name.matches(Regex("support-compat-[0-9.]+\\.aar")) } ?: return null
            val version = Regex(".*-([0-9.]+)\\.aar").find(file.name)!!.groupValues[1]
            return SupportCompat(file, version)
        }
    }
}
