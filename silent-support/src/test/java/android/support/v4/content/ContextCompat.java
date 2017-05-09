package android.support.v4.content;

import android.graphics.drawable.Drawable;

import com.android.tools.lint.detector.api.Context;

import static org.mockito.Mockito.mock;

public class ContextCompat {
    public static int record_getDrawable_id;

    public static void reset() {
        record_getDrawable_id = 0;
    }

    public static Drawable getDrawable(Context context, int id) {
        record_getDrawable_id = id;
        return mock(Drawable.class);
    }
}
