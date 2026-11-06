package com.team15.useitup.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

//Daily background check. Reads the pantry and, if anything expires inside the alert window,
//posts one summary notification. Tapping it opens the Pantry tab.
//Runs on a WorkManager background thread, scheduled by ReminderScheduler.
public class ExpiryCheckWorker extends Worker {

    private static final String TAG = "ExpiryCheckWorker";

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Reminders turned off, or notifications blocked: nothing to do today. Return success
        // (not retry) so WorkManager just tries again tomorrow.
        if (!AppPrefs.remindersEnabled(context) || !NotificationHelper.canPost(context)) {
            return Result.success();
        }

        List<PantryItem> pantry;
        try {
            pantry = loadPantry();
        } catch (Exception e) {
            Log.w(TAG, "Could not read the pantry", e);
            return Result.retry();
        }

        boolean posted = NotificationHelper.postExpiryReminder(context, pantry);
        Log.d(TAG, posted ? "Expiry reminder posted" : "Nothing expires soon");
        return Result.success();
    }

    // get() waits for the Room cache on this worker thread, even without an Activity.
    @WorkerThread
    @NonNull
    private List<PantryItem> loadPantry() {
        return new ArrayList<>(AppRepository.get().currentPantry());
    }
}
