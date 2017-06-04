package me.tatarka.silentsupport

import me.tatarka.assertk.assert
import me.tatarka.assertk.assertions.isGreaterThan
import me.tatarka.assertk.assertions.isLessThan
import me.tatarka.silentsupport.lint.ApiLookup
import org.junit.Before
import org.junit.Test
import java.io.File

class SupportApiLookupTest {

    lateinit var apiLookup: ApiLookup

    @Before
    fun setup() {
        val classpath = listOf(File("test-libs/android-25.jar"), File("test-libs/support-compat-25.3.0.aar"))
        val outputDir = File("build/resources/test")
        val supportCompat = SupportCompat.fromClasspath(classpath)!!
        val supportMetadataProcessor = SupportMetadataProcessor(supportCompat, outputDir)
        supportMetadataProcessor.process()
        apiLookup = ApiLookup.create(ProcessorLintClient(cacheDir = outputDir), supportMetadataProcessor.metadataFile, true)
    }

    @Test
    fun `finds ContextCompat#getDrawable`() {
        val result = apiLookup.getCallVersion("android/support/v4/content/ContextCompat", "getDrawable", "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;")

        assert(result).isGreaterThan(0)
    }

    @Test
    fun `finds ActivityCompat@getDrawable`() {
        val result = apiLookup.getCallVersion("android/support/v4/app/ActivityCompat", "getDrawable", "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;")

        assert(result).isGreaterThan(0)
    }

    @Test
    fun `finds ActivityCompat@getDrawable ignoring receiver`() {
        val result = apiLookup.getCallVersion("android/support/v4/app/ActivityCompat", "getDrawable", "(Landroid/app/Activity;I)Landroid/graphics/drawable/Drawable;", true)

        assert(result).isGreaterThan(0)
    }

    @Test
    fun `does not find non-support method ActivityCompat#createConfigurationContext`() {
        val result = apiLookup.getCallVersion("android/support/v4/app/ActivityCompat", "createConfigurationContext", "(Landroid/app/Activity;Landroid/content/res/Configuration;)Landroid/content/Context;", true)

        assert(result).isLessThan(0)
    }

    @Test
    fun `finds ConnectivityManagerCompat#isActiveNetworkMetered`() {
        val result = apiLookup.getCallVersion("android/support/v4/net/ConnectivityManagerCompat", "isActiveNetworkMetered", "(Landroid/net/ConnectivityManager;)Z")

        assert(result).isGreaterThan(0)
    }
}

