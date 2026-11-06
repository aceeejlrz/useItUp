package com.team15.useitup.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.team15.useitup.data.ShoppingItem;
import java.util.List;

@Dao
public abstract class ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY createdAt, id")
    public abstract List<ShoppingItem> getAll();

    @Insert
    public abstract void insertAll(List<ShoppingItem> items);

    @Query("DELETE FROM shopping_items")
    public abstract void deleteAll();

    @Transaction
    public void replaceAll(List<ShoppingItem> items) {
        deleteAll();
        insertAll(items);
    }
}
