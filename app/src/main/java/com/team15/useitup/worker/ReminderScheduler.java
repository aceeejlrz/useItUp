package com.team15.useitup.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.ReminderTime;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

//Schedules the daily ExpiryCheckWorker as unique periodic work.
//App start(keeps the existing schedule) and from Settings when the reminder switch or time changes.
// The alert window is read by the worker each time it runs, so changing it needs no reschedule.
public final class ReminderScheduler {

    public static final String WORK_NAME = "expiry_check_daily";

    private ReminderScheduler() {
    }

    public static void schedule(@NonNull Context context, boolean replace) {
        Context app = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(app);

        if (!AppPrefs.remindersEnabled(app)) {
            workManager.cancelUniqueWork(WORK_NAME);
            return;
        }

        // First run at the chosen hour, then every 24 hours. WorkManager may run it a little
        // late (Doze, battery saver), which is why Settings says "around" 6:00 PM.
        long delayMillis = ReminderTime
                .delayUntilNext(ZonedDateTime.now(), AppPrefs.reminderHour(app))
                .toMillis();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(ExpiryCheckWorker.class, 1, TimeUnit.DAYS)
                        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                        .addTag(WORK_NAME)
                        .build();

        workManager.enqueueUniquePeriodicWork(WORK_NAME,
                replace ? ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                        : ExistingPeriodicWorkPolicy.KEEP,
                request);
    }
}