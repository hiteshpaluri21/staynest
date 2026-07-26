package com.staynest.fb.serviceimpl;

import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.MenuCategory;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.MenuItemRepository;
import com.staynest.fb.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;

    @Override
    public MenuItemResponse addMenuItem(MenuItemRequest request) {
        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .dietaryTags(request.getDietaryTags())
                .build();
        MenuItem saved = menuItemRepository.save(item);
        log.info("MenuItem created: {}", saved.getMenuItemId());
        return mapToResponse(saved);
    }

    @Override
    public MenuItemResponse getMenuItemById(Integer id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        return mapToResponse(item);
    }

    @Override
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MenuItemResponse> getMenuItemsByCategory(MenuCategory category) {
        return menuItemRepository.findByCategory(category).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MenuItemResponse> getAvailableMenuItems() {
        return menuItemRepository.findByIsAvailable(true).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public MenuItemResponse updateAvailability(Integer id, Boolean isAvailable) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        item.setIsAvailable(isAvailable);
        MenuItem updated = menuItemRepository.save(item);
        log.info("MenuItem {} availability updated to {}", id, isAvailable);
        return mapToResponse(updated);
    }

    @Override
    public MenuItemResponse updatePrice(Integer id, BigDecimal price) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        item.setPrice(price);
        MenuItem updated = menuItemRepository.save(item);
        log.info("MenuItem {} price updated to {}", id, price);
        return mapToResponse(updated);
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .category(item.getCategory())
                .price(item.getPrice())
                .isAvailable(item.getIsAvailable())
                .dietaryTags(item.getDietaryTags())
                .build();
    }
}