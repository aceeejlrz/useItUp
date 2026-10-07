package com.team15.useitup.util;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Locale;

/**
 * Compares pantry item names with recipe ingredient names. Names are lower-cased, trimmed and
 * reduced to a simple singular so "Chicken Breasts" matches "chicken breast".
 */
public final class IngredientMatcher {

    private IngredientMatcher() {
    }

    @NonNull
    public static String normalize(@NonNull String name) {
        String s = name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (s.isEmpty()) {
            return s;
        }
        int lastSpace = s.lastIndexOf(' ');
        String head = lastSpace >= 0 ? s.substring(0, lastSpace + 1) : "";
        String last = lastSpace >= 0 ? s.substring(lastSpace + 1) : s;
        return head + singular(last);
    }

    @NonNull
    static String singular(@NonNull String word) {
        if (word.length() > 4 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.length() > 4 && word.endsWith("oes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    public static boolean matches(@NonNull String a, @NonNull String b) {
        return normalize(a).equals(normalize(b));
    }

    public static boolean containsMatch(@NonNull Collection<String> names, @NonNull String candidate) {
        String key = normalize(candidate);
        for (String name : names) {
            if (normalize(name).equals(key)) {
                return true;
            }
        }
        return false;
    }
}
