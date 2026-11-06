package com.team15.useitup.data;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.WorkerThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.team15.useitup.util.IngredientMatcher;
import com.team15.useitup.data.local.SavedRecipeWithIngredients;
import com.team15.useitup.data.local.UseItUpDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Room-backed store shared by screens and background workers. A single executor loads
 * the initial cache and serializes writes. Synchronous reads never query Room or rely
 * on LiveData delivery. get() waits for initial loading, including on a cold worker start.
 * Mutations update the cache immediately; awaitIdle() is the background durability barrier.
 */
public final class AppRepository {

    private static volatile AppRepository instance;

    private final MutableLiveData<List<PantryItem>> pantry = new MutableLiveData<>();
    private final MutableLiveData<List<ShoppingItem>> shopping = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> savedRecipeIds = new MutableLiveData<>();
    private final MutableLiveData<Throwable> storageErrors = new MutableLiveData<>();
    private final UseItUpDatabase database;
    private final ExecutorService disk = Executors.newSingleThreadExecutor();
    private final Future<?> loaded;
    private volatile RuntimeException writeFailure;
    private List<PantryItem> pantryCache = Collections.emptyList();
    private List<ShoppingItem> shoppingCache = Collections.emptyList();
    private final Map<String, Recipe> savedCache = new LinkedHashMap<>();
    private final List<Recipe> recipes;
    private long nextPantryId;
    private long nextShoppingId;

