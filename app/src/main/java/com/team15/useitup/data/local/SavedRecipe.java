package com.team15.useitup.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.List;

/** Complete recipe metadata. Resource IDs are deliberately never persisted. */
@Entity(tableName = "SavedRecipe")
public class SavedRecipe {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String name = "";
    @NonNull public String area = "";
    @NonNull public String category = "";
    @Nullable public String imageUrl;
    @NonNull public List<String> steps = java.util.Collections.emptyList();
    public int savedOrder;
}
