package me.tatarka.silentsupport

import me.tatarka.assertk.assert
import me.tatarka.assertk.assertions.isNotNull
import me.tatarka.silentsupport.assertions.hasVersion
import org.junit.Test
import java.io.File

class SupportCompatTest {

    @Test
    fun `finds support-compat dep from classpath`() {
        val supportCompat = SupportCompat.fromClasspath(listOf(File("test-libs/support-compat-25.3.0.aar")))

        assert(supportCompat).isNotNull()
    }

    @Test
    fun `extracts correct version`() {
        val supportCompat = SupportCompat.fromClasspath(listOf(File("test-libs/support-compat-25.3.0.aar")))

        assert(supportCompat).isNotNull {
            it.hasVersion("25.3.0")
        }
    }
}