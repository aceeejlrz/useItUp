package com.team15.useitup.util;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge helpers. Target SDK 37 always draws behind the system bars, so screens without
 * a bottom navigation bar pad their content for the gesture bar and the on-screen keyboard.
 */
public final class SystemBars {

    private SystemBars() {
    }

    /** Adds the navigation bar or keyboard height (whichever is larger) to the bottom padding. */
    public static void padBottom(@NonNull View view) {
        final int initialBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    initialBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /** Adds only the keyboard height to the bottom padding (for screens with a bottom bar). */
    public static void padBottomForKeyboard(@NonNull View view) {
        final int initialBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    initialBottom + Math.max(0, ime - nav));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
