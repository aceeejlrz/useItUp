package com.team15.useitup.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.team15.useitup.data.PantryItem;
import com.team15.useitup.data.ShoppingItem;
import com.team15.useitup.data.Recipe;
import com.team15.useitup.data.SampleData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Database(entities = {PantryItem.class, ShoppingItem.class, SavedRecipe.class,
        RecipeIngredient.class, SeedState.class}, version = 1, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class UseItUpDatabase extends RoomDatabase {
    public static final String NAME = "useitup.db";

    public abstract PantryDao pantryDao();
    public abstract ShoppingDao shoppingDao();
    public abstract SavedRecipeDao savedRecipeDao();
    public abstract SeedDao seedDao();

    public static UseItUpDatabase open(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), UseItUpDatabase.class, NAME)
                .build();
    }

    /** Run on the repository executor. Empty lists on subsequent launches stay empty. */
    public void seedOnce() {
        runInTransaction(() -> {
            if (seedDao().isSeeded()) return;
            pantryDao().insertAll(SampleData.pantryItems(LocalDate.now()));
            shoppingDao().insertAll(SampleData.shoppingItems(System.currentTimeMillis()));
            List<Recipe> saved = new ArrayList<>();
            Set<String> ids = SampleData.savedRecipeIds();
            for (String id : ids) {
                for (Recipe recipe : SampleData.recipes()) {
                    if (recipe.getId().equals(id)) saved.add(recipe);
                }
            }
            savedRecipeDao().replaceAll(saved);
            seedDao().markSeeded(new SeedState());
        });
    }
}
