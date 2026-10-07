package com.team15.useitup.util;

import android.content.res.Resources;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import com.team15.useitup.R;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Expiry status and labels. The status is calculated every time a list is shown, from the
 * expiry date and the alert window setting, so it is never stored and never out of date.
 */
public final class Expiry {

    /** Expired, inside the alert window, or fresh. */
    public enum Status { EXPIRED, SOON, FRESH }

    /** Which label wording applies; the count comes from {@link #labelCount(long)}. */
    public enum LabelKind {
        EXPIRED_DAYS_AGO, EXPIRED_YESTERDAY, TODAY, TOMORROW, DAYS_LEFT, WEEKS_LEFT, MONTHS_LEFT
    }

    private Expiry() {
    }

    /** Whole days from today until the expiry date; negative once it has passed. */
    public static long daysUntil(@NonNull LocalDate expiry, @NonNull LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiry);
    }

    @NonNull
    public static Status status(long daysLeft, int alertDays) {
        if (daysLeft < 0) {
            return Status.EXPIRED;
        }
        return daysLeft <= alertDays ? Status.SOON : Status.FRESH;
    }

    @NonNull
    public static LabelKind labelKind(long daysLeft) {
        if (daysLeft < -1) return LabelKind.EXPIRED_DAYS_AGO;
        if (daysLeft == -1) return LabelKind.EXPIRED_YESTERDAY;
        if (daysLeft == 0) return LabelKind.TODAY;
        if (daysLeft == 1) return LabelKind.TOMORROW;
        if (daysLeft < 14) return LabelKind.DAYS_LEFT;
        if (daysLeft < 60) return LabelKind.WEEKS_LEFT;
        return LabelKind.MONTHS_LEFT;
    }

    /** The number shown in the label: days ago, days, weeks or months. */
    public static long labelCount(long daysLeft) {
        switch (labelKind(daysLeft)) {
            case EXPIRED_DAYS_AGO:
                return -daysLeft;
            case WEEKS_LEFT:
                return daysLeft / 7;
            case MONTHS_LEFT:
                return daysLeft / 30;
            case DAYS_LEFT:
                return daysLeft;
            default:
                return 0;
        }
    }

    /** Badge text, e.g. "Expired yesterday", "Expires today", "2 days left", "2 weeks left". */
    @NonNull
    public static String label(@NonNull Resources res, long daysLeft) {
        int count = (int) labelCount(daysLeft);
        switch (labelKind(daysLeft)) {
            case EXPIRED_DAYS_AGO:
                return res.getQuantityString(R.plurals.status_expired_days_ago, count, count);
            case EXPIRED_YESTERDAY:
                return res.getString(R.string.status_expired_yesterday);
            case TODAY:
                return res.getString(R.string.status_expires_today);
            case TOMORROW:
                return res.getString(R.string.status_expires_tomorrow);
            case DAYS_LEFT:
                return res.getQuantityString(R.plurals.status_days_left, count, count);
            case WEEKS_LEFT:
                return res.getQuantityString(R.plurals.status_weeks_left, count, count);
            default:
                return res.getQuantityString(R.plurals.status_months_left, count, count);
        }
    }

    /** Helper text under the expiry date field, e.g. "Expires in 5 days". */
    @NonNull
    public static String helperText(@NonNull Resources res, long daysLeft) {
        if (daysLeft >= 2) {
            int days = (int) daysLeft;
            return res.getQuantityString(R.plurals.helper_expires_in, days, days);
        }
        return label(res, daysLeft);
    }

    @ColorRes
    public static int edgeColor(@NonNull Status status) {
        switch (status) {
            case EXPIRED:
                return R.color.status_expired_edge;
            case SOON:
                return R.color.status_soon_edge;
            default:
                return R.color.status_fresh_edge;
        }
    }

    @ColorRes
    public static int badgeBackground(@NonNull Status status) {
        switch (status) {
            case EXPIRED:
                return R.color.status_expired_bg;
            case SOON:
                return R.color.status_soon_bg;
            default:
                return R.color.status_fresh_bg;
        }
    }

    @ColorRes
    public static int badgeForeground(@NonNull Status status) {
        switch (status) {
            case EXPIRED:
                return R.color.status_expired_fg;
            case SOON:
                return R.color.status_soon_fg;
            default:
                return R.color.status_fresh_fg;
        }
    }
}
