package com.team15.useitup.data;

import androidx.annotation.NonNull;

import com.team15.useitup.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sample content for the UI prototype. Dates are relative to today so the expiry colours
 * always match the design (one expired item, three expiring soon, two fresh).
 */
public final class SampleData {

    private SampleData() {
    }

    @NonNull
    public static List<PantryItem> pantryItems(@NonNull LocalDate today) {
        LocalDate added = today.minusDays(3);
        List<PantryItem> items = new ArrayList<>();
        items.add(new PantryItem(1, "Bread", 1, "loaf", "Bakery", today.minusDays(1), added));
        items.add(new PantryItem(2, "Spinach", 1, "bag", "Produce", today, added));
        items.add(new PantryItem(3, "Chicken breast", 500, "g", "Meat & Fish", today.plusDays(2), added));
        items.add(new PantryItem(4, "Milk", 1, "L", "Dairy", today.plusDays(3), added));
        items.add(new PantryItem(5, "Greek yogurt", 650, "g", "Dairy", today.plusDays(9), added));
        items.add(new PantryItem(6, "Carrots", 1, "kg", "Produce", today.plusDays(14), added));
        return items;
    }

    @NonNull
    public static List<ShoppingItem> shoppingItems(long now) {
        List<ShoppingItem> items = new ArrayList<>();
        items.add(new ShoppingItem(1, "Bread", "1 loaf", false, null, now - 50_000));
        items.add(new ShoppingItem(2, "Eggs", "12", false, null, now - 40_000));
        items.add(new ShoppingItem(3, "Tomatoes", "4", true, null, now - 30_000));
        items.add(new ShoppingItem(4, "Bananas", "6", true, null, now - 20_000));
        return items;
    }

    /** Recipes saved for offline use when the app first starts. */
    @NonNull
    public static Set<String> savedRecipeIds() {
        return new LinkedHashSet<>(Arrays.asList("curry", "frittata", "cake"));
    }

