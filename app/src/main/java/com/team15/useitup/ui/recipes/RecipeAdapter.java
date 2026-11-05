package com.team15.useitup.ui.recipes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.team15.useitup.R;
import com.team15.useitup.data.Recipe;
import com.team15.useitup.databinding.ItemRecipeBinding;

import java.util.ArrayList;
import java.util.List;

/** Two-column grid of recipe cards, or skeleton cards while a search is loading. */
public class RecipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onRecipeClick(@NonNull Recipe recipe);
    }

    private static final int TYPE_CARD = 0;
    private static final int TYPE_SKELETON = 1;
    private static final int SKELETON_COUNT = 4;

    private final Listener listener;
    private final List<RecipesViewModel.Card> cards = new ArrayList<>();
    private boolean loading;

    public RecipeAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void showCards(@NonNull List<RecipesViewModel.Card> newCards) {
        loading = false;
        cards.clear();
        cards.addAll(newCards);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void showLoading() {
        loading = true;
        cards.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return loading ? SKELETON_COUNT : cards.size();
    }

    @Override
    public int getItemViewType(int position) {
        return loading ? TYPE_SKELETON : TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SKELETON) {
            View view = inflater.inflate(R.layout.item_recipe_skeleton, parent, false);
            return new RecyclerView.ViewHolder(view) { };
        }
        return new CardHolder(ItemRecipeBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof CardHolder)) {
            return;
        }
        RecipesViewModel.Card card = cards.get(position);
        ItemRecipeBinding b = ((CardHolder) holder).binding;
        Context context = b.getRoot().getContext();
        Recipe recipe = card.recipe;

        if (recipe.getImageUrl() != null) {
            Glide.with(b.image).load(recipe.getImageUrl())
                    .placeholder(R.color.md_surface_variant)
                    .error(R.color.md_surface_variant)
                    .into(b.image);
        } else {
            Glide.with(b.image).clear(b.image);
            b.image.setImageResource(recipe.getImageRes());
        }
        b.name.setText(recipe.getName());
        b.savedMark.setVisibility(card.savedTab ? View.VISIBLE : View.GONE);
        if (card.savedTab) {
            b.badge.setVisibility(View.GONE);
            b.subtitle.setVisibility(View.VISIBLE);
            b.subtitle.setText(context.getString(R.string.recipe_meta, recipe.getArea(), recipe.getCategory()));
        } else {
            b.subtitle.setVisibility(View.GONE);
            b.badge.setVisibility(View.VISIBLE);
            b.badge.setText(context.getResources().getQuantityString(R.plurals.match_badge,
                    card.ingredientCount, card.matchCount, card.ingredientCount));
            boolean full = card.isFullMatch();
            b.badge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    full ? R.color.status_fresh_bg : R.color.status_neutral_bg)));
            b.badge.setTextColor(ContextCompat.getColor(context,
                    full ? R.color.status_fresh_fg : R.color.status_neutral_fg));
        }
        b.getRoot().setOnClickListener(v -> listener.onRecipeClick(recipe));
    }

    static final class CardHolder extends RecyclerView.ViewHolder {
        final ItemRecipeBinding binding;

        CardHolder(@NonNull ItemRecipeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
