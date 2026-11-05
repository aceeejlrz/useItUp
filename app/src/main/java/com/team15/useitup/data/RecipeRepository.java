package com.team15.useitup.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.team15.useitup.util.IngredientMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Recipe search and lookup on TheMealDB. Results are kept in memory so Recipe Detail and the
 * Saved tab can find them by id until the process ends.
 *
 * <p>The free API filters by one ingredient at a time and only returns id, name and photo.
 * A search therefore sends one filter request per ingredient (singular and plural spelling,
 * because TheMealDB matches the name exactly), counts how many searched ingredients each
 * meal appeared for and ranks by that count. The full ingredient list comes from
 * {@link #loadDetails}. All methods and callbacks run on the main thread.
 */
public final class RecipeRepository {

    public interface SearchCallback {
        void onResult(@NonNull List<Match> matches);

        /** A request failed; nothing from this search was returned. */
        void onError();
    }

    public interface DetailsCallback {
        void onResult(@NonNull Recipe recipe);

        void onError();
    }

    /** A meal from a search and how many of the searched ingredients it uses. */
    public static final class Match {
        @NonNull public final Recipe recipe;
        public final int matchCount;

        Match(@NonNull Recipe recipe, int matchCount) {
            this.recipe = recipe;
            this.matchCount = matchCount;
        }
    }

    private static final int MAX_INGREDIENTS = 20;
    private static final Pattern LINE_BREAK = Pattern.compile("\\r?\\n");
    private static final Pattern STEP_LABEL =
            Pattern.compile("^(?i)(step\\s*\\d+|\\d+[.)](?=\\s|$))[\\s:.\\-–]*");

    private static RecipeRepository instance;

    private final MealDbService service;
    /** Meals seen in search results (name and photo only). */
    private final Map<String, Recipe> summaries = new HashMap<>();
    /** Meals with ingredients and instructions from lookup.php. */
    private final Map<String, Recipe> details = new HashMap<>();
    private final List<Call<?>> searchCalls = new ArrayList<>();
    private int searchGeneration;

    private RecipeRepository() {
        service = new Retrofit.Builder()
                .baseUrl(MealDbService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MealDbService.class);
    }

    @MainThread
    @NonNull
    public static RecipeRepository get() {
        if (instance == null) {
            instance = new RecipeRepository();
        }
        return instance;
    }

    /** The full recipe if it has been looked up, else the search summary, else null. */
    @Nullable
    public Recipe find(@Nullable String id) {
        Recipe full = details.get(id);
        return full != null ? full : summaries.get(id);
    }

    /** True if {@link #find} only has the name and photo, so {@link #loadDetails} is needed. */
    public boolean needsDetails(@NonNull String id) {
        return !details.containsKey(id) && summaries.containsKey(id);
    }

    /** Starts a search, cancelling any search still running. */
    public void search(@NonNull List<String> ingredients, @NonNull SearchCallback callback) {
        cancelSearch();
        int generation = searchGeneration;
        List<Set<String>> idsPerIngredient = new ArrayList<>();
        Map<String, MealDbService.MealSummary> meals = new HashMap<>();
        int[] pending = {0};
        boolean[] failed = {false};

        for (String ingredient : ingredients) {
            Set<String> ids = new LinkedHashSet<>();
            idsPerIngredient.add(ids);
            for (String query : queryNames(ingredient)) {
                pending[0]++;
                Call<MealDbService.MealList> call = service.filterByIngredient(query);
                searchCalls.add(call);
                call.enqueue(new Callback<MealDbService.MealList>() {
                    @Override
                    public void onResponse(@NonNull Call<MealDbService.MealList> c,
                                           @NonNull Response<MealDbService.MealList> response) {
                        if (generation != searchGeneration || failed[0]) {
                            return;
                        }
                        MealDbService.MealList body = response.body();
                        if (!response.isSuccessful() || body == null) {
                            fail();
                            return;
                        }
                        if (body.meals != null) {
                            for (MealDbService.MealSummary meal : body.meals) {
                                if (meal.idMeal != null && meal.strMeal != null) {
                                    ids.add(meal.idMeal);
                                    meals.put(meal.idMeal, meal);
                                }
                            }
                        }
                        if (--pending[0] == 0) {
                            searchCalls.clear();
                            callback.onResult(toMatches(rank(idsPerIngredient), meals));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MealDbService.MealList> c, @NonNull Throwable t) {
                        if (generation == searchGeneration && !failed[0]) {
                            fail();
                        }
                    }

                    private void fail() {
                        failed[0] = true;
                        cancelSearch();
                        callback.onError();
                    }
                });
            }
        }
    }

    /** Cancels the running search; its callback is not called. */
    public void cancelSearch() {
        searchGeneration++;
        for (Call<?> call : searchCalls) {
            call.cancel();
        }
        searchCalls.clear();
    }

    /** Fetches a meal's ingredients and instructions with lookup.php. */
    public void loadDetails(@NonNull String id, @NonNull DetailsCallback callback) {
        service.lookup(id).enqueue(new Callback<MealDbService.MealDetailList>() {
            @Override
            public void onResponse(@NonNull Call<MealDbService.MealDetailList> c,
                                   @NonNull Response<MealDbService.MealDetailList> response) {
                MealDbService.MealDetailList body = response.body();
                if (!response.isSuccessful() || body == null || body.meals == null || body.meals.isEmpty()) {
                    callback.onError();
                    return;
                }
                Recipe recipe = toRecipe(body.meals.get(0));
                details.put(recipe.getId(), recipe);
                callback.onResult(recipe);
            }

            @Override
            public void onFailure(@NonNull Call<MealDbService.MealDetailList> c, @NonNull Throwable t) {
                callback.onError();
            }
        });
    }

    @NonNull
    private List<Match> toMatches(@NonNull Map<String, Integer> ranked,
                                  @NonNull Map<String, MealDbService.MealSummary> meals) {
        List<Match> matches = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ranked.entrySet()) {
            Recipe recipe = details.get(entry.getKey());
            if (recipe == null) {
                MealDbService.MealSummary meal = meals.get(entry.getKey());
                recipe = new Recipe(meal.idMeal, meal.strMeal, orEmpty(meal.strArea), "",
                        meal.strMealThumb, Collections.emptyList(), Collections.emptyList());
                summaries.put(recipe.getId(), recipe);
            }
            matches.add(new Match(recipe, entry.getValue()));
        }
        return matches;
    }

    // ------------------------------------------------------ pure helpers (unit tested)

    /** Names to send to filter.php for one ingredient, e.g. chicken_breast and chicken_breasts. */
    @VisibleForTesting
    @NonNull
    static List<String> queryNames(@NonNull String ingredient) {
        String singular = IngredientMatcher.normalize(ingredient);
        String typed = ingredient.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        Set<String> names = new LinkedHashSet<>();
        for (String name : new String[] {singular, typed, singular + "s"}) {
            if (!name.isEmpty() && !name.equals("s")) {
                names.add(name.replace(' ', '_'));
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * Meal ids mapped to how many ingredients they appeared for, most first. Ties keep the
     * order the ids were first seen in.
     */
    @VisibleForTesting
    @NonNull
    static Map<String, Integer> rank(@NonNull List<? extends Set<String>> idsPerIngredient) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Set<String> ids : idsPerIngredient) {
            for (String id : ids) {
                Integer count = counts.get(id);
                counts.put(id, count == null ? 1 : count + 1);
            }
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        // Stable sort keeps first-seen order between equal counts.
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        Map<String, Integer> ranked = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            ranked.put(entry.getKey(), entry.getValue());
        }
        return ranked;
    }

    /** Maps one lookup.php meal to a Recipe. */
    @VisibleForTesting
    @NonNull
    static Recipe toRecipe(@NonNull Map<String, String> meal) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (int i = 1; i <= MAX_INGREDIENTS; i++) {
            String name = orEmpty(meal.get("strIngredient" + i)).trim();
            if (!name.isEmpty()) {
                ingredients.add(new RecipeIngredient(capitalize(name),
                        orEmpty(meal.get("strMeasure" + i)).trim()));
            }
        }
        String area = orEmpty(meal.get("strArea"));
        if (area.isEmpty()) {
            area = orEmpty(meal.get("strCountry"));
        }
        return new Recipe(orEmpty(meal.get("idMeal")), orEmpty(meal.get("strMeal")), area,
                orEmpty(meal.get("strCategory")), meal.get("strMealThumb"), ingredients,
                steps(meal.get("strInstructions")));
    }

    /** Splits instructions into steps, dropping blank lines and "STEP 1" / "1." labels. */
    @VisibleForTesting
    @NonNull
    static List<String> steps(@Nullable String instructions) {
        List<String> steps = new ArrayList<>();
        if (instructions == null) {
            return steps;
        }
        for (String line : LINE_BREAK.split(instructions)) {
            String text = STEP_LABEL.matcher(line.trim()).replaceFirst("").trim();
            if (!text.isEmpty()) {
                steps.add(text);
            }
        }
        return steps;
    }

    @NonNull
    private static String capitalize(@NonNull String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    @NonNull
    private static String orEmpty(@Nullable String s) {
        return s != null ? s : "";
    }
}
