package com.team15.useitup.ui.item;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.team15.useitup.R;
import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.data.SampleData;
import com.team15.useitup.databinding.ActivityAddEditItemBinding;
import com.team15.useitup.util.Expiry;
import com.team15.useitup.util.SystemBars;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Add a pantry item in a few seconds, or edit or delete an existing one. */
public class AddEditItemActivity extends AppCompatActivity {

    private static final String EXTRA_ITEM_ID = "com.team15.useitup.extra.ITEM_ID";
    private static final String STATE_EXPIRY = "expiry_epoch_day";
    private static final String TAG_DATE_PICKER = "expiry_date_picker";

    private ActivityAddEditItemBinding binding;
    private final AppRepository repository = AppRepository.get();
    @Nullable private PantryItem editing;
    @Nullable private LocalDate expiry;

    @NonNull
    public static Intent intentForAdd(@NonNull Context context) {
        return new Intent(context, AddEditItemActivity.class);
    }

    @NonNull
    public static Intent intentForEdit(@NonNull Context context, long itemId) {
        return new Intent(context, AddEditItemActivity.class).putExtra(EXTRA_ITEM_ID, itemId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long id = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        editing = id >= 0 ? repository.findPantryItem(id) : null;
        if (id >= 0 && editing == null) {
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(editing != null ? R.string.edit_item_title : R.string.add_item_title);
        }
        SystemBars.padBottom(binding.scroll);

        binding.name.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown,
                SampleData.ingredientSuggestions()));
        binding.unit.setSimpleItems(getResources().getStringArray(R.array.units));
        binding.category.setSimpleItems(getResources().getStringArray(R.array.categories));

        binding.scanGroup.setVisibility(editing == null ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.scanBarcode.setOnClickListener(v ->
                Snackbar.make(binding.coordinator, R.string.scan_barcode_later, Snackbar.LENGTH_SHORT).show());

        if (savedInstanceState == null) {
            if (editing != null) {
                binding.name.setText(editing.getName(), false);
                binding.quantity.setText(editing.quantityNumberText());
                binding.unit.setText(editing.getUnit(), false);
                binding.category.setText(editing.getCategory(), false);
                expiry = editing.getExpiryDate();
            } else {
                binding.unit.setText(getResources().getStringArray(R.array.units)[0], false);
                String[] categories = getResources().getStringArray(R.array.categories);
                binding.category.setText(categories[categories.length - 1], false);
            }
        } else if (savedInstanceState.containsKey(STATE_EXPIRY)) {
            expiry = LocalDate.ofEpochDay(savedInstanceState.getLong(STATE_EXPIRY));
        }
        updateExpiryField();

        binding.expiry.setOnClickListener(v -> showDatePicker());
        binding.expiryLayout.setEndIconOnClickListener(v -> showDatePicker());
        clearErrorWhenEdited(binding.nameLayout, binding.name);
        clearErrorWhenEdited(binding.quantityLayout, binding.quantity);
        binding.save.setOnClickListener(v -> save());

        // Re-attach the listener if the date picker survived a rotation.
        Fragment existing = getSupportFragmentManager().findFragmentByTag(TAG_DATE_PICKER);
        if (existing instanceof MaterialDatePicker) {
            @SuppressWarnings("unchecked")
            MaterialDatePicker<Long> picker = (MaterialDatePicker<Long>) existing;
            picker.addOnPositiveButtonClickListener(this::onDatePicked);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (expiry != null) {
            outState.putLong(STATE_EXPIRY, expiry.toEpochDay());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (editing != null) {
            getMenuInflater().inflate(R.menu.menu_edit_item, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Return to the screen that opened this one without recreating MainActivity.
        finish();
        return true;
    }

    private void showDatePicker() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_DATE_PICKER) != null) {
            return;
        }
        LocalDate start = expiry != null ? expiry : LocalDate.now();
        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder();
        if (editing == null) {
            // New items cannot expire in the past; today is still allowed.
            constraints.setValidator(DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds()));
        }
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .setCalendarConstraints(constraints.build())
                .build();
        picker.addOnPositiveButtonClickListener(this::onDatePicked);
        picker.show(getSupportFragmentManager(), TAG_DATE_PICKER);
    }

    private void onDatePicked(@Nullable Long selection) {
        if (selection == null) {
            return;
        }
        // MaterialDatePicker works in UTC milliseconds at midnight.
        expiry = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
        binding.expiryLayout.setError(null);
        updateExpiryField();
    }

    private void updateExpiryField() {
        if (expiry == null) {
            binding.expiry.setText(null);
            binding.expiryLayout.setHelperText(null);
            return;
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern(getString(R.string.date_format_field),
                Locale.getDefault());
        binding.expiry.setText(expiry.format(format));
        long days = Expiry.daysUntil(expiry, LocalDate.now());
        binding.expiryLayout.setHelperText(Expiry.helperText(getResources(), days));
    }

    private void save() {
        String name = textOf(binding.name.getText());
        String unit = textOf(binding.unit.getText());
        String category = textOf(binding.category.getText());
        double quantity = parseQuantity(textOf(binding.quantity.getText()));

        boolean valid = true;
        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        }
        if (quantity <= 0) {
            binding.quantityLayout.setError(getString(R.string.error_quantity));
            valid = false;
        }
        if (expiry == null) {
            binding.expiryLayout.setError(getString(R.string.error_expiry_required));
            valid = false;
        }
        if (!valid) {
            return;
        }
        if (unit.isEmpty()) {
            unit = getResources().getStringArray(R.array.units)[0];
        }
        if (category.isEmpty()) {
            String[] categories = getResources().getStringArray(R.array.categories);
            category = categories[categories.length - 1];
        }
        if (editing != null) {
            repository.updatePantryItem(new PantryItem(editing.getId(), name, quantity, unit, category,
                    expiry, editing.getAddedDate()));
        } else {
            repository.addPantryItem(name, quantity, unit, category, expiry);
            // Lets the Pantry screen know a new item was added (it may ask for notifications).
            setResult(RESULT_OK);
        }
        finish();
    }

    private void confirmDelete() {
        if (editing == null) {
            return;
        }
        String name = editing.getName();
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_delete)
                .setTitle(getString(R.string.delete_dialog_title, name))
                .setMessage(getString(R.string.delete_dialog_message, name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.deletePantryItem(editing.getId());
                    finish();
                })
                .show();
    }

    private static double parseQuantity(@NonNull String text) {
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @NonNull
    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private static void clearErrorWhenEdited(@NonNull TextInputLayout layout,
                                             @NonNull android.widget.TextView field) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(layout.getError())) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
