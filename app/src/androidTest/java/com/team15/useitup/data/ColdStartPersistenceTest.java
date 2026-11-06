package com.team15.useitup.data;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;
import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.NotificationHelper;
import com.team15.useitup.worker.ExpiryCheckWorker;
import com.team15.useitup.R;
import com.team15.useitup.ui.detail.RecipeDetailActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional three-phase host test against the real app database. Run seed, force-stop,
 * then verify in a fresh instrumentation process without opening an Activity; cleanup
 * removes only these fixtures. The default suite just checks Application initialization.
 */
@RunWith(AndroidJUnit4.class)
public class ColdStartPersistenceTest {
    private static final long PANTRY_ID = 900001;
    private static final long SHOPPING_ID = 900002;
    private static final String RECIPE_ID = "room-cold-start-fixture";

    @Test public void applicationDatabaseSurvivesProcessDeath() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppRepository repository = AppRepository.get();
        repository.awaitIdle();
        String phase = InstrumentationRegistry.getArguments().getString("persistencePhase", "default");
        PantryItem pantry = new PantryItem(PANTRY_ID, "Room cold-start pears", 2.75, "kg", "Produce",
                LocalDate.now().plusDays(1), LocalDate.of(2020, 4, 5));
        ShoppingItem shopping = new ShoppingItem(SHOPPING_ID, "Room cold-start rice", "2 bags",
                true, "Cem's soup", 123456789L);
        Recipe recipe = RoomPersistenceTest.remoteRecipe(RECIPE_ID);

        switch (phase) {
            case "seed":
                assertNull("Fixture already exists; run cleanup first", repository.findPantryItem(PANTRY_ID));
                assertFalse(repository.isSaved(RECIPE_ID));
                assertFalse(repository.currentShopping().stream().anyMatch(item -> item.getId() == SHOPPING_ID));
                repository.restorePantryItem(pantry);
                repository.restoreShoppingItem(shopping);
                repository.saveRecipe(recipe);
                repository.awaitIdle();
                break;
            case "verify":
                assertEquals(pantry, repository.findPantryItem(PANTRY_ID));
                assertTrue(repository.currentShopping().contains(shopping));
                assertTrue(repository.isSaved(RECIPE_ID));
                RoomPersistenceTest.assertRecipe(recipe, repository.findRecipe(RECIPE_ID));
                assertTrue(repository.savedRecipes().stream().anyMatch(r -> r.getId().equals(RECIPE_ID)));
                verifyWorkerReadsPersistedPantry(context);
                // The worker assertion above runs before any screen is opened. Then verify
                // that Recipe Detail can show a saved remote meal without the network cache.
                try (ActivityScenario<RecipeDetailActivity> screen = ActivityScenario.launch(
                        RecipeDetailActivity.intent(context, RECIPE_ID))) {
                    onView(withId(R.id.title)).check(matches(withText(recipe.getName())));
                    onView(withId(R.id.meta)).check(matches(withText("Turkish · Vegetarian")));
                }
                break;
            case "cleanup":
                repository.deletePantryItem(PANTRY_ID);
                repository.deleteShoppingItem(SHOPPING_ID);
                repository.setSaved(RECIPE_ID, false);
                repository.awaitIdle();
                break;
            case "default":
                assertNotNull(repository.currentPantry());
                assertNotNull(repository.currentShopping());
                assertNotNull(repository.savedRecipes());
                break;
            default:
                fail("Unknown persistencePhase: " + phase);
        }
    }

    private void verifyWorkerReadsPersistedPantry(Context context) throws Exception {
        assertTrue("Grant POST_NOTIFICATIONS before the verify phase", NotificationHelper.canPost(context));
        boolean reminders = AppPrefs.remindersEnabled(context);
        ExecutorService workerThread = Executors.newSingleThreadExecutor();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        try {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(AppPrefs.KEY_REMINDERS, true).commit();
            ExpiryCheckWorker worker = TestWorkerBuilder.from(context, ExpiryCheckWorker.class, workerThread).build();
            assertEquals(ListenableWorker.Result.success(), workerThread.submit(worker::doWork).get());
            boolean found = false;
            String observed = "none";
            // notify() enqueues work in system_server; success does not mean it is visible yet.
            long deadline = SystemClock.elapsedRealtime() + 5000;
            while (!found && SystemClock.elapsedRealtime() < deadline) {
                for (StatusBarNotification notification : manager.getActiveNotifications()) {
                    CharSequence text = notification.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
                    observed = notification.getPackageName() + ": " + notification.getNotification().extras;
                    // TextLists formats later ingredient names in sentence case.
                    if (text != null && text.toString().toLowerCase(Locale.ROOT)
                            .contains("room cold-start pears")) found = true;
                }
                if (!found) Thread.sleep(50);
            }
            assertTrue("The cold-start worker notification must include the persisted pantry item; observed "
                    + observed, found);
        } finally {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(AppPrefs.KEY_REMINDERS, reminders).commit();
            manager.cancel(1001);
            workerThread.shutdown();
        }
    }
}
