package com.team15.useitup.ui.pantry;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.team15.useitup.R;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.databinding.FragmentPantryBinding;
import com.team15.useitup.ui.common.SwipeToDeleteCallback;
import com.team15.useitup.ui.item.AddEditItemActivity;
import com.team15.useitup.ui.main.MainActivity;
import com.team15.useitup.ui.settings.SettingsActivity;
import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.NotificationHelper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Pantry (Home): every item, sorted so the food that expires first is at the top. */
public class PantryFragment extends Fragment {

    private FragmentPantryBinding binding;
    private PantryViewModel viewModel;
    private PantryAdapter adapter;
    @NonNull private List<String> bannerIngredients = Collections.emptyList();

    // Registered as fields so they exist before the fragment starts, as the API requires.
    private final ActivityResultLauncher<Intent> addItemLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    maybeAskForNotifications();
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted && binding != null) {
                    Snackbar.make(binding.coordinator, R.string.notification_prompt_denied,
                            Snackbar.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPantryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(PantryViewModel.class);

        adapter = new PantryAdapter(new PantryAdapter.Listener() {
            @Override
            public void onItemClick(@NonNull PantryItem item) {
                startActivity(AddEditItemActivity.intentForEdit(requireContext(), item.getId()));
            }

            @Override
            public void onFindRecipes(@NonNull PantryItem item) {
                openRecipes(Collections.singletonList(item.getName()));
            }
        });
        binding.list.setAdapter(adapter);

        setUpToolbar();
        setUpCategoryChips();
        setUpSwipeToDelete();

        binding.fab.setOnClickListener(v ->
                addItemLauncher.launch(AddEditItemActivity.intentForAdd(requireContext())));
        binding.bannerFindRecipes.setOnClickListener(v -> openRecipes(bannerIngredients));

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        // The alert window may have changed on the Settings screen.
        viewModel.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Asks for notification permission (Android 13+) right after the user adds an item, the
     * first moment a reminder is useful to them. Shown at most once; after that the user can
     * still allow notifications from Settings.
     */
    private void maybeAskForNotifications() {
        Context context = requireContext();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || NotificationHelper.canPost(context)
                || !AppPrefs.remindersEnabled(context)
                || AppPrefs.notificationPromptShown(context)) {
            return;
        }
        AppPrefs.setNotificationPromptShown(context);
        new MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.ic_notifications)
                .setTitle(R.string.notification_prompt_title)
                .setMessage(R.string.permission_rationale)
                .setPositiveButton(R.string.notification_prompt_allow, (dialog, which) ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton(R.string.notification_prompt_not_now, null)
                .show();
    }

    private void setUpToolbar() {
        MenuItem searchItem = binding.toolbar.getMenu().findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.pantry_search_hint));
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.setQuery(newText);
                    return true;
                }
            });
        }
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                viewModel.setQuery("");
                return true;
            }
        });
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(SettingsActivity.intent(requireContext()));
                return true;
            }
            return false;
        });
    }

    private void setUpCategoryChips() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String[] categories = getResources().getStringArray(R.array.categories);
        PantryViewModel.State current = viewModel.getState().getValue();
        String selected = current != null ? current.category : null;

        addChip(inflater, getString(R.string.category_all), null, selected == null);
        for (String category : categories) {
            addChip(inflater, category, category, category.equals(selected));
        }
        binding.categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            viewModel.setCategory(chip == null ? null : (String) chip.getTag());
        });
    }

    private void addChip(@NonNull LayoutInflater inflater, @NonNull String label,
                         @Nullable String category, boolean checked) {
        Chip chip = (Chip) inflater.inflate(R.layout.chip_filter, binding.categoryChips, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(category);
        binding.categoryChips.addView(chip);
        chip.setChecked(checked);
    }

    private void setUpSwipeToDelete() {
        new ItemTouchHelper(new SwipeToDeleteCallback(requireContext(), 12) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                long id = adapter.idAt(holder.getBindingAdapterPosition());
                if (id < 0) {
                    return;
                }
                viewModel.delete(id, removed -> {
                    if (binding == null) {
                        return;
                    }
                    Snackbar.make(binding.coordinator, R.string.item_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v -> viewModel.restore(removed))
                            .show();
                });
            }
        }).attachToRecyclerView(binding.list);
    }

    private void render(@NonNull PantryViewModel.State state) {
        adapter.submitList(state.rows);
        Resources res = getResources();

        boolean pantryEmpty = state.totalItems == 0;
        binding.emptyState.setVisibility(pantryEmpty ? View.VISIBLE : View.GONE);
        binding.chipScroll.setVisibility(pantryEmpty ? View.GONE : View.VISIBLE);

        boolean noMatch = !pantryEmpty && state.rows.isEmpty();
        binding.noMatch.setVisibility(noMatch ? View.VISIBLE : View.GONE);
        if (noMatch) {
            String query = state.query.trim();
            binding.noMatch.setText(!query.isEmpty()
                    ? getString(R.string.pantry_no_match_query, query)
                    : getString(R.string.pantry_no_match_category, Objects.toString(state.category, "")));
        }

        boolean showBanner = !pantryEmpty && (state.soonCount > 0 || state.expiredCount > 0);
        binding.banner.setVisibility(showBanner ? View.VISIBLE : View.GONE);
        if (showBanner) {
            String window = res.getQuantityString(R.plurals.banner_window, state.alertDays, state.alertDays);
            if (state.soonCount > 0) {
                binding.bannerTitle.setText(res.getQuantityString(R.plurals.banner_soon_title,
                        state.soonCount, state.soonCount));
                binding.bannerBody.setText(state.expiredCount > 0
                        ? getString(R.string.banner_expired_suffix, window, state.expiredCount)
                        : window);
                binding.bannerBody.setVisibility(View.VISIBLE);
                binding.bannerFindRecipes.setVisibility(View.VISIBLE);
            } else {
                binding.bannerTitle.setText(res.getQuantityString(R.plurals.banner_expired_title,
                        state.expiredCount, state.expiredCount));
                binding.bannerBody.setVisibility(View.GONE);
                binding.bannerFindRecipes.setVisibility(View.GONE);
            }
        }
        bannerIngredients = state.bannerIngredients;
    }

    private void openRecipes(@NonNull List<String> ingredients) {
        if (ingredients.isEmpty() || !(requireActivity() instanceof MainActivity)) {
            return;
        }
        ((MainActivity) requireActivity()).openRecipesFor(ingredients);
    }
}