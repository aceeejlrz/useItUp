package com.team15.useitup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.team15.useitup.util.IngredientMatcher;
import com.team15.useitup.util.TextLists;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class IngredientMatcherTest {

    @Test
    public void normalize_lowercasesTrimsAndCollapsesSpaces() {
        assertEquals("chicken breast", IngredientMatcher.normalize("  Chicken   Breast "));
    }

    @Test
    public void normalize_reducesSimplePlurals() {
        assertEquals("chicken breast", IngredientMatcher.normalize("Chicken breasts"));
        assertEquals("carrot", IngredientMatcher.normalize("Carrots"));
        assertEquals("tomato", IngredientMatcher.normalize("Tomatoes"));
        assertEquals("cherry", IngredientMatcher.normalize("Cherries"));
        assertEquals("egg", IngredientMatcher.normalize("Eggs"));
    }

    @Test
    public void normalize_keepsWordsThatAreNotPlurals() {
        assertEquals("hummus", IngredientMatcher.normalize("Hummus"));
        assertEquals("watercress", IngredientMatcher.normalize("Watercress"));
        assertEquals("greek yogurt", IngredientMatcher.normalize("Greek yogurt"));
    }

    @Test
    public void matches_ignoresCaseAndPlurals() {
        assertTrue(IngredientMatcher.matches("Chicken Breast", "chicken breasts"));
        assertFalse(IngredientMatcher.matches("Chicken", "Chicken breast"));
    }

    @Test
    public void containsMatch_findsPantryNames() {
        assertTrue(IngredientMatcher.containsMatch(Arrays.asList("Spinach", "Milk"), "spinach"));
        assertFalse(IngredientMatcher.containsMatch(Arrays.asList("Spinach", "Milk"), "Garlic"));
    }

    @Test
    public void sentenceList_buildsTheNotificationText() {
        assertEquals("Spinach, chicken breast and milk", TextLists.sentenceList(
                Arrays.asList("Spinach", "Chicken breast", "Milk"), ", ", " and "));
        assertEquals("Milk", TextLists.sentenceList(Collections.singletonList("Milk"), ", ", " and "));
    }
}
