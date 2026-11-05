package com.team15.useitup.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipeRepositoryTest {

    @Test
    public void queryNames_sendsSingularAndPluralWithUnderscores() {
        assertEquals(Arrays.asList("chicken_breast", "chicken_breasts"),
                RecipeRepository.queryNames("Chicken breast"));
        assertEquals(Arrays.asList("chicken_breast", "chicken_breasts"),
                RecipeRepository.queryNames("  Chicken   Breasts "));
        assertEquals(Arrays.asList("tomato", "tomatoes", "tomatos"),
                RecipeRepository.queryNames("Tomatoes"));
    }

    @Test
    public void rank_countsIngredientsPerMealAndSortsByCount() {
        List<Set<String>> ids = Arrays.asList(ids("1", "2", "3"), ids("4", "2"), ids("2", "4"));
        Map<String, Integer> ranked = RecipeRepository.rank(ids);
        assertEquals(Arrays.asList("2", "4", "1", "3"), Arrays.asList(ranked.keySet().toArray()));
        assertEquals(Integer.valueOf(3), ranked.get("2"));
        assertEquals(Integer.valueOf(2), ranked.get("4"));
        assertEquals(Integer.valueOf(1), ranked.get("1"));
    }

    @Test
    public void rank_emptyWhenNothingMatches() {
        assertEquals(Collections.emptyMap(), RecipeRepository.rank(Arrays.asList(ids(), ids())));
    }

    @Test
    public void toRecipe_readsNumberedIngredientsAndSkipsBlanks() {
        Map<String, String> meal = new HashMap<>();
        meal.put("idMeal", "52780");
        meal.put("strMeal", "Potato Gratin with Chicken");
        meal.put("strCategory", "Chicken");
        meal.put("strArea", null);
        meal.put("strCountry", "Italy");
        meal.put("strMealThumb", "https://www.themealdb.com/images/media/meals/x.jpg");
        meal.put("strInstructions", "STEP 1\r\nHeat the oven.\r\n\r\n2. Add 1.5 cups of milk.");
        meal.put("strIngredient1", "chicken breasts");
        meal.put("strMeasure1", " 2 ");
        meal.put("strIngredient2", "Spinach");
        meal.put("strMeasure2", null);
        meal.put("strIngredient3", "");
        meal.put("strMeasure3", "");
        meal.put("strIngredient4", null);

        Recipe recipe = RecipeRepository.toRecipe(meal);

        assertEquals("52780", recipe.getId());
        assertEquals("Potato Gratin with Chicken", recipe.getName());
        assertEquals("Italy", recipe.getArea());
        assertEquals("Chicken", recipe.getCategory());
        assertEquals("https://www.themealdb.com/images/media/meals/x.jpg", recipe.getImageUrl());
        assertEquals(2, recipe.getIngredients().size());
        assertEquals("Chicken breasts", recipe.getIngredients().get(0).getName());
        assertEquals("2", recipe.getIngredients().get(0).getMeasure());
        assertEquals("Spinach", recipe.getIngredients().get(1).getName());
        assertEquals("", recipe.getIngredients().get(1).getMeasure());
        assertEquals(Arrays.asList("Heat the oven.", "Add 1.5 cups of milk."), recipe.getSteps());
    }

    @Test
    public void steps_keepsLinesThatStartWithAQuantity() {
        assertEquals(Arrays.asList("2 cups flour", "1.5 L stock"),
                RecipeRepository.steps("2 cups flour\n1.5 L stock"));
        assertEquals(Collections.emptyList(), RecipeRepository.steps(null));
    }

    @Test
    public void sampleRecipes_haveNoImageUrl() {
        assertNull(SampleData.recipes().get(0).getImageUrl());
    }

    private static Set<String> ids(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
