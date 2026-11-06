package com.team15.useitup.data;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.team15.useitup.data.local.UseItUpDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/** Real Room/SQLite on a private test file; never clears the application's database. */
@RunWith(AndroidJUnit4.class)
public class RoomPersistenceTest {
    private static final String TEST_DB = "room-persistence-test.db";
    private Context context;
    private UseItUpDatabase database;
    private AppRepository repository;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB);
        open();
    }

    private void open() {
        database = Room.databaseBuilder(context, UseItUpDatabase.class, TEST_DB).build();
        repository = new AppRepository(database);
        repository.awaitIdle();
    }

    private void reopen() {
        repository.closeForTest();
        open();
    }

    @After public void tearDown() {
        repository.closeForTest();
        context.deleteDatabase(TEST_DB);
    }

    @Test public void seedsOnlyOnceAndEmptyListsRemainEmpty() {
        assertEquals(6, repository.currentPantry().size());
        assertEquals(4, repository.currentShopping().size());
        assertEquals(3, repository.savedRecipes().size());
        for (PantryItem item : repository.currentPantry()) repository.deletePantryItem(item.getId());
        for (ShoppingItem item : repository.currentShopping()) repository.deleteShoppingItem(item.getId());
        for (Recipe recipe : repository.savedRecipes()) repository.setSaved(recipe.getId(), false);
        reopen();
        assertTrue(repository.currentPantry().isEmpty());
        assertTrue(repository.currentShopping().isEmpty());
        assertTrue(repository.savedRecipes().isEmpty());
        assertEquals(7, repository.getRecipes().size()); // search samples were never removed
        assertTrue(database.seedDao().isSeeded());
    }

    @Test public void pantryUpdateDeleteUndoRestoresEveryFieldAfterReopen() {
        long id = repository.addPantryItem("Room pears", 2.5, "kg", "Produce", LocalDate.of(2030, 1, 2));
        PantryItem original = new PantryItem(id, "Updated pears", 3.25, "bags", "Other",
                LocalDate.of(2031, 2, 3), LocalDate.of(2020, 4, 5));
        repository.updatePantryItem(original);
        PantryItem removed = repository.deletePantryItem(id);
        assertEquals(original, removed);
        long newerId = repository.addPantryItem("Other item", 1, "g", "Other", LocalDate.now());
        assertTrue(newerId > id);
        repository.restorePantryItem(removed);
        repository.restorePantryItem(removed); // repeated Undo is idempotent
        reopen();
        assertEquals(original, repository.findPantryItem(id));
        assertTrue(repository.pantryNames().contains("Updated pears"));
        assertEquals(8, repository.currentPantry().size());
        assertTrue(repository.addPantryItem("Next", 1, "g", "Other", LocalDate.now()) > newerId);
    }

    @Test public void shoppingCheckUndoAndClearPreserveOriginalIdentityAndMetadata() {
        ShoppingItem original = new ShoppingItem(500, "Room beans", "2 cans", true,
                "Remote meal", 123456789L);
        repository.restoreShoppingItem(original);
        ShoppingItem removed = repository.deleteShoppingItem(original.getId());
        repository.addShoppingItem("Unrelated", null, null);
        repository.restoreShoppingItem(removed);
        repository.restoreShoppingItem(removed);
        reopen();
        assertTrue(repository.currentShopping().contains(original));
        repository.setChecked(500, false);
        reopen();
        assertTrue(repository.currentShopping().contains(original.withChecked(false)));
        repository.setChecked(500, true);
        assertEquals(3, repository.clearChecked()); // two seeded checked rows plus ours
        reopen();
        assertFalse(repository.isOnShoppingList("Room beans"));
        assertEquals(3, repository.currentShopping().size());
    }

    static Recipe remoteRecipe(String id) {
        return new Recipe(id, "Cem's soup — çorba", "Turkish", "Vegetarian",
                "https://www.themealdb.com/images/media/meals/test.jpg",
                Arrays.asList(new RecipeIngredient("Salt", "½ tsp"),
                        new RecipeIngredient("Water", "2 cups"),
                        new RecipeIngredient("Salt", "to taste")),
                Arrays.asList("Mix \"gently\".\nKeep warm.", "Serve — enjoy!"));
    }

    static void assertRecipe(Recipe expected, Recipe actual) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getArea(), actual.getArea());
        assertEquals(expected.getCategory(), actual.getCategory());
        assertEquals(expected.getImageUrl(), actual.getImageUrl());
        assertEquals(expected.getSteps(), actual.getSteps());
        assertEquals(expected.getIngredients().size(), actual.getIngredients().size());
        for (int i = 0; i < expected.getIngredients().size(); i++) {
            assertEquals(expected.getIngredients().get(i).getName(), actual.getIngredients().get(i).getName());
            assertEquals(expected.getIngredients().get(i).getMeasure(), actual.getIngredients().get(i).getMeasure());
        }
    }

    @Test public void fullRemoteRecipeSurvivesWithoutNetworkCacheAndKeepsOrder() {
        Recipe meal = remoteRecipe("mealdb-12345");
        repository.saveRecipe(meal);
        repository.saveRecipe(remoteRecipe("mealdb-67890"));
        reopen();
        assertTrue(repository.isSaved(meal.getId()));
        assertRecipe(meal, repository.findRecipe(meal.getId()));
        assertEquals("mealdb-67890", repository.savedRecipes().get(0).getId());
        assertEquals(meal.getId(), repository.savedRecipes().get(1).getId());
        assertEquals(0, repository.findRecipe(meal.getId()).getImageRes());
        assertTrue(repository.findRecipe("curry").getImageRes() != 0);
        // Schema must not store build-specific drawable IDs.
        try (Cursor columns = database.getOpenHelper().getReadableDatabase().query("PRAGMA table_info(SavedRecipe)")) {
            while (columns.moveToNext()) {
                String name = columns.getString(columns.getColumnIndexOrThrow("name"));
                assertFalse(name.equals("imageRes") || name.equals("heroRes"));
            }
        }
    }

    @Test public void resavingReplacesIngredientsAndUnsaveCascades() {
        Recipe meal = remoteRecipe("mealdb-replace");
        repository.saveRecipe(meal);
        Recipe updated = new Recipe(meal.getId(), "Updated", "Area", "Category", (String) null,
                Collections.singletonList(new RecipeIngredient("Rice", "1 cup")),
                Collections.singletonList("Cook."));
        repository.saveRecipe(updated);
        reopen();
        assertRecipe(updated, repository.findRecipe(meal.getId()));
        repository.setSaved(meal.getId(), false);
        reopen();
        assertFalse(repository.isSaved(meal.getId()));
        assertNull(repository.findRecipe(meal.getId()));
        try (Cursor rows = database.getOpenHelper().getReadableDatabase().query(
                "SELECT COUNT(*) FROM RecipeIngredient WHERE recipeId = ?", new Object[] {meal.getId()})) {
            assertTrue(rows.moveToFirst());
            assertEquals(0, rows.getInt(0));
        }
    }

    @Test public void rejectsIncompleteSearchSummaryWithoutSavingIt() {
        Recipe summary = new Recipe("summary-only", "Summary", "", "", "https://example.com/photo.jpg",
                Collections.emptyList(), Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> repository.saveRecipe(summary));
        reopen();
        assertFalse(repository.isSaved(summary.getId()));
    }

    @Test public void cacheIsImmediateBeforeLiveDataDeliveryAndWritesRemainOrdered() {
        AtomicReference<PantryItem> expected = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            long id = repository.addPantryItem("Immediate", 1, "kg", "Produce", LocalDate.now());
            PantryItem item = repository.findPantryItem(id);
            assertNotNull(item);
            assertTrue(repository.pantryNames().contains("Immediate"));
            repository.deletePantryItem(id);
            assertNull(repository.findPantryItem(id));
            repository.restorePantryItem(item);
            repository.saveRecipe(remoteRecipe("immediate-recipe"));
            assertTrue(repository.isSaved("immediate-recipe"));
            assertEquals("immediate-recipe", repository.savedRecipes().get(0).getId());
            repository.addShoppingItem("Immediate shopping", null, null);
            assertTrue(repository.isOnShoppingList("Immediate shopping"));
            expected.set(item);
        });
        reopen();
        assertEquals(expected.get(), repository.findPantryItem(expected.get().getId()));
        assertTrue(repository.isSaved("immediate-recipe"));
        assertTrue(repository.isOnShoppingList("Immediate shopping"));
    }

    @Test public void liveDataPublishesLoadedAndUpdatedCache() {
        AtomicReference<List<PantryItem>> observed = new AtomicReference<>();
        Observer<List<PantryItem>> observer = observed::set;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                repository.getPantryItems().observeForever(observer));
        try {
            repository.addPantryItem("Observed", 1, "g", "Other", LocalDate.now());
            repository.awaitIdle();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(repository.currentPantry(), observed.get());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    repository.getPantryItems().removeObserver(observer));
        }
    }

    @Test public void parallelMutationsCannotLoseItemsOrReuseIds() throws Exception {
        ExecutorService callers = Executors.newFixedThreadPool(4);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                final int index = i;
                futures.add(callers.submit(() -> repository.addPantryItem("Parallel " + index,
                        1, "g", "Other", LocalDate.now())));
            }
            Set<Long> ids = new HashSet<>();
            for (Future<Long> future : futures) assertTrue(ids.add(future.get()));
            reopen();
            assertEquals(26, repository.currentPantry().size());
            for (Long id : ids) assertNotNull(repository.findPantryItem(id));
        } finally {
            callers.shutdown();
        }
    }

    @Test public void failedSnapshotTransactionLeavesOriginalRowsIntact() {
        List<PantryItem> original = database.pantryDao().getAll();
        assertThrows(RuntimeException.class, () -> database.pantryDao().replaceAll(
                Arrays.asList(original.get(0), original.get(0))));
        assertEquals(original, database.pantryDao().getAll());
    }

    @Test public void roomRejectsQueriesOnMainThread() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                assertThrows(IllegalStateException.class, () -> database.pantryDao().getAll()));
    }
}
