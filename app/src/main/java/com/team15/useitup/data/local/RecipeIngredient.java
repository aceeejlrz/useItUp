package com.team15.useitup.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

/** Position is part of the key: repeated ingredient names and their order are preserved. */
@Entity(tableName = "RecipeIngredient", primaryKeys = {"recipeId", "position"},
        foreignKeys = @ForeignKey(entity = SavedRecipe.class, parentColumns = "id",
                childColumns = "recipeId", onDelete = ForeignKey.CASCADE))
public class RecipeIngredient {
    @NonNull public String recipeId = "";
    public int position;
    @NonNull public String name = "";
    @NonNull public String measure = "";
}
