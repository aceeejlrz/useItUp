package com.team15.useitup.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** One persisted food item in the pantry. */
@Entity(tableName = "pantry_items")
public final class PantryItem {

    @PrimaryKey
    private final long id;
    @NonNull private final String name;
    private final double quantity;
    @NonNull private final String unit;
    @NonNull private final String category;
    @NonNull private final LocalDate expiryDate;
    @NonNull private final LocalDate addedDate;

    public PantryItem(long id, @NonNull String name, double quantity, @NonNull String unit,
                      @NonNull String category, @NonNull LocalDate expiryDate,
                      @NonNull LocalDate addedDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.expiryDate = expiryDate;
        this.addedDate = addedDate;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    @NonNull
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    @NonNull
    public LocalDate getAddedDate() {
        return addedDate;
    }

    /** Quantity without a trailing ".0", e.g. "500", "1.5". */
    @NonNull
    public String quantityNumberText() {
        return formatQuantity(quantity);
    }

    /** Quantity and unit, e.g. "500 g" or "1 loaf". */
    @NonNull
    public String quantityText() {
        return formatQuantity(quantity) + " " + unit;
    }

    @NonNull
    public static String formatQuantity(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%d", (long) value);
        }
        return String.format(Locale.ROOT, "%s", value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PantryItem)) return false;
        PantryItem that = (PantryItem) o;
        return id == that.id
                && Double.compare(that.quantity, quantity) == 0
                && name.equals(that.name)
                && unit.equals(that.unit)
                && category.equals(that.category)
                && expiryDate.equals(that.expiryDate)
                && addedDate.equals(that.addedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, quantity, unit, category, expiryDate, addedDate);
    }
}
