package com.team15.useitup.ui.detail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.team15.useitup.R;
import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.Recipe;
import com.team15.useitup.data.RecipeIngredient;
import com.team15.useitup.databinding.ActivityRecipeDetailBinding;
import com.team15.useitup.databinding.ItemIngredientBinding;
import com.team15.useitup.databinding.ItemStepBinding;
import com.team15.useitup.ui.main.MainActivity;
import com.team15.useitup.util.IngredientMatcher;
import com.team15.useitup.util.SystemBars;

import java.util.List;

/** A full recipe and which of its ingredients are already in the pantry. */
public class RecipeDetailActivity extends AppCompatActivity {

    private static final String EXTRA_RECIPE_ID = "com.team15.useitup.extra.RECIPE_ID";

    private ActivityRecipeDetailBinding binding;
    private final AppRepository repository = AppRepository.get();
    private Recipe recipe;

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull String recipeId) {
        return new Intent(context, RecipeDetailActivity.class).putExtra(EXTRA_RECIPE_ID, recipeId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        recipe = repository.findRecipe(getIntent().getStringExtra(EXTRA_RECIPE_ID));
        if (recipe == null) {
            finish();
            return;
        }
        binding = ActivityRecipeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.recipe_title);
        }
        SystemBars.padBottom(binding.scroll);

        binding.hero.setImageResource(recipe.getHeroRes());
        binding.hero.setContentDescription(recipe.getName());
        binding.title.setText(recipe.getName());
        binding.meta.setText(getString(R.string.recipe_meta, recipe.getArea(), recipe.getCategory()));
        binding.watchVideo.setOnClickListener(v -> openVideo());
        binding.addMissing.setOnClickListener(v -> addMissing());
        binding.viewList.setOnClickListener(v -> openShoppingList());
        renderSteps();

        // Re-check In pantry / Missing whenever the pantry or the shopping list changes.
        repository.getPantryItems().observe(this, items -> renderIngredients());
        repository.getShoppingItems().observe(this, items -> renderIngredients());
        repository.getSavedRecipeIds().observe(this, ids -> invalidateOptionsMenu());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem save = menu.findItem(R.id.action_save);
        boolean saved = repository.isSaved(recipe.getId());
        save.setIcon(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        save.setTitle(saved ? R.string.unsave_recipe : R.string.save_recipe);
        if (save.getIcon() != null) {
            save.getIcon().mutate().setTintList(ColorStateList.valueOf(saved
                    ? ContextCompat.getColor(this, R.color.md_primary)
                    : ContextCompat.getColor(this, R.color.md_on_surface_variant)));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            boolean saved = !repository.isSaved(recipe.getId());
            repository.setSaved(recipe.getId(), saved);
            Snackbar.make(binding.coordinator, saved ? R.string.recipe_saved : R.string.recipe_unsaved,
                    Snackbar.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void renderIngredients() {
        List<String> pantryNames = repository.pantryNames();
        LayoutInflater inflater = getLayoutInflater();
        binding.ingredients.removeAllViews();
        int have = 0;
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            boolean inPantry = IngredientMatcher.containsMatch(pantryNames, ingredient.getName());
            if (inPantry) {
                have++;
            }
            ItemIngredientBinding row = ItemIngredientBinding.inflate(inflater, binding.ingredients, false);
            row.icon.setImageResource(inPantry ? R.drawable.ic_check : R.drawable.ic_close);
            row.icon.setImageTintList(ColorStateList.valueOf(inPantry
                    ? ContextCompat.getColor(this, R.color.md_primary)
                    : ContextCompat.getColor(this, R.color.md_error)));
            row.name.setText(ingredient.getName());
            row.measure.setText(getString(R.string.ingredient_measure, ingredient.getMeasure()));
            row.label.setText(inPantry ? R.string.in_pantry : R.string.missing);
            row.label.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    inPantry ? R.color.status_fresh_bg : R.color.status_expired_bg)));
            row.label.setTextColor(ContextCompat.getColor(this,
                    inPantry ? R.color.status_fresh_fg : R.color.status_expired_fg));
            binding.ingredients.addView(row.getRoot());
        }
        binding.inPantryCount.setText(getString(R.string.ingredients_in_pantry, have,
                recipe.getIngredients().size()));
        renderButton();
    }

    private void renderButton() {
        int missing = repository.missingIngredients(recipe).size();
        int toAdd = repository.missingNotOnList(recipe).size();
        binding.addMissing.setVisibility(toAdd > 0 ? View.VISIBLE : View.GONE);
        binding.viewList.setVisibility(missing > 0 && toAdd == 0 ? View.VISIBLE : View.GONE);
        binding.haveEverything.setVisibility(missing == 0 ? View.VISIBLE : View.GONE);
        if (toAdd > 0) {
            binding.addMissing.setText(getResources().getQuantityString(R.plurals.add_missing, toAdd, toAdd));
        }
    }

    private void renderSteps() {
        LayoutInflater inflater = getLayoutInflater();
        List<String> steps = recipe.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            ItemStepBinding row = ItemStepBinding.inflate(inflater, binding.steps, false);
            row.number.setText(String.valueOf(i + 1));
            row.text.setText(steps.get(i));
            binding.steps.addView(row.getRoot());
        }
    }

    private void addMissing() {
        int added = repository.addMissingToShoppingList(recipe);
        if (added == 0) {
            return;
        }
        Snackbar.make(binding.coordinator,
                        getResources().getQuantityString(R.plurals.items_added, added, added),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.view_list, v -> openShoppingList())
                .show();
    }

    private void openShoppingList() {
        // Brings the existing MainActivity forward on the Shopping tab.
        startActivity(MainActivity.intentForTab(this, MainActivity.TAB_SHOPPING));
    }

    private void openVideo() {
        Uri uri = Uri.parse("https://www.youtube.com/results").buildUpon()
                .appendQueryParameter("search_query", recipe.getName() + " recipe")
                .build();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.coordinator, uri.toString(), Snackbar.LENGTH_LONG).show();
        }
    }

}
