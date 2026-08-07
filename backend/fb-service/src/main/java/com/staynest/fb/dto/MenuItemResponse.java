package com.staynest.fb.dto;

import com.staynest.fb.enums.FoodType;
import com.staynest.fb.enums.MenuCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MenuItemResponse {
    private Integer menuItemId;
    private String name;
    private MenuCategory category;
    private BigDecimal price;
    private FoodType foodType;
    private Boolean isAvailable;
    private String dietaryTags;
}