package com.team15.useitup.ui.pantry;

import androidx.annotation.NonNull;

import com.team15.useitup.data.PantryItem;
import com.team15.useitup.util.Expiry;

import java.util.Objects;

/** A pantry item plus its calculated expiry status for display. */
public final class PantryRow {

    @NonNull public final PantryItem item;
    public final long daysLeft;
    @NonNull public final Expiry.Status status;

    public PantryRow(@NonNull PantryItem item, long daysLeft, @NonNull Expiry.Status status) {
        this.item = item;
        this.daysLeft = daysLeft;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PantryRow)) return false;
        PantryRow that = (PantryRow) o;
        return daysLeft == that.daysLeft && item.equals(that.item) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, daysLeft, status);
    }
}
