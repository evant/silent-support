package me.tatarka.silentsupport

import me.tatarka.silentsupport.lint.ApiLookup
import org.objectweb.asm.*
import java.io.*
import java.util.logging.Logger
import java.util.zip.ZipInputStream

class SilentSupportProcessor(
        classpath: Collection<File>,
        supportMetaDataFile: File,
        private val apiLevel: Int,
        cacheDir: File?) {

    private val logger: Logger = Logger.getLogger("SilentSupportProcessor")

    private val lintClient = ProcessorLintClient(logger, cacheDir)

    private val sdkApiLookup: ApiLookup by lazy {
        ApiLookup.create(lintClient)!!
    }

    private val supportApiLookup: ApiLookup by lazy {
        ApiLookup.create(lintClient, supportMetaDataFile, true)
    }

    private val superMap: Map<String, String> by lazy {
        val map = HashMap<String, String>()

        for (classPathEntry in classpath) {
            if (classPathEntry.extension == "jar") {
                ZipInputStream(FileInputStream(classPathEntry)).use {
                    it.entries().forEach { entry ->
                        val name = entry.name
                        if (name.endsWith(".class")) {
                            val bytes = it.readBytes()
                            readSuperClass(bytes, map)
                        }
                    }
                }
            } else if (classPathEntry.isDirectory) {
                for (entry in classPathEntry.walk()) {
                    if (entry.extension == "class") {
                        val bytes = entry.readBytes()
                        readSuperClass(bytes, map)
                    }
                }
            }
        }
        map
    }

    private fun readSuperClass(bytes: ByteArray, map: MutableMap<String, String>) {
        val visitor = object : ClassVisitor(Opcodes.ASM5) {
            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                if (superName != null) {
                    map.put(name, superName)
                }
            }
        }
        val classReader = ClassReader(bytes)
        classReader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    }


    fun process(classFile: File): ByteArray {
        val output = ByteArrayOutputStream()
        classFile.inputStream().use { stream ->
            process(stream, apiLevel, output)
        }
        return output.toByteArray()
    }

    fun process(classFile: InputStream, apiLevel: Int, output: OutputStream) {
        val classReader = ClassReader(classFile)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)

        val visitor = object : ClassVisitor(Opcodes.ASM5, classWriter) {
            var className: String? = null
            var superName: String? = null

            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                className = name
                this.superName = superName
                super.visit(version, access, name, signature, superName, interfaces)
            }

            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                return object : MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
                    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                        var currentOwner: String? = owner

                        // No need to check methods in this local class; we know they
                        // won't be an API match
                        if (opcode == Opcodes.INVOKEVIRTUAL && owner == className) {
                            currentOwner = superName
                        }

                        while (currentOwner != null) {
                            if (currentOwner.startsWith("android")) {
                                val version = sdkApiLookup.getCallVersion(currentOwner, name, desc)
                                if (version > apiLevel) {
                                    val compatClassName = compatClassName(currentOwner)
                                    val newDesc = compatMethodDesc(currentOwner, desc)
                                    if (supportApiLookup.getCallVersion(compatClassName, name, newDesc) >= 0) {
                                        println("SilentSupport $owner.$name$desc -> $compatClassName.$name$desc")
                                        super.visitMethodInsn(Opcodes.INVOKESTATIC, compatClassName, name, newDesc, false)
                                        return
                                    }
                                }
                            }

                            // For virtual dispatch, walk up the inheritance chain checking
                            // each inherited method
                            if (opcode == Opcodes.INVOKEVIRTUAL) {
                                currentOwner = getSuperClass(currentOwner)
                            } else {
                                currentOwner = null
                            }
                        }

                        super.visitMethodInsn(opcode, owner, name, desc, itf)
                    }

                    private fun getSuperClass(owner: String): String? = superMap[owner]
                }
            }

        }
        classReader.accept(visitor, 0)

        output.write(classWriter.toByteArray())
    }

    companion object {
        @JvmStatic
        fun compatClassName(owner: String): String =
                owner.replaceFirst("android/", "android/support/v4/") + "Compat"

        @JvmStatic
        fun compatMethodDesc(owner: String, desc: String): String =
                desc.replaceFirst("(", "(L$owner;")
    }
}

