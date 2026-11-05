package com.team15.useitup;

import android.app.Application;

import androidx.preference.PreferenceManager;

import com.team15.useitup.util.NotificationHelper;
import com.team15.useitup.worker.ReminderScheduler;

/** Application entry point. Sets up app-wide services before any screen opens. */
public class UseItUpApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Defaults: reminders on, 3-day alert window.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // The "Expiry alerts" channel must exist before the first reminder is posted.
        NotificationHelper.createChannel(this);
        ReminderScheduler.schedule(this, false);
    }
}
