package com.staynest.fb.enums;

/**
 * Whether a dish is vegetarian, which guests filter on before anything else.
 *
 * Kept separate from the free-text {@code dietaryTags} on purpose: tags carry the extras
 * (jain, gluten-free, contains nuts), while this is the one attribute the menu must state
 * unambiguously for every item.
 */
public enum FoodType {
    VEG,
    NONVEG,
    EGG
}
