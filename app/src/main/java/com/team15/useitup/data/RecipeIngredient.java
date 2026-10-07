package com.team15.useitup.data;

import androidx.annotation.NonNull;

/** An ingredient line of a recipe, e.g. "Garlic" / "3 cloves". */
public final class RecipeIngredient {

    @NonNull private final String name;
    @NonNull private final String measure;

    public RecipeIngredient(@NonNull String name, @NonNull String measure) {
        this.name = name;
        this.measure = measure;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getMeasure() {
        return measure;
    }
}
