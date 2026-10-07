package com.team15.useitup.ui.shopping;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.team15.useitup.R;
import com.team15.useitup.data.ShoppingItem;
import com.team15.useitup.databinding.ItemShoppingBinding;
import com.team15.useitup.databinding.ItemShoppingFooterBinding;
import com.team15.useitup.databinding.ItemShoppingHeaderBinding;

import java.util.ArrayList;
import java.util.List;

/** "To buy" and "In cart" sections, then the optional Move checked to pantry button. */
public class ShoppingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onCheckedChanged(@NonNull ShoppingItem item, boolean checked);

        void onMoveToPantry();
    }

    static final int TYPE_HEADER = 0;
    static final int TYPE_ITEM = 1;
    static final int TYPE_FOOTER = 2;

    private static final class Row {
        final int type;
        final String header;
        final ShoppingItem item;

        Row(int type, String header, ShoppingItem item) {
            this.type = type;
            this.header = header;
            this.item = item;
        }
    }

    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();

    public ShoppingAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(@NonNull Context context, @NonNull List<ShoppingItem> items) {
        rows.clear();
        List<ShoppingItem> toBuy = new ArrayList<>();
        List<ShoppingItem> inCart = new ArrayList<>();
        for (ShoppingItem item : items) {
            (item.isChecked() ? inCart : toBuy).add(item);
        }
        if (!toBuy.isEmpty()) {
            rows.add(new Row(TYPE_HEADER, context.getString(R.string.to_buy_header, toBuy.size()), null));
            for (ShoppingItem item : toBuy) {
                rows.add(new Row(TYPE_ITEM, null, item));
            }
        }
        if (!inCart.isEmpty()) {
            rows.add(new Row(TYPE_HEADER, context.getString(R.string.in_cart_header, inCart.size()), null));
            for (ShoppingItem item : inCart) {
                rows.add(new Row(TYPE_ITEM, null, item));
            }
        }
        if (!items.isEmpty()) {
            rows.add(new Row(TYPE_FOOTER, null, null));
        }
        notifyDataSetChanged();
    }

    /** The shopping item at a position, or null for headers and the footer. */
    public ShoppingItem itemAt(int position) {
        if (position < 0 || position >= rows.size()) {
            return null;
        }
        return rows.get(position).item;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(ItemShoppingHeaderBinding.inflate(inflater, parent, false));
        }
        if (viewType == TYPE_FOOTER) {
            return new FooterHolder(ItemShoppingFooterBinding.inflate(inflater, parent, false));
        }
        return new ItemHolder(ItemShoppingBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.title.setText(row.header);
        } else if (holder instanceof FooterHolder) {
            ((FooterHolder) holder).binding.moveToPantry.setOnClickListener(v -> listener.onMoveToPantry());
        } else if (holder instanceof ItemHolder) {
            bindItem((ItemHolder) holder, row.item);
        }
    }

    private void bindItem(@NonNull ItemHolder holder, @NonNull ShoppingItem item) {
        ItemShoppingBinding b = holder.binding;
        Context context = b.getRoot().getContext();
        b.name.setText(item.getName());
        String note = item.getQuantityNote();
        String recipe = item.getFromRecipe();
        String detail;
        if (note != null && recipe != null) {
            detail = context.getString(R.string.note_for_recipe, note, recipe);
        } else if (recipe != null) {
            detail = context.getString(R.string.for_recipe, recipe);
        } else {
            detail = note;
        }
        b.detail.setText(detail);
        b.detail.setVisibility(detail == null || detail.isEmpty() ? View.GONE : View.VISIBLE);

        boolean checked = item.isChecked();
        b.checkbox.setOnCheckedChangeListener(null);
        b.checkbox.setChecked(checked);
        b.checkbox.setContentDescription(item.getName());
        b.checkbox.setOnCheckedChangeListener((button, isChecked) -> listener.onCheckedChanged(item, isChecked));
        b.getRoot().setOnClickListener(v -> b.checkbox.toggle());

        int nameColor = ContextCompat.getColor(context, checked ? R.color.md_on_surface_variant : R.color.md_on_surface);
        b.name.setTextColor(nameColor);
        setStrike(b.name, checked);
        setStrike(b.detail, checked);
    }

    private static void setStrike(@NonNull android.widget.TextView view, boolean strike) {
        int flags = view.getPaintFlags();
        view.setPaintFlags(strike ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemShoppingHeaderBinding binding;

        HeaderHolder(@NonNull ItemShoppingHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final class ItemHolder extends RecyclerView.ViewHolder {
        final ItemShoppingBinding binding;

        ItemHolder(@NonNull ItemShoppingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final class FooterHolder extends RecyclerView.ViewHolder {
        final ItemShoppingFooterBinding binding;

        FooterHolder(@NonNull ItemShoppingFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
