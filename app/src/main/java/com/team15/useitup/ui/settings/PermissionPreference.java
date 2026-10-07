package com.team15.useitup.ui.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.team15.useitup.R;

/** Notification permission row: "Allowed" chip, or "Notifications are blocked" in amber, plus Manage. */
public class PermissionPreference extends Preference {

    private boolean allowed = true;
    @Nullable private View.OnClickListener onManage;

    public PermissionPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pref_permission);
        setSelectable(false);
        setPersistent(false);
    }

    public void setAllowed(boolean value) {
        if (allowed != value) {
            allowed = value;
            notifyChanged();
        }
    }

    public void setOnManageClick(@Nullable View.OnClickListener listener) {
        onManage = listener;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View allowedChip = holder.findViewById(R.id.permission_allowed);
        TextView blocked = (TextView) holder.findViewById(R.id.permission_blocked);
        View manageOutlined = holder.findViewById(R.id.manage_outlined);
        View manageFilled = holder.findViewById(R.id.manage_filled);
        allowedChip.setVisibility(allowed ? View.VISIBLE : View.GONE);
        blocked.setVisibility(allowed ? View.GONE : View.VISIBLE);
        manageOutlined.setVisibility(allowed ? View.VISIBLE : View.GONE);
        manageFilled.setVisibility(allowed ? View.GONE : View.VISIBLE);
        manageOutlined.setOnClickListener(onManage);
        manageFilled.setOnClickListener(onManage);
        View icon = holder.findViewById(android.R.id.icon);
        if (icon instanceof android.widget.ImageView) {
            ((android.widget.ImageView) icon).setImageResource(allowed
                    ? R.drawable.ic_notifications : R.drawable.ic_notifications_off);
        }
    }
}
