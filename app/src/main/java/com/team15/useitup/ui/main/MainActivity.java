package com.team15.useitup.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.team15.useitup.R;
import com.google.android.material.snackbar.Snackbar;
import com.team15.useitup.data.AppRepository;
import com.team15.useitup.databinding.ActivityMainBinding;
import com.team15.useitup.ui.pantry.PantryFragment;
import com.team15.useitup.ui.recipes.RecipesFragment;
import com.team15.useitup.ui.recipes.RecipesViewModel;
import com.team15.useitup.ui.shopping.ShoppingFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Hosts the three main screens (Pantry, Recipes, Shopping list) as fragments and switches
 * between them with the bottom navigation bar. Secondary screens are separate activities.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TAB = "com.team15.useitup.extra.TAB";
    public static final String EXTRA_INGREDIENTS = "com.team15.useitup.extra.INGREDIENTS";
    public static final String TAB_PANTRY = "pantry";
    public static final String TAB_RECIPES = "recipes";
    public static final String TAB_SHOPPING = "shopping";

    private ActivityMainBinding binding;
    private RecipesViewModel recipesViewModel;

    /** Opens MainActivity on a tab, reusing the existing instance if there is one. */
    public static Intent intentForTab(@NonNull Context context, @NonNull String tab) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_TAB, tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppRepository.get().getStorageErrors().observe(this, error ->
                Snackbar.make(binding.getRoot(), R.string.storage_save_failed,
                        Snackbar.LENGTH_INDEFINITE).show());
        recipesViewModel = new ViewModelProvider(this).get(RecipesViewModel.class);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new PantryFragment(), TAB_PANTRY)
                    .add(R.id.fragment_container, new RecipesFragment(), TAB_RECIPES)
                    .add(R.id.fragment_container, new ShoppingFragment(), TAB_SHOPPING)
                    .commitNow();
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        binding.bottomNav.setOnItemReselectedListener(item -> { /* Already showing. */ });

        if (savedInstanceState == null) {
            showTab(R.id.nav_pantry);
            handleIntent(getIntent());
        } else {
            showTab(binding.bottomNav.getSelectedItemId());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** Switches to the Recipes tab with these ingredients already filled in. */
    public void openRecipesFor(@NonNull List<String> ingredients) {
        recipesViewModel.searchFor(ingredients);
        binding.bottomNav.setSelectedItemId(R.id.nav_recipes);
    }

    /** Switches to the Saved tab of Recipes (used by the offline state). */
    public void selectTab(@NonNull String tab) {
        binding.bottomNav.setSelectedItemId(menuIdFor(tab));
    }

    private void handleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        ArrayList<String> ingredients = intent.getStringArrayListExtra(EXTRA_INGREDIENTS);
        if (ingredients != null && !ingredients.isEmpty()) {
            openRecipesFor(ingredients);
            return;
        }
        String tab = intent.getStringExtra(EXTRA_TAB);
        if (tab != null) {
            binding.bottomNav.setSelectedItemId(menuIdFor(tab));
        }
    }

    @IdRes
    private static int menuIdFor(@NonNull String tab) {
        switch (tab) {
            case TAB_RECIPES:
                return R.id.nav_recipes;
            case TAB_SHOPPING:
                return R.id.nav_shopping;
            default:
                return R.id.nav_pantry;
        }
    }

    private void showTab(@IdRes int itemId) {
        String visibleTag = TAB_PANTRY;
        if (itemId == R.id.nav_recipes) {
            visibleTag = TAB_RECIPES;
        } else if (itemId == R.id.nav_shopping) {
            visibleTag = TAB_SHOPPING;
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        for (String tag : new String[] {TAB_PANTRY, TAB_RECIPES, TAB_SHOPPING}) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment == null) {
                continue;
            }
            if (tag.equals(visibleTag)) {
                tx.show(fragment);
            } else {
                tx.hide(fragment);
            }
        }
        tx.commitNow();
    }
}
