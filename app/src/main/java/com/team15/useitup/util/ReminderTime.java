package com.team15.useitup.util;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public final class ReminderTime {

    private ReminderTime() {
    }

    @NonNull
    public static Duration delayUntilNext(@NonNull ZonedDateTime now, int hour) {
        ZonedDateTime next = ZonedDateTime.of(now.toLocalDate(), LocalTime.of(hour, 0), now.getZone());
        if (!next.isAfter(now)) {
            next = ZonedDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(hour, 0), now.getZone());
        }
        return Duration.between(now, next);
    }
}