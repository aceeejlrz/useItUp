package com.team15.useitup.ui.shopping;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.team15.useitup.R;
import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.ShoppingItem;
import com.team15.useitup.databinding.FragmentShoppingBinding;
import com.team15.useitup.ui.common.SwipeToDeleteCallback;
import com.team15.useitup.ui.settings.SettingsActivity;
import com.team15.useitup.util.SystemBars;

import java.util.List;

/** Shopping list: plan groceries and tick items off in the store, even offline. */
public class ShoppingFragment extends Fragment {

    private FragmentShoppingBinding binding;
    private final AppRepository repository = AppRepository.get();
    private ShoppingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentShoppingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new ShoppingAdapter(new ShoppingAdapter.Listener() {
            @Override
            public void onCheckedChanged(@NonNull ShoppingItem item, boolean checked) {
                // Post so the RecyclerView is not changed while it handles the click.
                binding.list.post(() -> repository.setChecked(item.getId(), checked));
            }

            @Override
            public void onMoveToPantry() {
                Snackbar.make(binding.coordinator, R.string.move_to_pantry_later, Snackbar.LENGTH_SHORT).show();
            }
        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        SystemBars.padBottomForKeyboard(binding.list);

        binding.clearChecked.setOnClickListener(v -> clearChecked());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(SettingsActivity.intent(requireContext()));
                return true;
            }
            return false;
        });

        binding.add.setOnClickListener(v -> addTypedItem());
        binding.newItem.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || enter) {
                addTypedItem();
                return true;
            }
            return false;
        });

        new ItemTouchHelper(new SwipeToDeleteCallback(requireContext(), 0) {
            @Override
            protected boolean canSwipe(@NonNull RecyclerView.ViewHolder holder) {
                return holder.getItemViewType() == ShoppingAdapter.TYPE_ITEM;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                ShoppingItem item = adapter.itemAt(holder.getBindingAdapterPosition());
                if (item == null) {
                    return;
                }
                ShoppingItem removed = repository.deleteShoppingItem(item.getId());
                if (removed != null) {
                    Snackbar.make(binding.coordinator, R.string.item_removed, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v -> repository.restoreShoppingItem(removed))
                            .show();
                }
            }
        }).attachToRecyclerView(binding.list);

        repository.getShoppingItems().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void render(@NonNull List<ShoppingItem> items) {
        adapter.submit(requireContext(), items);
        boolean anyChecked = false;
        for (ShoppingItem item : items) {
            if (item.isChecked()) {
                anyChecked = true;
                break;
            }
        }
        binding.clearChecked.setEnabled(anyChecked);
        binding.emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addTypedItem() {
        CharSequence text = binding.newItem.getText();
        String name = text == null ? "" : text.toString().trim();
        if (name.isEmpty()) {
            return;
        }
        repository.addShoppingItem(name, null, null);
        binding.newItem.setText(null);
    }

    private void clearChecked() {
        int removed = repository.clearChecked();
        if (removed > 0) {
            Snackbar.make(binding.coordinator,
                    getResources().getQuantityString(R.plurals.cleared_checked, removed, removed),
                    Snackbar.LENGTH_SHORT).show();
        }
    }
}
