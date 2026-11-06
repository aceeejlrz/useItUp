package com.team15.useitup.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.team15.useitup.data.PantryItem;
import java.util.List;

@Dao
public abstract class PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY expiryDate, name COLLATE NOCASE, id")
    public abstract List<PantryItem> getAll();

    @Insert
    public abstract void insertAll(List<PantryItem> items);

    @Query("DELETE FROM pantry_items")
    public abstract void deleteAll();

    @Transaction
    public void replaceAll(List<PantryItem> items) {
        deleteAll();
        insertAll(items);
    }
}
