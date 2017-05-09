package android.support.v4.view

import android.view.View

object ViewCompat {
    var record_getX: Boolean = false

    fun reset() {
        record_getX = false
    }

    @JvmStatic
    fun getX(view: View): Float {
        record_getX = true
        return 0f
    }
}
