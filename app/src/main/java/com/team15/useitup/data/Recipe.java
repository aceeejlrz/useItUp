package com.team15.useitup.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A recipe as shown on Recipes and Recipe Detail. Sample recipes use bundled drawables;
 * TheMealDB recipes have a photo URL instead (loaded with Glide) and no drawables.
 */
public final class Recipe {

    @NonNull private final String id;
    @NonNull private final String name;
    @NonNull private final String area;
    @NonNull private final String category;
    @DrawableRes private final int imageRes;
    @DrawableRes private final int heroRes;
    @Nullable private final String imageUrl;
    @NonNull private final List<RecipeIngredient> ingredients;
    @NonNull private final List<String> steps;

    public Recipe(@NonNull String id, @NonNull String name, @NonNull String area,
                  @NonNull String category, @DrawableRes int imageRes, @DrawableRes int heroRes,
                  @NonNull List<RecipeIngredient> ingredients, @NonNull List<String> steps) {
        this(id, name, area, category, imageRes, heroRes, null, ingredients, steps);
    }

    /** A TheMealDB recipe whose card and hero image is the photo at {@code imageUrl}. */
    public Recipe(@NonNull String id, @NonNull String name, @NonNull String area,
                  @NonNull String category, @Nullable String imageUrl,
                  @NonNull List<RecipeIngredient> ingredients, @NonNull List<String> steps) {
        this(id, name, area, category, 0, 0, imageUrl, ingredients, steps);
    }

    private Recipe(@NonNull String id, @NonNull String name, @NonNull String area,
                   @NonNull String category, @DrawableRes int imageRes, @DrawableRes int heroRes,
                   @Nullable String imageUrl, @NonNull List<RecipeIngredient> ingredients,
                   @NonNull List<String> steps) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.category = category;
        this.imageRes = imageRes;
        this.heroRes = heroRes;
        this.imageUrl = imageUrl;
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

    /** Photo URL for TheMealDB recipes; null for sample recipes, which use the drawables. */
    @Nullable
    public String getImageUrl() {
        return imageUrl;
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
