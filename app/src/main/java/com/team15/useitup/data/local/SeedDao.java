package com.team15.useitup.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SeedDao {
    @Query("SELECT EXISTS(SELECT 1 FROM seed_state WHERE id = 1)")
    boolean isSeeded();

    @Insert
    void markSeeded(SeedState state);
}
