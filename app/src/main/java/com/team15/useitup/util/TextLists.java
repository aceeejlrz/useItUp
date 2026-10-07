package com.team15.useitup.util;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

/** Small text helpers for lists of names. */
public final class TextLists {

    private TextLists() {
    }

    /** "A", "A and B", "A, B and C". */
    @NonNull
    public static String joinNatural(@NonNull List<String> parts, @NonNull String separator,
                                     @NonNull String lastSeparator) {
        int n = parts.size();
        if (n == 0) return "";
        if (n == 1) return parts.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(i == n - 1 ? lastSeparator : separator);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /** "Spinach, chicken breast and milk": the first name keeps its capital, the rest are lower case. */
    @NonNull
    public static String sentenceList(@NonNull List<String> names, @NonNull String separator,
                                      @NonNull String lastSeparator) {
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            parts.add(i == 0 ? name : name.toLowerCase(Locale.getDefault()));
        }
        return joinNatural(parts, separator, lastSeparator);
    }
}
