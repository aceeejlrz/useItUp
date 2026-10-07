package com.team15.useitup;

import static org.junit.Assert.assertEquals;

import com.team15.useitup.util.Expiry;

import org.junit.Test;

import java.time.LocalDate;

public class ExpiryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    @Test
    public void daysUntil_countsWholeDays() {
        assertEquals(-1, Expiry.daysUntil(TODAY.minusDays(1), TODAY));
        assertEquals(0, Expiry.daysUntil(TODAY, TODAY));
        assertEquals(14, Expiry.daysUntil(LocalDate.of(2026, 10, 21), TODAY));
    }

    @Test
    public void status_usesAlertWindow() {
        assertEquals(Expiry.Status.EXPIRED, Expiry.status(-1, 3));
        assertEquals(Expiry.Status.SOON, Expiry.status(0, 3));
        assertEquals(Expiry.Status.SOON, Expiry.status(3, 3));
        assertEquals(Expiry.Status.FRESH, Expiry.status(4, 3));
        assertEquals(Expiry.Status.FRESH, Expiry.status(2, 1));
        assertEquals(Expiry.Status.SOON, Expiry.status(7, 7));
    }

    @Test
    public void labels_matchTheDesignSamples() {
        // Bread, spinach, chicken, milk, yogurt and carrots from the sample pantry.
        assertEquals(Expiry.LabelKind.EXPIRED_YESTERDAY, Expiry.labelKind(-1));
        assertEquals(Expiry.LabelKind.TODAY, Expiry.labelKind(0));
        assertEquals(Expiry.LabelKind.DAYS_LEFT, Expiry.labelKind(2));
        assertEquals(2, Expiry.labelCount(2));
        assertEquals(Expiry.LabelKind.DAYS_LEFT, Expiry.labelKind(9));
        assertEquals(Expiry.LabelKind.WEEKS_LEFT, Expiry.labelKind(14));
        assertEquals(2, Expiry.labelCount(14));
    }

    @Test
    public void labels_edgeCases() {
        assertEquals(Expiry.LabelKind.EXPIRED_DAYS_AGO, Expiry.labelKind(-5));
        assertEquals(5, Expiry.labelCount(-5));
        assertEquals(Expiry.LabelKind.TOMORROW, Expiry.labelKind(1));
        assertEquals(Expiry.LabelKind.DAYS_LEFT, Expiry.labelKind(13));
        assertEquals(Expiry.LabelKind.WEEKS_LEFT, Expiry.labelKind(59));
        assertEquals(8, Expiry.labelCount(59));
        assertEquals(Expiry.LabelKind.MONTHS_LEFT, Expiry.labelKind(90));
        assertEquals(3, Expiry.labelCount(90));
    }
}
