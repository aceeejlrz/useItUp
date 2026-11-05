package com.team15.useitup.ui.recipes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.data.Recipe;
import com.team15.useitup.data.RecipeIngredient;
import com.team15.useitup.data.RecipeRepository;
import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.IngredientMatcher;
import com.team15.useitup.util.Network;
import com.team15.useitup.util.NotificationHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recipes screen state. Shared with MainActivity so the Pantry's "Find recipes" can fill in
 * the search. Searches TheMealDB through {@link RecipeRepository}; if a request fails the
 * sample recipes are searched instead so the screen still shows something useful.
 */
public class RecipesViewModel extends AndroidViewModel {

    public enum Tab { SEARCH, SAVED }

    public enum Mode { PROMPT, LOADING, RESULTS, EMPTY, OFFLINE }

    /** A recipe card: how many of the searched ingredients it uses. */
    public static final class Card {
        @NonNull public final Recipe recipe;
        public final int matchCount;
        public final int ingredientCount;
        public final boolean savedTab;

        Card(@NonNull Recipe recipe, int matchCount, int ingredientCount, boolean savedTab) {
            this.recipe = recipe;
            this.matchCount = matchCount;
            this.ingredientCount = ingredientCount;
            this.savedTab = savedTab;
        }

        public boolean isFullMatch() {
            return matchCount == ingredientCount;
        }
    }

    public static final class State {
        @NonNull public final Tab tab;
        @NonNull public final Mode mode;
        @NonNull public final List<String> ingredients;
        @NonNull public final List<String> suggestions;
        @NonNull public final List<Card> cards;

        State(@NonNull Tab tab, @NonNull Mode mode, @NonNull List<String> ingredients,
              @NonNull List<String> suggestions, @NonNull List<Card> cards) {
            this.tab = tab;
            this.mode = mode;
            this.ingredients = ingredients;
            this.suggestions = suggestions;
            this.cards = cards;
        }
    }

    private final AppRepository repository = AppRepository.get();
    private final RecipeRepository recipeRepository = RecipeRepository.get();
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final List<String> ingredients = new ArrayList<>();
    @NonNull private Tab tab = Tab.SEARCH;
    @NonNull private Mode mode = Mode.PROMPT;
    @NonNull private List<Card> results = Collections.emptyList();

    public RecipesViewModel(@NonNull Application application) {
        super(application);
        state.addSource(repository.getPantryItems(), items -> publish());
        state.addSource(repository.getSavedRecipeIds(), ids -> publish());
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /** Replaces the search with these ingredients (from the Pantry's Find recipes). */
    public void searchFor(@NonNull List<String> names) {
        ingredients.clear();
        for (String name : names) {
            addUnique(name);
        }
        tab = Tab.SEARCH;
        startSearch();
    }

    public void addIngredient(@NonNull String name) {
        if (addUnique(name)) {
            startSearch();
        }
    }

    public void removeIngredient(@NonNull String name) {
        if (ingredients.remove(name)) {
            startSearch();
        }
    }

    public void clearIngredients() {
        ingredients.clear();
        startSearch();
    }

    public void retry() {
        startSearch();
    }

    public void setTab(@NonNull Tab value) {
        if (tab != value) {
            tab = value;
            publish();
        }
    }

    /** Re-checks the pantry and alert window (the expiring-soon suggestions depend on them). */
    public void refresh() {
        publish();
    }

    private boolean addUnique(@NonNull String raw) {
        String name = raw.trim();
        if (name.isEmpty() || IngredientMatcher.containsMatch(ingredients, name)) {
            return false;
        }
        ingredients.add(name);
        return true;
    }

    private void startSearch() {
        recipeRepository.cancelSearch();
        if (ingredients.isEmpty()) {
            mode = Mode.PROMPT;
            results = Collections.emptyList();
        } else if (!Network.isOnline(getApplication())) {
            mode = Mode.OFFLINE;
            results = Collections.emptyList();
        } else {
            mode = Mode.LOADING;
            List<String> searched = new ArrayList<>(ingredients);
            recipeRepository.search(searched, new RecipeRepository.SearchCallback() {
                @Override
                public void onResult(@NonNull List<RecipeRepository.Match> matches) {
                    List<Card> cards = new ArrayList<>();
                    for (RecipeRepository.Match match : matches) {
                        cards.add(new Card(match.recipe, match.matchCount, searched.size(), false));
                    }
                    finishSearch(cards);
                }

                @Override
                public void onError() {
                    finishSearch(matchSamples(searched));
                }
            });
        }
        publish();
    }

    private void finishSearch(@NonNull List<Card> cards) {
        results = cards;
        mode = results.isEmpty() ? Mode.EMPTY : Mode.RESULTS;
        publish();
    }

    /** Sample recipes that use at least one searched ingredient, best matches first. */
    @NonNull
    private List<Card> matchSamples(@NonNull List<String> searched) {
        List<Card> cards = new ArrayList<>();
        for (Recipe recipe : repository.getRecipes()) {
            List<String> names = new ArrayList<>();
            for (RecipeIngredient ingredient : recipe.getIngredients()) {
                names.add(ingredient.getName());
            }
            int count = 0;
            for (String wanted : searched) {
                if (IngredientMatcher.containsMatch(names, wanted)) {
                    count++;
                }
            }
            if (count > 0) {
                cards.add(new Card(recipe, count, searched.size(), false));
            }
        }
        // Stable sort keeps the original order between equal matches.
        cards.sort((a, b) -> Integer.compare(b.matchCount, a.matchCount));
        return cards;
    }

    private void publish() {
        int alertDays = AppPrefs.alertDays(getApplication());
        List<String> suggestions = new ArrayList<>();
        for (PantryItem item : NotificationHelper.expiringSoon(repository.currentPantry(), alertDays,
                LocalDate.now())) {
            if (!IngredientMatcher.containsMatch(ingredients, item.getName())) {
                suggestions.add(item.getName());
            }
        }

        List<Card> cards;
        if (tab == Tab.SAVED) {
            cards = new ArrayList<>();
            for (Recipe recipe : repository.savedRecipes()) {
                cards.add(new Card(recipe, 0, 0, true));
            }
        } else {
            cards = results;
        }
        state.setValue(new State(tab, mode, Collections.unmodifiableList(new ArrayList<>(ingredients)),
                Collections.unmodifiableList(suggestions), Collections.unmodifiableList(cards)));
    }

    @Override
    protected void onCleared() {
        recipeRepository.cancelSearch();
    }
}
