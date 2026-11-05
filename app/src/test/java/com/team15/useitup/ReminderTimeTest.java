package com.team15.useitup;

import static org.junit.Assert.assertEquals;

import com.team15.useitup.util.ReminderTime;

import org.junit.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ReminderTimeTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    private static ZonedDateTime at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, TORONTO);
    }

    @Test
    public void laterToday_runsToday() {
        Duration d = ReminderTime.delayUntilNext(at(2026, 11, 4, 9, 30), 18);
        assertEquals(Duration.ofHours(8).plusMinutes(30), d);
    }

    @Test
    public void hourAlreadyPassed_runsTomorrow() {
        Duration d = ReminderTime.delayUntilNext(at(2026, 11, 4, 22, 0), 18);
        assertEquals(Duration.ofHours(20), d);
    }

    @Test
    public void exactlyNow_runsTomorrow() {
        Duration d = ReminderTime.delayUntilNext(at(2026, 11, 4, 18, 0), 18);
        assertEquals(Duration.ofHours(24), d);
    }

    @Test
    public void acrossDaylightSavingEnd_isTwentyFiveHours() {
        // Clocks go back on Sun Nov 1, 2026 in Toronto, so that day is 25 hours long.
        Duration d = ReminderTime.delayUntilNext(at(2026, 10, 31, 18, 0), 18);
        assertEquals(Duration.ofHours(25), d);
    }
}
