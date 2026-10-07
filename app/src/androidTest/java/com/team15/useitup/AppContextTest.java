package com.team15.useitup;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumented smoke test, runs on a device or emulator. */
@RunWith(AndroidJUnit4.class)
public class AppContextTest {

    @Test
    public void usesTheAppPackage() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.team15.useitup", context.getPackageName());
    }
}
