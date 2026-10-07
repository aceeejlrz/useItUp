package com.team15.useitup.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.team15.useitup.util.IngredientMatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory data store for the UI prototype, shared by every screen.
 *
 * <p>The finished app splits this into PantryRepository, RecipeRepository and
 * ShoppingRepository backed by Room DAOs and a Retrofit service. The screens only use the
 * methods and LiveData below, so they will not need to change when that happens. All
 * methods must be called on the main thread. Data resets when the app process ends.
 */
public final class AppRepository {

    private static AppRepository instance;

    private final MutableLiveData<List<PantryItem>> pantry = new MutableLiveData<>();
    private final MutableLiveData<List<ShoppingItem>> shopping = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> savedRecipeIds = new MutableLiveData<>();
    private final List<Recipe> recipes;
    private long nextPantryId;
    private long nextShoppingId;

    private static final Comparator<PantryItem> BY_EXPIRY =
            Comparator.comparing(PantryItem::getExpiryDate)
                    .thenComparing(PantryItem::getName, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<ShoppingItem> BY_CREATED =
            Comparator.comparingLong(ShoppingItem::getCreatedAt);

    private AppRepository() {
        List<PantryItem> items = SampleData.pantryItems(LocalDate.now());
        items.sort(BY_EXPIRY);
        pantry.setValue(Collections.unmodifiableList(items));
        nextPantryId = items.size() + 1;

        List<ShoppingItem> list = SampleData.shoppingItems(System.currentTimeMillis());
        shopping.setValue(Collections.unmodifiableList(list));
        nextShoppingId = list.size() + 1;

        savedRecipeIds.setValue(Collections.unmodifiableSet(SampleData.savedRecipeIds()));
        recipes = Collections.unmodifiableList(SampleData.recipes());
    }

    @MainThread
    @NonNull
    public static AppRepository get() {
        if (instance == null) {
            instance = new AppRepository();
        }
        return instance;
    }

    // ------------------------------------------------------------------ pantry

    /** Pantry items sorted by expiry date, soonest first. */
    @NonNull
    public LiveData<List<PantryItem>> getPantryItems() {
        return pantry;
    }

    @NonNull
    public List<PantryItem> currentPantry() {
        List<PantryItem> value = pantry.getValue();
        return value != null ? value : Collections.emptyList();
    }

    @Nullable
    public PantryItem findPantryItem(long id) {
        for (PantryItem item : currentPantry()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public long addPantryItem(@NonNull String name, double quantity, @NonNull String unit,
                              @NonNull String category, @NonNull LocalDate expiryDate) {
        long id = nextPantryId++;
        List<PantryItem> items = new ArrayList<>(currentPantry());
        items.add(new PantryItem(id, name, quantity, unit, category, expiryDate, LocalDate.now()));
        publishPantry(items);
        return id;
    }

    public void updatePantryItem(@NonNull PantryItem updated) {
        List<PantryItem> items = new ArrayList<>(currentPantry());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == updated.getId()) {
                items.set(i, updated);
                publishPantry(items);
                return;
            }
        }
    }

    /** Removes an item and returns it so the caller can offer Undo. */
    @Nullable
    public PantryItem deletePantryItem(long id) {
        List<PantryItem> items = new ArrayList<>(currentPantry());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) {
                PantryItem removed = items.remove(i);
                publishPantry(items);
                return removed;
            }
        }
        return null;
    }

    public void restorePantryItem(@NonNull PantryItem item) {
        if (findPantryItem(item.getId()) != null) {
            return;
        }
        List<PantryItem> items = new ArrayList<>(currentPantry());
        items.add(item);
        publishPantry(items);
    }

    /** Names of everything in the pantry, used to mark recipe ingredients In pantry / Missing. */
    @NonNull
    public List<String> pantryNames() {
        List<String> names = new ArrayList<>();
        for (PantryItem item : currentPantry()) {
            names.add(item.getName());
        }
        return names;
    }

    private void publishPantry(@NonNull List<PantryItem> items) {
        items.sort(BY_EXPIRY);
        pantry.setValue(Collections.unmodifiableList(items));
    }

    // ----------------------------------------------------------------- recipes

    @NonNull
    public List<Recipe> getRecipes() {
        return recipes;
    }

    @Nullable
    public Recipe findRecipe(@Nullable String id) {
        for (Recipe recipe : recipes) {
            if (recipe.getId().equals(id)) {
                return recipe;
            }
        }
        return null;
    }

    @NonNull
    public LiveData<Set<String>> getSavedRecipeIds() {
        return savedRecipeIds;
    }

    public boolean isSaved(@NonNull String recipeId) {
        Set<String> ids = savedRecipeIds.getValue();
        return ids != null && ids.contains(recipeId);
    }

    public void setSaved(@NonNull String recipeId, boolean saved) {
        Set<String> ids = new LinkedHashSet<>();
        Set<String> current = savedRecipeIds.getValue();
        if (current != null) {
            ids.addAll(current);
        }
        if (saved) {
            ids.add(recipeId);
        } else {
            ids.remove(recipeId);
        }
        savedRecipeIds.setValue(Collections.unmodifiableSet(ids));
    }

    /** Saved recipes, most recently saved first. */
    @NonNull
    public List<Recipe> savedRecipes() {
        List<Recipe> result = new ArrayList<>();
        Set<String> ids = savedRecipeIds.getValue();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            Recipe recipe = findRecipe(id);
            if (recipe != null) {
                result.add(0, recipe);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- shopping

    /** Shopping list in the order items were added. */
    @NonNull
    public LiveData<List<ShoppingItem>> getShoppingItems() {
        return shopping;
    }

    @NonNull
    public List<ShoppingItem> currentShopping() {
        List<ShoppingItem> value = shopping.getValue();
        return value != null ? value : Collections.emptyList();
    }

    public void addShoppingItem(@NonNull String name, @Nullable String quantityNote,
                                @Nullable String fromRecipe) {
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        items.add(new ShoppingItem(nextShoppingId++, name, quantityNote, false, fromRecipe,
                System.currentTimeMillis()));
        publishShopping(items);
    }

    public void setChecked(long id, boolean checked) {
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) {
                items.set(i, items.get(i).withChecked(checked));
                publishShopping(items);
                return;
            }
        }
    }

    @Nullable
    public ShoppingItem deleteShoppingItem(long id) {
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) {
                ShoppingItem removed = items.remove(i);
                publishShopping(items);
                return removed;
            }
        }
        return null;
    }

    public void restoreShoppingItem(@NonNull ShoppingItem item) {
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        for (ShoppingItem existing : items) {
            if (existing.getId() == item.getId()) {
                return;
            }
        }
        items.add(item);
        publishShopping(items);
    }

    /** Removes every checked item and returns how many were removed. */
    public int clearChecked() {
        List<ShoppingItem> items = new ArrayList<>();
        int removed = 0;
        for (ShoppingItem item : currentShopping()) {
            if (item.isChecked()) {
                removed++;
            } else {
                items.add(item);
            }
        }
        if (removed > 0) {
            publishShopping(items);
        }
        return removed;
    }

    /** True if an unchecked item with this name is already on the list. */
    public boolean isOnShoppingList(@NonNull String name) {
        for (ShoppingItem item : currentShopping()) {
            if (!item.isChecked() && IngredientMatcher.matches(item.getName(), name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the recipe's ingredients that are neither in the pantry nor already on the list.
     *
     * @return how many items were added
     */
    public int addMissingToShoppingList(@NonNull Recipe recipe) {
        List<RecipeIngredient> toAdd = missingNotOnList(recipe);
        if (toAdd.isEmpty()) {
            return 0;
        }
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        long now = System.currentTimeMillis();
        for (RecipeIngredient ingredient : toAdd) {
            items.add(new ShoppingItem(nextShoppingId++, ingredient.getName(), ingredient.getMeasure(),
                    false, recipe.getName(), now++));
        }
        publishShopping(items);
        return toAdd.size();
    }

    /** Ingredients of the recipe that are not in the pantry. */
    @NonNull
    public List<RecipeIngredient> missingIngredients(@NonNull Recipe recipe) {
        List<String> names = pantryNames();
        List<RecipeIngredient> missing = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (!IngredientMatcher.containsMatch(names, ingredient.getName())) {
                missing.add(ingredient);
            }
        }
        return missing;
    }

    /** Missing ingredients that still need to go on the shopping list. */
    @NonNull
    public List<RecipeIngredient> missingNotOnList(@NonNull Recipe recipe) {
        List<RecipeIngredient> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RecipeIngredient ingredient : missingIngredients(recipe)) {
            String key = IngredientMatcher.normalize(ingredient.getName());
            if (seen.add(key) && !isOnShoppingList(ingredient.getName())) {
                result.add(ingredient);
            }
        }
        return result;
    }

    private void publishShopping(@NonNull List<ShoppingItem> items) {
        items.sort(BY_CREATED);
        shopping.setValue(Collections.unmodifiableList(items));
    }
}
