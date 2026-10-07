package com.team15.useitup.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.team15.useitup.R;

/** Swipe left to delete: draws a red "Delete" area with a bin icon behind the swiped row. */
public abstract class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable icon;
    private final String label;
    private final float radius;
    private final int iconSize;
    private final int margin;
    private final RectF rect = new RectF();

    protected SwipeToDeleteCallback(@NonNull Context context, float cornerRadiusDp) {
        super(0, ItemTouchHelper.LEFT);
        float density = context.getResources().getDisplayMetrics().density;
        int fg = ContextCompat.getColor(context, R.color.swipe_delete_fg);
        background.setColor(ContextCompat.getColor(context, R.color.swipe_delete_bg));
        text.setColor(fg);
        text.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14,
                context.getResources().getDisplayMetrics()));
        text.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        Drawable d = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        icon = d == null ? null : DrawableCompat.wrap(d.mutate());
        if (icon != null) {
            DrawableCompat.setTint(icon, fg);
        }
        label = context.getString(R.string.swipe_delete);
        radius = cornerRadiusDp * density;
        iconSize = Math.round(24 * density);
        margin = Math.round(20 * density);
    }

    /** Return false for rows that cannot be swiped (headers, footers). */
    protected boolean canSwipe(@NonNull RecyclerView.ViewHolder holder) {
        return true;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder holder) {
        return canSwipe(holder) ? super.getSwipeDirs(recyclerView, holder) : 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder a,
                          @NonNull RecyclerView.ViewHolder b) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder holder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        View item = holder.itemView;
        if (dX < 0) {
            rect.set(item.getLeft(), item.getTop(), item.getRight(), item.getBottom());
            c.save();
            c.clipRect(item.getRight() + dX - radius, item.getTop(), item.getRight(), item.getBottom());
            c.drawRoundRect(rect, radius, radius, background);
            int centerY = (item.getTop() + item.getBottom()) / 2;
            int iconRight = item.getRight() - margin;
            int iconLeft = iconRight - iconSize;
            if (icon != null) {
                icon.setBounds(iconLeft, centerY - iconSize / 2, iconRight, centerY + iconSize / 2);
                icon.draw(c);
            }
            float textWidth = text.measureText(label);
            float baseline = centerY - (text.descent() + text.ascent()) / 2;
            c.drawText(label, iconLeft - margin / 2f - textWidth, baseline, text);
            c.restore();
        }
        super.onChildDraw(c, recyclerView, holder, dX, dY, actionState, isCurrentlyActive);
    }
}
