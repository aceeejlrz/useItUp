package com.team15.useitup.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Lives in the same transaction and backup as the seed data, even when every list is empty. */
@Entity(tableName = "seed_state")
public class SeedState {
    @PrimaryKey public int id = 1;
}
