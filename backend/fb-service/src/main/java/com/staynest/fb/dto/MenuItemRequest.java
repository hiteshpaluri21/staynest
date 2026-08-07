package com.staynest.fb.dto;

import com.staynest.fb.enums.FoodType;
import com.staynest.fb.enums.MenuCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemRequest {
    @NotBlank
    private String name;
    @NotNull
    private MenuCategory category;
    @NotNull @Positive
    private BigDecimal price;
    /** Required on every item a guest can order — the menu must not be ambiguous about this. */
    @NotNull
    private FoodType foodType;
    private String dietaryTags;
}