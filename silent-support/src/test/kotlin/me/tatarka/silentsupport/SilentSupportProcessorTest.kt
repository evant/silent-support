package me.tatarka.silentsupport

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.view.View
import me.tatarka.assertk.assert
import me.tatarka.assertk.assertions.isEqualTo
import me.tatarka.assertk.assertions.isNotNull
import me.tatarka.assertk.assertions.isTrue
import me.tatarka.assertk.assertions.isZero
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.io.File
import java.util.function.Function

@Suppress("UNCHECKED_CAST")
class SilentSupportProcessorTest {
    @Before
    fun setup() {
        ContextCompat.reset()
        ViewCompat.reset()
    }

    private fun processor(apiLevel: Int): SilentSupportProcessor {
        val classpath = listOf(File("test-libs/android-25.jar"), File("test-libs/support-compat-25.3.0.aar"))
        val outputDir = File("build/resources/test")
        val supportCompat = SupportCompat.fromClasspath(classpath)!!
        val supportMetadataProcessor = SupportMetadataProcessor(supportCompat, outputDir)
        supportMetadataProcessor.process()
        return SilentSupportProcessor(
                classpath,
                supportMetadataProcessor.metadataFile,
                apiLevel,
                outputDir)
    }

    @After
    fun teardown() {
        validateMockitoUsage()
    }

    @Test
    fun `context#getDrawable to ContextCompat#getDrawable because api level is not high enough`() {
        val inputClassFile = compile("TestGetDrawable", """public class TestGetDrawable implements java.util.function.Function<android.content.Context, android.graphics.drawable.Drawable> {
            public android.graphics.drawable.Drawable apply(android.content.Context context) {
                return context.getDrawable(4);
            }
        }""")
        val processedClass = processor(14).process(inputClassFile.classFile())
        inputClassFile.replaceWith(processedClass)
        val testClass: Function<Context, Drawable?> = inputClassFile.load().newInstance() as Function<Context, Drawable?>
        val result = testClass.apply(mock(Context::class.java))

        assert(result).isNotNull()
        assert(ContextCompat.record_getDrawable_id).isEqualTo(4)
    }

    @Test
    fun `context#getDrawable not converted because api level is high enough`() {
        val inputClassFile = compile("TestGetDrawable2", """public class TestGetDrawable2 implements java.util.function.Function<android.content.Context, android.graphics.drawable.Drawable> {
            public android.graphics.drawable.Drawable apply(android.content.Context context) {
                return context.getDrawable(4);
            }
        }""")
        val processedClass = processor(21).process(inputClassFile.classFile())
        inputClassFile.replaceWith(processedClass)
        val testClass: Function<Context, Drawable?> = inputClassFile.load().newInstance() as Function<Context, Drawable?>
        val contextMock = mock(Context::class.java)
        testClass.apply(contextMock)

        assert(ContextCompat.record_getDrawable_id).isZero()
        verify(contextMock).getDrawable(eq(4))
    }

    @Test
    fun `context#createConfigurationContext not converted because it does not exist in ContextCompat`() {
        val inputClassFile = compile("TestCreateConfigurationContext", """public class TestCreateConfigurationContext implements java.util.function.Function<android.content.Context, android.content.Context> {
            public android.content.Context apply(android.content.Context context) {
                return context.createConfigurationContext(null);
            }
        }""")
        val processedClass = processor(16).process(inputClassFile.classFile())
        inputClassFile.replaceWith(processedClass)
        val testClass: Function<Context, Context> = inputClassFile.load().newInstance() as Function<Context, Context>
        val contextMock = mock(Context::class.java)
        testClass.apply(contextMock)

        verify(contextMock).createConfigurationContext(any())
    }

    @Test
    fun `activity#getDrawable to ContextCompat#getDrawable because it extends Context and is in ContextCompat`() {
        val inputClassFile = compile("TestActivityGetDrawable", """public class TestActivityGetDrawable implements java.util.function.Function<android.app.Activity, android.graphics.drawable.Drawable> {
            public android.graphics.drawable.Drawable apply(android.app.Activity activity) {
                return activity.getDrawable(4);
            }
        }""")
        val processedClass = processor(14).process(inputClassFile.classFile())
        inputClassFile.replaceWith(processedClass)
        val testClass: Function<Activity, Drawable?> = inputClassFile.load().newInstance() as Function<Activity, Drawable?>
        val result = testClass.apply(mock(Activity::class.java))

        assert(result).isNotNull()
        assert(ContextCompat.record_getDrawable_id).isEqualTo(4)
    }

    @Test
    fun `view#getX to ViewCompat#getX because api level is not high enough`() {
        val inputClassFile = compile("TestGetX", """public class TestGetX implements java.util.function.Function<android.view.View, Float> {
            public Float apply(android.view.View view) {
                return view.getX();
            }
        }""")
        val processedClass = processor(10).process(inputClassFile.classFile())
        inputClassFile.replaceWith(processedClass)
        val testClass: Function<View, Float> = inputClassFile.load().newInstance() as Function<View, Float>
        val result = testClass.apply(mock(View::class.java))

        assert(result).isNotNull()
        assert(ViewCompat.record_getX).isTrue()
    }
}