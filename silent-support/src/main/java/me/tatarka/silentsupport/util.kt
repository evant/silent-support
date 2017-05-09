package me.tatarka.silentsupport

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

internal fun ZipInputStream.entries(): Iterator<ZipEntry> {
    return object : Iterator<ZipEntry> {
        var entry: ZipEntry? = this@entries.nextEntry
        var read = false

        override fun hasNext(): Boolean {
            if (read) {
                entry = nextEntry
                read = false
            }
            return entry != null
        }

        override fun next(): ZipEntry {
            read = true
            return entry!!
        }
    }
}
