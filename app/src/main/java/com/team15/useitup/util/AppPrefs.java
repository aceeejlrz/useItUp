package com.team15.useitup.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Reminder settings stored in SharedPreferences by the Settings screen. */
public final class AppPrefs {

    public static final String KEY_REMINDERS = "reminders_enabled";
    public static final String KEY_ALERT_DAYS = "alert_days";
    public static final String KEY_REMINDER_HOUR = "reminder_hour";
    public static final String KEY_PERMISSION = "notification_permission";
    public static final String KEY_TEST_REMINDER = "test_reminder";
    public static final String KEY_VERSION = "about_version";
    public static final String KEY_NOTIFICATION_PROMPT_SHOWN = "notification_prompt_shown";

    public static final int DEFAULT_ALERT_DAYS = 3;
    public static final int DEFAULT_REMINDER_HOUR = 18;

    private AppPrefs() {
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean remindersEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_REMINDERS, true);
    }

    /** Days before expiry that count as "expiring soon" (1–7, default 3). */
    public static int alertDays(@NonNull Context context) {
        String value = prefs(context).getString(KEY_ALERT_DAYS, String.valueOf(DEFAULT_ALERT_DAYS));
        try {
            int days = Integer.parseInt(value);
            return Math.max(1, Math.min(7, days));
        } catch (NumberFormatException e) {
            return DEFAULT_ALERT_DAYS;
        }
    }

    public static int reminderHour(@NonNull Context context) {
        return prefs(context).getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR);
    }

    public static void setReminderHour(@NonNull Context context, int hour) {
        prefs(context).edit().putInt(KEY_REMINDER_HOUR, hour).apply();
    }

    public static boolean notificationPromptShown(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_NOTIFICATION_PROMPT_SHOWN, false);
    }

    public static void setNotificationPromptShown(@NonNull Context context) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATION_PROMPT_SHOWN, true).apply();
    }

    /** "6:00 PM" (or "18:00", following the locale). */
    @NonNull
    public static String formatHour(int hour) {
        return LocalTime.of(hour, 0)
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()));
    }
}
