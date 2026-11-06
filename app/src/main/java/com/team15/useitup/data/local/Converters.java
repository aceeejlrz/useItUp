package com.team15.useitup.data.local;

import androidx.room.TypeConverter;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Dates are ISO-8601; instructions retain order, newlines and punctuation as JSON. */
public final class Converters {
    @TypeConverter
    public static String dateToString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    public static LocalDate stringToDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    @TypeConverter
    public static String stepsToJson(List<String> steps) {
        return new JSONArray(steps).toString();
    }

    @TypeConverter
    public static List<String> jsonToSteps(String value) {
        try {
            JSONArray array = new JSONArray(value);
            List<String> steps = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                steps.add(array.getString(i));
            }
            return steps;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid saved recipe instructions", e);
        }
    }
}