    @NonNull
    public static List<Recipe> recipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(new Recipe("curry", "Chicken & spinach curry", "Indian", "Chicken",
                R.drawable.art_curry, R.drawable.art_curry_hero,
                Arrays.asList(
                        new RecipeIngredient("Chicken breast", "500 g"),
                        new RecipeIngredient("Spinach", "200 g"),
                        new RecipeIngredient("Greek yogurt", "4 tbsp"),
                        new RecipeIngredient("Garlic", "3 cloves"),
                        new RecipeIngredient("Garam masala", "2 tsp")),
                Arrays.asList(
                        "Cut the chicken breast into bite-sized pieces and crush the garlic.",
                        "Heat a tablespoon of oil in a large pan and brown the chicken for 5–6 minutes.",
                        "Add the garlic and garam masala and cook for 1 minute, stirring all the time.",
                        "Stir in the spinach a handful at a time until it wilts, then simmer for 5 minutes.",
                        "Take the pan off the heat and stir in the Greek yogurt. Season with salt and serve with rice or naan.")));
        recipes.add(new Recipe("pie", "Spinach & chicken pie", "British", "Chicken",
                R.drawable.art_pie, 0,
                Arrays.asList(
                        new RecipeIngredient("Chicken breast", "400 g"),
                        new RecipeIngredient("Spinach", "150 g"),
                        new RecipeIngredient("Milk", "200 mL"),
                        new RecipeIngredient("Butter", "30 g"),
                        new RecipeIngredient("Onion", "1"),
                        new RecipeIngredient("Puff pastry", "1 sheet")),
                Arrays.asList(
                        "Heat the oven to 200 °C. Dice the chicken and the onion.",
                        "Melt the butter in a pan, soften the onion, then brown the chicken for 5 minutes.",
                        "Stir in the spinach until it wilts, add the milk and simmer for 5 minutes until creamy.",
                        "Spoon the filling into a pie dish, cover with the pastry and cut two small slits in the top.",
                        "Bake for 25 minutes until golden.")));
        recipes.add(new Recipe("stirfry", "Chicken stir-fry", "Chinese", "Chicken",
                R.drawable.art_stirfry, 0,
                Arrays.asList(
                        new RecipeIngredient("Chicken breast", "300 g"),
                        new RecipeIngredient("Carrots", "2"),
                        new RecipeIngredient("Red pepper", "1"),
                        new RecipeIngredient("Garlic", "2 cloves"),
                        new RecipeIngredient("Soy sauce", "3 tbsp"),
                        new RecipeIngredient("Rice", "200 g")),
                Arrays.asList(
                        "Cook the rice. Slice the chicken, carrots and pepper into thin strips.",
                        "Heat a little oil in a wok and stir-fry the chicken for 4–5 minutes.",
                        "Add the garlic, carrots and pepper and cook for 3 minutes more.",
                        "Pour in the soy sauce, toss everything together and serve over the rice.")));
        recipes.add(new Recipe("frittata", "Spinach frittata", "Italian", "Vegetarian",
                R.drawable.art_frittata, 0,
                Arrays.asList(
                        new RecipeIngredient("Eggs", "6"),
                        new RecipeIngredient("Spinach", "100 g"),
                        new RecipeIngredient("Milk", "3 tbsp"),
                        new RecipeIngredient("Cheddar cheese", "50 g"),
                        new RecipeIngredient("Cherry tomatoes", "6")),
                Arrays.asList(
                        "Heat the grill. Whisk the eggs with the milk and a pinch of salt.",
                        "Wilt the spinach in an ovenproof pan with a little oil.",
                        "Pour in the eggs, add the halved tomatoes and cook gently for 5 minutes.",
                        "Scatter over the cheese and grill for 3–4 minutes until set and golden.")));
        recipes.add(new Recipe("handi", "Chicken handi", "Indian", "Chicken",
                R.drawable.art_handi, 0,
                Arrays.asList(
                        new RecipeIngredient("Chicken breast", "500 g"),
                        new RecipeIngredient("Greek yogurt", "150 g"),
                        new RecipeIngredient("Onion", "2"),
                        new RecipeIngredient("Tomatoes", "2"),
                        new RecipeIngredient("Garlic", "4 cloves"),
                        new RecipeIngredient("Garam masala", "1 tsp")),
                Arrays.asList(
                        "Fry the sliced onions until golden, then add the garlic.",
                        "Add the chicken and cook until it changes colour.",
                        "Stir in the chopped tomatoes and the garam masala and cook for 10 minutes.",
                        "Lower the heat, stir in the yogurt and simmer for 10 minutes more.")));
        recipes.add(new Recipe("cannelloni", "Spinach & ricotta cannelloni", "Italian", "Vegetarian",
                R.drawable.art_cannelloni, 0,
                Arrays.asList(
                        new RecipeIngredient("Spinach", "250 g"),
                        new RecipeIngredient("Ricotta", "250 g"),
                        new RecipeIngredient("Cannelloni tubes", "12"),
                        new RecipeIngredient("Passata", "500 g"),
                        new RecipeIngredient("Mozzarella", "125 g")),
                Arrays.asList(
                        "Heat the oven to 190 °C. Wilt the spinach, squeeze out the water and chop it.",
                        "Mix the spinach with the ricotta and season.",
                        "Fill the cannelloni tubes and lay them in a baking dish.",
                        "Pour over the passata, tear over the mozzarella and bake for 30 minutes.")));
        recipes.add(new Recipe("cake", "Carrot cake", "British", "Dessert",
                R.drawable.art_cake, 0,
                Arrays.asList(
                        new RecipeIngredient("Carrots", "300 g"),
                        new RecipeIngredient("Eggs", "3"),
                        new RecipeIngredient("Flour", "250 g"),
                        new RecipeIngredient("Brown sugar", "200 g"),
                        new RecipeIngredient("Cream cheese", "200 g"),
                        new RecipeIngredient("Butter", "50 g")),
                Arrays.asList(
                        "Heat the oven to 180 °C and line a cake tin.",
                        "Grate the carrots. Beat the eggs and sugar, then fold in the flour and carrots.",
                        "Bake for 45 minutes and leave to cool.",
                        "Beat the cream cheese with the butter and spread it over the cake.")));
        return recipes;
    }

    /** Ingredient names offered while typing, as TheMealDB's ingredient list would be. */
    @NonNull
    public static List<String> ingredientSuggestions() {
        return Arrays.asList("Bananas", "Bread", "Brown sugar", "Butter", "Carrots", "Cheddar cheese",
                "Cherry tomatoes", "Chicken", "Chicken Breast", "Chicken Thighs", "Chicken Stock",
                "Cream cheese", "Eggs", "Flour", "Garam masala", "Garlic", "Greek yogurt", "Milk",
                "Mozzarella", "Onion", "Passata", "Puff pastry", "Red pepper", "Rice", "Ricotta",
                "Soy sauce", "Spinach", "Tomatoes");
    }
}
