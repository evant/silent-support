package me.tatarka.silentsupport

import com.android.tools.lint.client.api.JavaParser
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.client.api.UastParser
import com.android.tools.lint.client.api.XmlParser
import com.android.tools.lint.detector.api.*
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class ProcessorLintClient(
        private val logger: Logger? = null,
        private val cacheDir: File? = null) : LintClient() {

    override fun log(severity: Severity, exception: Throwable?, format: String, vararg args: Any?) {
        logger?.log(Level.ALL, String.format(format, args))
    }

    override fun getJavaParser(project: Project?): JavaParser = throw NotImplementedError()

    override fun readFile(file: File?): CharSequence = throw NotImplementedError()

    override fun getXmlParser(): XmlParser = throw NotImplementedError()

    override fun report(context: Context, issue: Issue, severity: Severity?, location: Location?, message: String?, format: TextFormat?, args: Any?) {
        throw NotImplementedError()
    }

    override fun getCacheDir(create: Boolean): File {
        return cacheDir ?: super.getCacheDir(create)
    }

    override fun getUastParser(project: Project): UastParser {
        throw NotImplementedError()
    }
}

