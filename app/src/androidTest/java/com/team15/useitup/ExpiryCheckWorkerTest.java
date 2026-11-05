package com.team15.useitup;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;

import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.worker.ExpiryCheckWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Runs ExpiryCheckWorker directly on a device or emulator.
@RunWith(AndroidJUnit4.class)
public class ExpiryCheckWorkerTest {

    private Context context;
    private ExecutorService executor;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(AppPrefs.KEY_REMINDERS, true).commit();
        executor.shutdown();
    }

    @Test
    public void remindersOn_succeeds() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(AppPrefs.KEY_REMINDERS, true).commit();
        ListenableWorker.Result result = buildWorker().doWork();
        assertEquals(ListenableWorker.Result.success(), result);
    }

    @Test
    public void remindersOff_succeedsWithoutPosting() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(AppPrefs.KEY_REMINDERS, false).commit();
        ListenableWorker.Result result = buildWorker().doWork();
        assertEquals(ListenableWorker.Result.success(), result);
    }

    private ExpiryCheckWorker buildWorker() {
        return TestWorkerBuilder.from(context, ExpiryCheckWorker.class, executor).build();
    }
}
