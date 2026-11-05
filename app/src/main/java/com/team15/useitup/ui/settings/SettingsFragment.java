package com.team15.useitup.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.team15.useitup.R;
import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.NotificationHelper;
import com.team15.useitup.util.SystemBars;
import com.team15.useitup.worker.ReminderScheduler;

/** The preference list. Values are stored in the default SharedPreferences (see AppPrefs). */
public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG_TIME_PICKER = "reminder_time_picker";

    private ActivityResultLauncher<String> permissionLauncher;
    private boolean sendTestAfterPermission;
    @Nullable private Preference timePreference;
    @Nullable private PermissionPreference permissionPreference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            updatePermission();
            if (sendTestAfterPermission) {
                sendTestAfterPermission = false;
                sendTestReminder();
            }
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        SwitchPreferenceCompat reminders = findPreference(AppPrefs.KEY_REMINDERS);
        if (reminders != null) {
            reminders.setOnPreferenceChangeListener((preference, newValue) -> {
                if (Boolean.TRUE.equals(newValue)) {
                    requestPermissionIfNeeded(false);
                }
                return true;
            });
        }

        timePreference = findPreference(AppPrefs.KEY_REMINDER_HOUR);
        if (timePreference != null) {
            timePreference.setOnPreferenceClickListener(p -> {
                showTimePicker();
                return true;
            });
        }
        updateTimeSummary();

        permissionPreference = findPreference(AppPrefs.KEY_PERMISSION);
        if (permissionPreference != null) {
            permissionPreference.setOnManageClick(v -> openNotificationSettings());
        }

        Preference test = findPreference(AppPrefs.KEY_TEST_REMINDER);
        if (test != null) {
            test.setOnPreferenceClickListener(p -> {
                if (NotificationHelper.canPost(requireContext())) {
                    sendTestReminder();
                } else {
                    requestPermissionIfNeeded(true);
                }
                return true;
            });
        }

        Preference version = findPreference(AppPrefs.KEY_VERSION);
        if (version != null) {
            version.setTitle(getString(R.string.about_version_title, versionName()));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setClipToPadding(false);
        SystemBars.padBottom(getListView());

        // Re-attach the listener if the time picker survived a rotation.
        Fragment existing = getChildFragmentManager().findFragmentByTag(TAG_TIME_PICKER);
        if (existing instanceof MaterialTimePicker) {
            MaterialTimePicker picker = (MaterialTimePicker) existing;
            picker.addOnPositiveButtonClickListener(v -> onTimePicked(picker.getHour()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // The user may come back from the system notification settings.
        updatePermission();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(prefsListener);
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(prefsListener);
        super.onPause();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (prefs, key) -> {
                if (AppPrefs.KEY_REMINDERS.equals(key) || AppPrefs.KEY_REMINDER_HOUR.equals(key)) {
                    ReminderScheduler.schedule(requireContext(), true);
                }
            };

    private void updatePermission() {
        if (permissionPreference != null && isAdded()) {
            permissionPreference.setAllowed(NotificationHelper.canPost(requireContext()));
        }
    }

    private void updateTimeSummary() {
        if (timePreference != null) {
            timePreference.setSummary(getString(R.string.pref_time_summary,
                    AppPrefs.formatHour(AppPrefs.reminderHour(requireContext()))));
        }
    }

    private void showTimePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_TIME_PICKER) != null) {
            return;
        }
        int format = DateFormat.is24HourFormat(requireContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(format)
                .setHour(AppPrefs.reminderHour(requireContext()))
                .setMinute(0)
                .setTitleText(R.string.select_time)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();
        picker.addOnPositiveButtonClickListener(v -> onTimePicked(picker.getHour()));
        picker.show(getChildFragmentManager(), TAG_TIME_PICKER);
    }

    private void onTimePicked(int hour) {
        // The reminder runs "around" this hour (WorkManager daily work is not exact).
        AppPrefs.setReminderHour(requireContext(), hour);
        updateTimeSummary();
    }

    private void requestPermissionIfNeeded(boolean thenSendTest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            sendTestAfterPermission = thenSendTest;
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else if (thenSendTest) {
            sendTestReminder();
        }
    }

    private void sendTestReminder() {
        if (!isAdded()) {
            return;
        }
        String text;
        if (NotificationHelper.postExpiryReminder(requireContext())) {
            text = getString(R.string.test_reminder_sent);
        } else if (NotificationHelper.canPost(requireContext())) {
            int days = AppPrefs.alertDays(requireContext());
            text = getResources().getQuantityString(R.plurals.test_reminder_none, days, days);
        } else {
            text = getString(R.string.permission_rationale);
        }
        Snackbar.make(requireView(), text, Snackbar.LENGTH_LONG).show();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }

    @NonNull
    private String versionName() {
        try {
            String name = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            return name != null ? name : "";
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