    private static final Comparator<PantryItem> BY_EXPIRY =
            Comparator.comparing(PantryItem::getExpiryDate)
                    .thenComparing(PantryItem::getName, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<ShoppingItem> BY_CREATED =
            Comparator.comparingLong(ShoppingItem::getCreatedAt);

    // Package-private for tests using their own on-disk database, never the user's data.
    AppRepository(UseItUpDatabase database) {
        this.database = database;
        recipes = Collections.unmodifiableList(SampleData.recipes());
        loaded = disk.submit(() -> {
            database.seedOnce();
            synchronized (this) {
                pantryCache = Collections.unmodifiableList(database.pantryDao().getAll());
                shoppingCache = Collections.unmodifiableList(database.shoppingDao().getAll());
                nextPantryId = 1;
                for (PantryItem item : pantryCache) {
                    nextPantryId = Math.max(nextPantryId, item.getId() + 1);
                }
                nextShoppingId = 1;
                for (ShoppingItem item : shoppingCache) {
                    nextShoppingId = Math.max(nextShoppingId, item.getId() + 1);
                }
                for (SavedRecipeWithIngredients saved : database.savedRecipeDao().getAll()) {
                    Recipe recipe = saved.toRecipe();
                    savedCache.put(recipe.getId(), recipe);
                }
                pantry.postValue(pantryCache);
                shopping.postValue(shoppingCache);
                publishSavedIds();
            }
        });
    }

    /** Called once by Application, before activities or WorkManager jobs use the store. */
    public static synchronized void initialize(@NonNull Context context) {
        if (instance == null) {
            instance = new AppRepository(UseItUpDatabase.open(context));
        }
    }

    @NonNull
    public static AppRepository get() {
        AppRepository repository = instance;
        if (repository == null) {
            throw new IllegalStateException("Initialize AppRepository from UseItUpApp first");
        }
        // No main-thread database access: startup work runs entirely on disk's executor.
        // Waiting here preserves the existing synchronous API without returning false empties.
        await(repository.loaded);
        return repository;
    }

    private static void await(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for storage", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not load storage", e.getCause());
        }
    }

    /** Wait for writes submitted before this call. Workers/tests only; never the UI thread. */
    @WorkerThread
    public void awaitIdle() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("awaitIdle must run off the main thread");
        }
        await(loaded);
        await(disk.submit(() -> { }));
        if (writeFailure != null) throw writeFailure;
    }

    @NonNull
    public LiveData<Throwable> getStorageErrors() {
        return storageErrors;
    }

    private void persist(Runnable write) {
        disk.execute(() -> {
            try {
                write.run();
            } catch (RuntimeException e) {
                writeFailure = e;
                Log.e("AppRepository", "Could not save changes", e);
                storageErrors.postValue(e);
            }
        });
    }

    @WorkerThread
    void closeForTest() {
        try {
            awaitIdle();
        } finally {
            disk.shutdown();
            database.close();
        }
    }

    // ------------------------------------------------------------------ pantry

    /** Pantry items sorted by expiry date, soonest first. */
    @NonNull
    public LiveData<List<PantryItem>> getPantryItems() {
        return pantry;
    }

    @NonNull
    public synchronized List<PantryItem> currentPantry() {
        return pantryCache;
    }

    @Nullable
    public synchronized PantryItem findPantryItem(long id) {
        for (PantryItem item : currentPantry()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public synchronized long addPantryItem(@NonNull String name, double quantity, @NonNull String unit,
                              @NonNull String category, @NonNull LocalDate expiryDate) {
        long id = nextPantryId++;
        List<PantryItem> items = new ArrayList<>(currentPantry());
        items.add(new PantryItem(id, name, quantity, unit, category, expiryDate, LocalDate.now()));
        publishPantry(items);
        return id;
    }

    public synchronized void updatePantryItem(@NonNull PantryItem updated) {
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
    public synchronized PantryItem deletePantryItem(long id) {
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

    public synchronized void restorePantryItem(@NonNull PantryItem item) {
        if (findPantryItem(item.getId()) != null) {
            return;
        }
        List<PantryItem> items = new ArrayList<>(currentPantry());
        items.add(item);
        nextPantryId = Math.max(nextPantryId, item.getId() + 1);
        publishPantry(items);
    }

    /** Names of everything in the pantry, used to mark recipe ingredients In pantry / Missing. */
    @NonNull
    public synchronized List<String> pantryNames() {
        List<String> names = new ArrayList<>();
        for (PantryItem item : currentPantry()) {
            names.add(item.getName());
        }
        return names;
    }

    private void publishPantry(@NonNull List<PantryItem> items) {
        items.sort(BY_EXPIRY);
        List<PantryItem> snapshot = Collections.unmodifiableList(new ArrayList<>(items));
        pantryCache = snapshot;
        persist(() -> database.pantryDao().replaceAll(snapshot));
        pantry.postValue(snapshot);
    }

    // ----------------------------------------------------------------- recipes

    @NonNull
    public List<Recipe> getRecipes() {
        return recipes;
    }

    /** Saved full recipe first, then a sample or a TheMealDB result from this session. */
    @Nullable
    public synchronized Recipe findRecipe(@Nullable String id) {
        Recipe saved = savedCache.get(id);
        if (saved != null) return saved;
        for (Recipe recipe : recipes) {
            if (recipe.getId().equals(id)) {
                return recipe;
            }
        }
        // The teammate's network cache is main-thread-only. Saved recipes are safe on workers.
        return Looper.myLooper() == Looper.getMainLooper() ? RecipeRepository.get().find(id) : null;
    }

    @NonNull
    public LiveData<Set<String>> getSavedRecipeIds() {
        return savedRecipeIds;
    }

    public synchronized boolean isSaved(@NonNull String recipeId) {
        return savedCache.containsKey(recipeId);
    }

    public synchronized void setSaved(@NonNull String recipeId, boolean saved) {
        if (saved) {
            Recipe recipe = findRecipe(recipeId);
            if (recipe == null) throw new IllegalArgumentException("Unknown recipe: " + recipeId);
            saveRecipe(recipe);
        } else if (savedCache.remove(recipeId) != null) {
            persistSaved();
        }
    }

    /** Save a full lookup.php meal directly, including its URL, steps and ingredient measures. */
    public synchronized void saveRecipe(@NonNull Recipe recipe) {
        if (recipe.getIngredients().isEmpty() || recipe.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Load the full recipe before saving");
        }
        savedCache.put(recipe.getId(), recipe);
        persistSaved();
    }

    private void persistSaved() {
        List<Recipe> snapshot = new ArrayList<>(savedCache.values());
        persist(() -> database.savedRecipeDao().replaceAll(snapshot));
        publishSavedIds();
    }

    private void publishSavedIds() {
        savedRecipeIds.postValue(Collections.unmodifiableSet(new LinkedHashSet<>(savedCache.keySet())));
    }

    /** Saved recipes, most recently saved first. */
    @NonNull
    public synchronized List<Recipe> savedRecipes() {
        List<Recipe> result = new ArrayList<>(savedCache.values());
        Collections.reverse(result);
        return result;
    }

    // ---------------------------------------------------------------- shopping

    /** Shopping list in the order items were added. */
    @NonNull
    public LiveData<List<ShoppingItem>> getShoppingItems() {
        return shopping;
    }

    @NonNull
    public synchronized List<ShoppingItem> currentShopping() {
        return shoppingCache;
    }

    public synchronized void addShoppingItem(@NonNull String name, @Nullable String quantityNote,
                                @Nullable String fromRecipe) {
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        items.add(new ShoppingItem(nextShoppingId++, name, quantityNote, false, fromRecipe,
                System.currentTimeMillis()));
        publishShopping(items);
    }

    public synchronized void setChecked(long id, boolean checked) {
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
    public synchronized ShoppingItem deleteShoppingItem(long id) {
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

    public synchronized void restoreShoppingItem(@NonNull ShoppingItem item) {
        List<ShoppingItem> items = new ArrayList<>(currentShopping());
        for (ShoppingItem existing : items) {
            if (existing.getId() == item.getId()) {
                return;
            }
        }
        items.add(item);
        nextShoppingId = Math.max(nextShoppingId, item.getId() + 1);
        publishShopping(items);
    }

    /** Removes every checked item and returns how many were removed. */
    public synchronized int clearChecked() {
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
    public synchronized boolean isOnShoppingList(@NonNull String name) {
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
    public synchronized int addMissingToShoppingList(@NonNull Recipe recipe) {
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
    public synchronized List<RecipeIngredient> missingIngredients(@NonNull Recipe recipe) {
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
    public synchronized List<RecipeIngredient> missingNotOnList(@NonNull Recipe recipe) {
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
        List<ShoppingItem> snapshot = Collections.unmodifiableList(new ArrayList<>(items));
        shoppingCache = snapshot;
        persist(() -> database.shoppingDao().replaceAll(snapshot));
        shopping.postValue(snapshot);
    }
}
