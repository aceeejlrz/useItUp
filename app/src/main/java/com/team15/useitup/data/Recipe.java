package com.team15.useitup.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * A recipe as shown on Recipes and Recipe Detail. In the finished app this comes from
 * TheMealDB (MealDetail) and is stored as SavedRecipe + RecipeIngredient rows when saved.
 */
public final class Recipe {

    @NonNull private final String id;
    @NonNull private final String name;
    @NonNull private final String area;
    @NonNull private final String category;
    @DrawableRes private final int imageRes;
    @DrawableRes private final int heroRes;
    @NonNull private final List<RecipeIngredient> ingredients;
    @NonNull private final List<String> steps;

    public Recipe(@NonNull String id, @NonNull String name, @NonNull String area,
                  @NonNull String category, @DrawableRes int imageRes, @DrawableRes int heroRes,
                  @NonNull List<RecipeIngredient> ingredients, @NonNull List<String> steps) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.category = category;
        this.imageRes = imageRes;
        this.heroRes = heroRes;
        this.ingredients = Collections.unmodifiableList(ingredients);
        this.steps = Collections.unmodifiableList(steps);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getArea() {
        return area;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    @DrawableRes
    public int getImageRes() {
        return imageRes;
    }

    /** Wide image for Recipe Detail; falls back to the card image. */
    @DrawableRes
    public int getHeroRes() {
        return heroRes != 0 ? heroRes : imageRes;
    }

    @NonNull
    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    @NonNull
    public List<String> getSteps() {
        return steps;
    }
}
