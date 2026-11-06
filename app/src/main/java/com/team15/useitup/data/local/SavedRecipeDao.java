package com.team15.useitup.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.team15.useitup.data.Recipe;
import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class SavedRecipeDao {
    @Transaction
    @Query("SELECT * FROM SavedRecipe ORDER BY savedOrder")
    public abstract List<SavedRecipeWithIngredients> getAll();

    @Insert
    public abstract void insertRecipe(SavedRecipe recipe);

    @Insert
    public abstract void insertIngredients(List<RecipeIngredient> ingredients);

    @Query("DELETE FROM SavedRecipe")
    public abstract void deleteAll();

    /** Parent metadata and all ingredient rows are committed together, including removals. */
    @Transaction
    public void replaceAll(List<Recipe> recipes) {
        deleteAll();
        for (int order = 0; order < recipes.size(); order++) {
            Recipe source = recipes.get(order);
            SavedRecipe row = new SavedRecipe();
            row.id = source.getId();
            row.name = source.getName();
            row.area = source.getArea();
            row.category = source.getCategory();
            row.imageUrl = source.getImageUrl();
            row.steps = source.getSteps();
            row.savedOrder = order;
            insertRecipe(row);
            List<RecipeIngredient> lines = new ArrayList<>();
            for (int i = 0; i < source.getIngredients().size(); i++) {
                com.team15.useitup.data.RecipeIngredient sourceLine = source.getIngredients().get(i);
                RecipeIngredient line = new RecipeIngredient();
                line.recipeId = row.id;
                line.position = i;
                line.name = sourceLine.getName();
                line.measure = sourceLine.getMeasure();
                lines.add(line);
            }
            insertIngredients(lines);
        }
    }
}
