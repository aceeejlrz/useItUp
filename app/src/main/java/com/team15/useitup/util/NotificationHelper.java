package com.team15.useitup.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.team15.useitup.R;
import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.ui.main.MainActivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Expiry reminder notification. In the finished app ExpiryCheckWorker (WorkManager, daily)
 * calls {@link #postExpiryReminder(Context)}; the prototype calls it from Settings.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "expiry_alerts";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationHelper() {
    }

    /** Creates the "Expiry alerts" channel. Safe to call more than once. */
    public static void createChannel(@NonNull Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /** True when the runtime permission (Android 13+) is granted and notifications are on. */
    public static boolean canPost(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Items that expire today or within the alert window (expired items are not included). */
    @NonNull
    public static List<PantryItem> expiringSoon(@NonNull List<PantryItem> items, int alertDays,
                                                @NonNull LocalDate today) {
        List<PantryItem> result = new ArrayList<>();
        for (PantryItem item : items) {
            long days = Expiry.daysUntil(item.getExpiryDate(), today);
            if (Expiry.status(days, alertDays) == Expiry.Status.SOON) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Posts "3 items expire within 3 days" listing the items; tapping it opens the Pantry.
     *
     * @return false if nothing expires soon or notifications are not allowed
     */
    @SuppressLint("MissingPermission")
    public static boolean postExpiryReminder(@NonNull Context context) {
        int alertDays = AppPrefs.alertDays(context);
        List<PantryItem> soon = expiringSoon(AppRepository.get().currentPantry(), alertDays,
                LocalDate.now());
        if (soon.isEmpty() || !canPost(context)) {
            return false;
        }
        Resources res = context.getResources();
        List<String> names = new ArrayList<>();
        for (PantryItem item : soon) {
            names.add(item.getName());
        }
        String window = res.getQuantityString(R.plurals.day_count, alertDays, alertDays);
        String title = res.getQuantityString(R.plurals.notif_title, soon.size(), soon.size(), window);
        String list = TextLists.sentenceList(names, res.getString(R.string.list_separator),
                res.getString(R.string.list_last_separator));
        String text = res.getString(R.string.notif_text, list);

        Intent open = MainActivity.intentForTab(context, MainActivity.TAB_PANTRY);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_useitup)
                .setColor(ContextCompat.getColor(context, R.color.ic_launcher_background))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        return true;
    }
}
