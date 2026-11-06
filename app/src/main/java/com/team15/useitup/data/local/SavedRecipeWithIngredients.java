package com.team15.useitup.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.team15.useitup.data.Recipe;
import com.team15.useitup.data.SampleData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SavedRecipeWithIngredients {
    @Embedded public SavedRecipe recipe;
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    public List<RecipeIngredient> ingredients;

    public Recipe toRecipe() {
        List<com.team15.useitup.data.RecipeIngredient> lines = new ArrayList<>();
        ingredients.sort(Comparator.comparingInt(line -> line.position));
        for (RecipeIngredient line : ingredients) {
            lines.add(new com.team15.useitup.data.RecipeIngredient(line.name, line.measure));
        }
        // Only artwork is resolved from the current build; all text comes from the database.
        if (recipe.imageUrl == null) {
            for (Recipe sample : SampleData.recipes()) {
                if (sample.getId().equals(recipe.id)) {
                    return new Recipe(recipe.id, recipe.name, recipe.area, recipe.category,
                            sample.getImageRes(), sample.getHeroRes(), lines, recipe.steps);
                }
            }
        }
        return new Recipe(recipe.id, recipe.name, recipe.area, recipe.category,
                recipe.imageUrl, lines, recipe.steps);
    }
}
