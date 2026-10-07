package com.team15.useitup.ui.recipes;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.team15.useitup.R;
import com.team15.useitup.data.SampleData;
import com.team15.useitup.databinding.FragmentRecipesBinding;
import com.team15.useitup.ui.detail.RecipeDetailActivity;
import com.team15.useitup.ui.settings.SettingsActivity;
import com.team15.useitup.util.SystemBars;
import com.team15.useitup.util.TextLists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Recipes (Search | Saved): find recipes that use pantry ingredients, or open saved ones. */
public class RecipesFragment extends Fragment {

    private FragmentRecipesBinding binding;
    private RecipesViewModel viewModel;
    private RecipeAdapter adapter;
    @NonNull private List<String> shownIngredients = Collections.emptyList();
    @NonNull private List<String> shownSuggestions = Collections.emptyList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecipesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Shared with MainActivity so the Pantry can start a search.
        viewModel = new ViewModelProvider(requireActivity()).get(RecipesViewModel.class);

        adapter = new RecipeAdapter(recipe ->
                startActivity(RecipeDetailActivity.intent(requireContext(), recipe.getId())));
        binding.grid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.grid.setAdapter(adapter);
        SystemBars.padBottomForKeyboard(binding.content);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(SettingsActivity.intent(requireContext()));
                return true;
            }
            return false;
        });

        binding.tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setTab(checkedId == R.id.tab_saved
                        ? RecipesViewModel.Tab.SAVED : RecipesViewModel.Tab.SEARCH);
            }
        });

        binding.ingredientInput.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown,
                SampleData.ingredientSuggestions()));
        binding.ingredientInput.setOnItemClickListener((parent, v, position, id) ->
                addTypedIngredient(String.valueOf(parent.getItemAtPosition(position))));
        binding.ingredientInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || enter) {
                addTypedIngredient(String.valueOf(binding.ingredientInput.getText()));
                return true;
            }
            return false;
        });

        binding.messagePrimary.setOnClickListener(v -> onMessageAction(true));
        binding.messageSecondary.setOnClickListener(v -> onMessageAction(false));

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void addTypedIngredient(@NonNull String text) {
        String name = text.trim();
        if (!name.isEmpty()) {
            viewModel.addIngredient(name);
        }
        binding.ingredientInput.setText(null);
    }

    private void onMessageAction(boolean primary) {
        RecipesViewModel.State state = viewModel.getState().getValue();
        if (state == null) {
            return;
        }
        if (state.mode == RecipesViewModel.Mode.OFFLINE && state.tab == RecipesViewModel.Tab.SEARCH) {
            if (primary) {
                viewModel.retry();
            } else {
                binding.tabs.check(R.id.tab_saved);
            }
        } else if (state.mode == RecipesViewModel.Mode.EMPTY && primary) {
            viewModel.clearIngredients();
        }
    }

    private void render(@NonNull RecipesViewModel.State state) {
        Resources res = getResources();
        boolean search = state.tab == RecipesViewModel.Tab.SEARCH;
        int checkedTab = search ? R.id.tab_search : R.id.tab_saved;
        if (binding.tabs.getCheckedButtonId() != checkedTab) {
            binding.tabs.check(checkedTab);
        }
        binding.searchGroup.setVisibility(search ? View.VISIBLE : View.GONE);
        renderIngredientChips(state.ingredients);
        renderSuggestions(state.suggestions);

        // Summary line and progress bar.
        binding.progress.setVisibility(search && state.mode == RecipesViewModel.Mode.LOADING
                ? View.VISIBLE : View.GONE);
        if (!search) {
            showSummary(res.getQuantityString(R.plurals.saved_summary, state.cards.size(), state.cards.size()),
                    !state.cards.isEmpty(), true);
        } else if (state.mode == RecipesViewModel.Mode.RESULTS) {
            showSummary(res.getQuantityString(R.plurals.recipes_summary, state.cards.size(), state.cards.size()),
                    true, false);
        } else if (state.mode == RecipesViewModel.Mode.LOADING) {
            showSummary(getString(R.string.recipes_searching), true, false);
        } else {
            showSummary(null, false, false);
        }

        // Grid or a message.
        if (!search) {
            if (state.cards.isEmpty()) {
                showMessage(R.drawable.ic_bookmark, R.string.saved_empty_title,
                        getString(R.string.saved_empty_body), 0, 0, false);
            } else {
                hideMessage();
                adapter.showCards(state.cards);
            }
            return;
        }
        switch (state.mode) {
            case LOADING:
                hideMessage();
                adapter.showLoading();
                break;
            case RESULTS:
                hideMessage();
                adapter.showCards(state.cards);
                break;
            case OFFLINE:
                showMessage(R.drawable.ic_cloud_off, R.string.offline_title,
                        getString(R.string.offline_body), R.string.retry, R.string.open_saved, false);
                break;
            case EMPTY:
                List<String> quoted = new ArrayList<>();
                for (String name : state.ingredients) {
                    quoted.add(getString(R.string.quoted, name));
                }
                String names = TextLists.joinNatural(quoted, getString(R.string.list_separator),
                        getString(R.string.quoted_or));
                showMessage(R.drawable.ic_search_off, R.string.empty_title,
                        getString(R.string.empty_body, names), R.string.clear_ingredients, 0, true);
                break;
            default:
                showMessage(R.drawable.ic_restaurant, R.string.prompt_title,
                        getString(R.string.prompt_body), 0, 0, false);
                break;
        }
    }

    private void showSummary(@Nullable String text, boolean visible, boolean savedIcon) {
        binding.summary.setVisibility(visible && text != null ? View.VISIBLE : View.GONE);
        binding.summary.setText(text);
        binding.summary.setCompoundDrawablesRelativeWithIntrinsicBounds(
                savedIcon ? R.drawable.ic_check_circle_small : 0, 0, 0, 0);
    }

    private void hideMessage() {
        binding.message.setVisibility(View.GONE);
        binding.grid.setVisibility(View.VISIBLE);
    }

    private void showMessage(@DrawableRes int icon, @StringRes int title, @NonNull String body,
                             @StringRes int primaryAction, @StringRes int secondaryAction,
                             boolean primaryIsOutlined) {
        adapter.showCards(Collections.emptyList());
        binding.grid.setVisibility(View.GONE);
        binding.message.setVisibility(View.VISIBLE);
        binding.messageIcon.setImageResource(icon);
        binding.messageTitle.setText(title);
        binding.messageBody.setText(body);
        boolean hasPrimary = primaryAction != 0;
        binding.messagePrimary.setVisibility(hasPrimary && !primaryIsOutlined ? View.VISIBLE : View.GONE);
        binding.messagePrimaryOutlined.setVisibility(hasPrimary && primaryIsOutlined ? View.VISIBLE : View.GONE);
        if (hasPrimary) {
            binding.messagePrimary.setText(primaryAction);
            binding.messagePrimaryOutlined.setText(primaryAction);
            binding.messagePrimaryOutlined.setOnClickListener(v -> onMessageAction(true));
        }
        binding.messageSecondary.setVisibility(secondaryAction != 0 ? View.VISIBLE : View.GONE);
        if (secondaryAction != 0) {
            binding.messageSecondary.setText(secondaryAction);
        }
    }

    private void renderIngredientChips(@NonNull List<String> ingredients) {
        if (ingredients.equals(shownIngredients)) {
            return;
        }
        shownIngredients = ingredients;
        binding.ingredientChips.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String name : ingredients) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_input, binding.ingredientChips, false);
            chip.setText(name);
            chip.setCloseIconContentDescription(getString(R.string.remove_ingredient, name));
            chip.setOnCloseIconClickListener(v -> viewModel.removeIngredient(name));
            binding.ingredientChips.addView(chip);
        }
        binding.ingredientChips.setVisibility(ingredients.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderSuggestions(@NonNull List<String> suggestions) {
        binding.suggestRow.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
        if (suggestions.equals(shownSuggestions)) {
            return;
        }
        shownSuggestions = suggestions;
        binding.suggestChips.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String name : suggestions) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_suggestion, binding.suggestChips, false);
            chip.setText(name);
            chip.setContentDescription(getString(R.string.add_ingredient_to_search, name));
            chip.setOnClickListener(v -> viewModel.addIngredient(name));
            binding.suggestChips.addView(chip);
        }
    }
}
