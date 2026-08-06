package com.staynest.fb.serviceimpl;

import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.MenuCategory;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.MenuItemRepository;
import com.staynest.fb.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public MenuItemResponse addMenuItem(MenuItemRequest request) {
        String name = normaliseName(request.getName());
        rejectDuplicateName(name, null);

        MenuItem item = MenuItem.builder()
                .name(name)
                .category(request.getCategory())
                .price(request.getPrice())
                .dietaryTags(request.getDietaryTags())
                .build();
        MenuItem saved = menuItemRepository.save(item);
        log.info("MenuItem created: {}", saved.getMenuItemId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Integer id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));

        String name = normaliseName(request.getName());
        // Excluding this item, or renaming it to its own name would look like a clash.
        rejectDuplicateName(name, id);

        item.setName(name);
        item.setCategory(request.getCategory());
        item.setPrice(request.getPrice());
        item.setDietaryTags(request.getDietaryTags());
        MenuItem updated = menuItemRepository.save(item);
        log.info("MenuItem {} updated", id);
        return mapToResponse(updated);
    }

    /**
     * Stored trimmed and collapsed, so " Masala  Dosa " can't sneak past the duplicate
     * check as a distinct dish from "Masala Dosa".
     */
    private String normaliseName(String raw) {
        return raw == null ? null : raw.trim().replaceAll("\\s+", " ");
    }

    /**
     * The menu is a guest-facing list, so a name appears at most once across the whole
     * menu — not just within a category. Two "Club Sandwich" rows filed under different
     * categories still read as a duplicate.
     *
     * @param exceptId the item being updated, which must not clash with itself
     */
    private void rejectDuplicateName(String name, Integer exceptId) {
        menuItemRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getMenuItemId().equals(exceptId)) {
                throw new BadRequestException("A menu item named \"" + existing.getName()
                        + "\" already exists (" + existing.getCategory() + ")");
            }
        });
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
    @Transactional
    public MenuItemResponse updateAvailability(Integer id, Boolean isAvailable) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        item.setIsAvailable(isAvailable);
        MenuItem updated = menuItemRepository.save(item);
        log.info("MenuItem {} availability updated to {}", id, isAvailable);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
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