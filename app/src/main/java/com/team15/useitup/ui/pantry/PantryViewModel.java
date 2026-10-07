package com.team15.useitup.ui.pantry;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.team15.useitup.data.AppRepository;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.util.AppPrefs;
import com.team15.useitup.util.Expiry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pantry screen state: the filtered, expiry-sorted list and the expiring-soon banner. */
public class PantryViewModel extends AndroidViewModel {

    /** Everything the Pantry screen shows. */
    public static final class State {
        @NonNull public final List<PantryRow> rows;
        public final int totalItems;
        public final int soonCount;
        public final int expiredCount;
        public final int alertDays;
        /** Up to two items that expire soonest, for the banner's Find recipes button. */
        @NonNull public final List<String> bannerIngredients;
        @Nullable public final String category;
        @NonNull public final String query;

        State(@NonNull List<PantryRow> rows, int totalItems, int soonCount, int expiredCount,
              int alertDays, @NonNull List<String> bannerIngredients, @Nullable String category,
              @NonNull String query) {
            this.rows = rows;
            this.totalItems = totalItems;
            this.soonCount = soonCount;
            this.expiredCount = expiredCount;
            this.alertDays = alertDays;
            this.bannerIngredients = bannerIngredients;
            this.category = category;
            this.query = query;
        }
    }

    private final AppRepository repository = AppRepository.get();
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    @Nullable private String category;
    @NonNull private String query = "";

    public PantryViewModel(@NonNull Application application) {
        super(application);
        state.addSource(repository.getPantryItems(), items -> recompute());
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    /** Null shows every category. */
    public void setCategory(@Nullable String value) {
        category = value;
        recompute();
    }

    public void setQuery(@Nullable String value) {
        query = value == null ? "" : value;
        recompute();
    }

    /** Re-reads the alert window, which can change on the Settings screen. */
    public void refresh() {
        recompute();
    }

    public void delete(long id, @NonNull java.util.function.Consumer<PantryItem> onDeleted) {
        PantryItem removed = repository.deletePantryItem(id);
        if (removed != null) {
            onDeleted.accept(removed);
        }
    }

    public void restore(@NonNull PantryItem item) {
        repository.restorePantryItem(item);
    }

    private void recompute() {
        int alertDays = AppPrefs.alertDays(getApplication());
        LocalDate today = LocalDate.now();
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<PantryItem> all = repository.currentPantry();

        List<PantryRow> rows = new ArrayList<>();
        List<PantryItem> soon = new ArrayList<>();
        int soonCount = 0;
        int expiredCount = 0;
        for (PantryItem item : all) {
            long days = Expiry.daysUntil(item.getExpiryDate(), today);
            Expiry.Status status = Expiry.status(days, alertDays);
            if (status == Expiry.Status.SOON) {
                soonCount++;
                soon.add(item);
            } else if (status == Expiry.Status.EXPIRED) {
                expiredCount++;
            }
            boolean categoryOk = category == null || category.equals(item.getCategory());
            boolean queryOk = q.isEmpty() || item.getName().toLowerCase(Locale.getDefault()).contains(q);
            if (categoryOk && queryOk) {
                rows.add(new PantryRow(item, days, status));
            }
        }

        List<String> banner = new ArrayList<>();
        for (int i = 0; i < soon.size() && i < 2; i++) {
            banner.add(soon.get(i).getName());
        }
        Collections.sort(banner, String.CASE_INSENSITIVE_ORDER);

        state.setValue(new State(Collections.unmodifiableList(rows), all.size(), soonCount,
                expiredCount, alertDays, banner, category, query));
    }
}
