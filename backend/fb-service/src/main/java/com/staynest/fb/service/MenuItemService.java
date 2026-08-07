package com.staynest.fb.service;

import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.enums.MenuCategory;

import java.util.List;
import java.math.BigDecimal;

public interface MenuItemService {
    MenuItemResponse addMenuItem(MenuItemRequest request);
    MenuItemResponse updateMenuItem(Integer id, MenuItemRequest request);
    void deleteMenuItem(Integer id);
    MenuItemResponse getMenuItemById(Integer id);
    List<MenuItemResponse> getAllMenuItems();
    List<MenuItemResponse> getMenuItemsByCategory(MenuCategory category);
    List<MenuItemResponse> getAvailableMenuItems();
    MenuItemResponse updateAvailability(Integer id, Boolean isAvailable);
    MenuItemResponse updatePrice(Integer id, BigDecimal price);
}