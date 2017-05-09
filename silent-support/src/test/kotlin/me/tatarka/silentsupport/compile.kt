package me.tatarka.silentsupport

import org.junit.Assert.fail
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.*
import javax.tools.*

internal class InMemoryJavaFileObject @Throws(Exception::class)
constructor(className: String, private val contents: String) : SimpleJavaFileObject(
        URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
        JavaFileObject.Kind.SOURCE) {

    @Throws(IOException::class)
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = contents
}

/**
 * Compiles the given string of java source, allowing you to call methods on it and assert their
 * results.
 */
fun compile(className: String, code: String): GeneratedCode {
    // Create an in-memory Java file object
    val javaFileObject = InMemoryJavaFileObject(className, code)

    val compiler = ToolProvider.getSystemJavaCompiler()

    val fileManager = compiler.getStandardFileManager(null,
            null,
            null)

    val path = File("build/classes/test")
    val files = listOf(path)
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, files)

    val diagnostics = DiagnosticCollector<JavaFileObject>()

    val task = compiler.getTask(null,
            fileManager,
            diagnostics,
            null,
            null,
            listOf(javaFileObject))

    val success = task.call()!!

    fileManager.close()

    // If there' a compilation error, display error messages and fail the test
    if (!success) {
        val msg = StringBuilder()
        for (diagnostic in diagnostics.diagnostics) {
            msg.append("Code: ${diagnostic.code}\n")
            msg.append("Kind: ${diagnostic.kind}\n")
            msg.append("Position: ${diagnostic.position}\n")
            msg.append("Start Position: ${diagnostic.startPosition}\n")
            msg.append("End Position: ${diagnostic.endPosition}\n")
            msg.append("Source: ${diagnostic.source}\n")
            msg.append("Message: ${diagnostic.getMessage(Locale.getDefault())}\n\n")
        }

        msg.append(diagnostics.diagnostics[0].source.getCharContent(true))

        fail(msg.toString())
    } else {
        println(javaFileObject.getCharContent(true))
    }
    return GeneratedCode(className, path)
}

class GeneratedCode(val className: String, val path: File) {
    fun classFile(): File = File(path, className + ".class")

    fun replaceWith(content: ByteArray) {
        classFile().writeBytes(content)
    }

    fun load(): Class<*> = Class.forName(className)
}
