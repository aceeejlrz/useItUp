package com.team15.useitup.ui.pantry;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.team15.useitup.R;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.databinding.ItemPantryBinding;
import com.team15.useitup.util.Expiry;

/** Pantry item cards: coloured edge, name, "quantity · category", status badge, Find recipes. */
public class PantryAdapter extends ListAdapter<PantryRow, PantryAdapter.Holder> {

    public interface Listener {
        void onItemClick(@NonNull PantryItem item);

        void onFindRecipes(@NonNull PantryItem item);
    }

    private final Listener listener;

    public PantryAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).item.getId();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPantryBinding binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PantryRow row = getItem(position);
        PantryItem item = row.item;
        Context context = holder.itemView.getContext();
        ItemPantryBinding b = holder.binding;

        b.name.setText(item.getName());
        b.detail.setText(context.getString(R.string.item_detail_format, item.quantityText(),
                item.getCategory()));
        b.badge.setText(Expiry.label(context.getResources(), row.daysLeft));
        b.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, Expiry.badgeBackground(row.status))));
        b.badge.setTextColor(ContextCompat.getColor(context, Expiry.badgeForeground(row.status)));
        b.edge.setBackgroundColor(ContextCompat.getColor(context, Expiry.edgeColor(row.status)));
        b.findRecipes.setContentDescription(context.getString(R.string.find_recipes_with, item.getName()));

        b.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        b.findRecipes.setOnClickListener(v -> listener.onFindRecipes(item));
    }

    /** Item id at an adapter position, or -1. Used by swipe to delete (the list may be filtered). */
    public long idAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return -1;
        }
        return getItem(position).item.getId();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemPantryBinding binding;

        Holder(@NonNull ItemPantryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Clip the status edge to the card's rounded corners.
            binding.getRoot().setClipToOutline(true);
        }
    }

    private static final DiffUtil.ItemCallback<PantryRow> DIFF = new DiffUtil.ItemCallback<PantryRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull PantryRow a, @NonNull PantryRow b) {
            return a.item.getId() == b.item.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PantryRow a, @NonNull PantryRow b) {
            return a.equals(b);
        }
    };
}
