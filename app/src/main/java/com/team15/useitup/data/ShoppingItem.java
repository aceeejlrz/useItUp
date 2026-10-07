package com.team15.useitup.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** One line on the shopping list. Mirrors the planned Room entity {@code shopping_items}. */
public final class ShoppingItem {

    private final long id;
    @NonNull private final String name;
    @Nullable private final String quantityNote;
    private final boolean checked;
    @Nullable private final String fromRecipe;
    private final long createdAt;

    public ShoppingItem(long id, @NonNull String name, @Nullable String quantityNote,
                        boolean checked, @Nullable String fromRecipe, long createdAt) {
        this.id = id;
        this.name = name;
        this.quantityNote = quantityNote;
        this.checked = checked;
        this.fromRecipe = fromRecipe;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getQuantityNote() {
        return quantityNote;
    }

    public boolean isChecked() {
        return checked;
    }

    @Nullable
    public String getFromRecipe() {
        return fromRecipe;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @NonNull
    public ShoppingItem withChecked(boolean value) {
        return new ShoppingItem(id, name, quantityNote, value, fromRecipe, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShoppingItem)) return false;
        ShoppingItem that = (ShoppingItem) o;
        return id == that.id
                && checked == that.checked
                && createdAt == that.createdAt
                && name.equals(that.name)
                && Objects.equals(quantityNote, that.quantityNote)
                && Objects.equals(fromRecipe, that.fromRecipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, quantityNote, checked, fromRecipe, createdAt);
    }
}
