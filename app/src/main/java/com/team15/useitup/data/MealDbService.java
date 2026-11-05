package com.team15.useitup.data;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * TheMealDB v1 with the free test key. Multi-ingredient filtering is premium only, so
 * {@link #filterByIngredient} takes one ingredient and the app merges the results itself.
 */
interface MealDbService {

    String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";

    /** Meals that use the ingredient (exact name, spaces as underscores). Only id, name and photo. */
    @GET("filter.php")
    Call<MealList> filterByIngredient(@Query("i") String ingredient);

    /** One meal with its instructions and strIngredient1..20 / strMeasure1..20. */
    @GET("lookup.php")
    Call<MealDetailList> lookup(@Query("i") String id);

    /** {@code meals} is null when nothing matches. */
    final class MealList {
        @Nullable List<MealSummary> meals;
    }

    final class MealSummary {
        String idMeal;
        String strMeal;
        @Nullable String strMealThumb;
        @Nullable String strArea;
    }

    /** Kept as a map because the ingredients are 20 numbered fields. */
    final class MealDetailList {
        @Nullable List<Map<String, String>> meals;
    }
}
