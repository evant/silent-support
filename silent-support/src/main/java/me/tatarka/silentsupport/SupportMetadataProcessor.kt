package me.tatarka.silentsupport

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import java.util.zip.ZipInputStream

class SupportMetadataProcessor(private val supportCompat: SupportCompat, private val outputDir: File) {

    val metadataFile: File by lazy {
        File(outputDir, "support-api-versions-${supportCompat.version}.xml")
    }

    fun process() {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        metadataFile.writer().buffered().use { out ->
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<api version=\"2\">\n")
            ZipInputStream(supportCompat.file.inputStream()).use {
                it.entries().forEach { entry ->
                    val name = entry.name
                    if (name == "classes.jar") {
                        processClassesJar(it, out)
                    } else if (name.endsWith(".class")) {
                        readSupportClass(it.readBytes(), out)
                    }
                }
            }
            out.write("</api>")
        }
    }

    private fun processClassesJar(input: InputStream, out: Writer) {
        ZipInputStream(input).let {
            it.entries().forEach { entry ->
                if (entry.name.endsWith(".class")) {
                    readSupportClass(it.readBytes(), out)
                }
            }
        }
    }

    private fun readSupportClass(bytes: ByteArray, out: Writer) {
        val visitor = object : ClassVisitor(Opcodes.ASM5) {
            var inClass = false

            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                if (name.endsWith("Compat")) {
                    inClass = true
                    out.write("  <class name=\"$name\" since=\"2\">\n")
                    if (superName != null) {
                        out.write("    <extends name=\"$superName\" />\n")
                    }
                }
            }

            override fun visitEnd() {
                if (inClass) {
                    out.write("  </class>\n")
                }
                inClass = false
            }

            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                if (!inClass) {
                    return null
                }
                val acc = (Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC)
                if ((access and acc) == acc) {
                    out.write("    <method name=\"$name$desc\" />\n")
                }
                return null
            }

            private fun descWithoutReceiver(desc: String): String =
                    desc.replaceFirst("\\(L.*?;".toRegex(), "(")
        }
        val classReader = ClassReader(bytes)
        classReader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    }
}

