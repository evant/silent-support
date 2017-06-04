package me.tatarka.silentsupport

import com.android.tools.lint.client.api.JavaParser
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.client.api.UastParser
import com.android.tools.lint.client.api.XmlParser
import com.android.tools.lint.detector.api.*
import me.tatarka.assertk.assert
import me.tatarka.assertk.assertions.isGreaterThan
import me.tatarka.silentsupport.lint.ApiLookup
import org.junit.Before
import org.junit.Test
import java.io.File

class SupportMetadataProcessorTest {

    lateinit var processor: SupportMetadataProcessor
    lateinit var lookup: ApiLookup

    val outputDir = File("build/resources/test")

    @Before
    fun setup() {
        processor = SupportMetadataProcessor(
                SupportCompat(File("test-libs/support-compat-25.3.0.aar"), "25.3.0"),
                outputDir)
    }

    @Test
    fun `can find ContextCompat#getDrawable`() {
        val lintClient = object : LintClient() {

            override fun getUastParser(p0: Project): UastParser {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun report(context: Context, issue: Issue, severity: Severity?, location: Location?, message: String?, format: TextFormat?, fix: LintFix?) {
            }

            override fun log(severity: Severity?, exception: Throwable?, format: String?, vararg args: Any?) {
            }

            override fun getJavaParser(project: Project?): JavaParser {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun readFile(file: File?): CharSequence {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getXmlParser(): XmlParser {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getCacheDir(name: String, create: Boolean): File {
                return outputDir
            }
        }

        processor.process()
        lookup = ApiLookup.create(lintClient, processor.metadataFile, true)
        val result = lookup.getCallVersion("android/support/v4/content/ContextCompat", "getDrawable", "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;")

        assert(result).isGreaterThan(0)
    }
}


