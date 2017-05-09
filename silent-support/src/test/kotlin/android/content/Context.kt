package android.content

import android.content.res.Configuration
import android.graphics.drawable.Drawable


open class Context {
    open fun getDrawable(id: Int): Drawable = Drawable()

    open fun createConfigurationContext(overrideConfiguration: Configuration?): Context = Context()
}
