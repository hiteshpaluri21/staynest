package com.staynest.fb.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.MenuCategory;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.FBOrderRepository;
import com.staynest.fb.repository.MenuItemRepository;
import com.staynest.fb.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "MENUITEM";

    private final MenuItemRepository menuItemRepository;
    /** Read-only here, to check whether a dish is still on an unfinished order before deleting it. */
    private final FBOrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final AuditRecorder auditRecorder;

    @Override
    @Transactional
    public MenuItemResponse addMenuItem(MenuItemRequest request) {
        String name = normaliseName(request.getName());
        rejectDuplicateName(name, null);

        MenuItem item = MenuItem.builder()
                .name(name)
                .category(request.getCategory())
                .price(request.getPrice())
                .foodType(request.getFoodType())
                .dietaryTags(request.getDietaryTags())
                .build();
        MenuItem saved = menuItemRepository.save(item);
        log.info("MenuItem created: {}", saved.getMenuItemId());
        auditRecorder.record("CREATE", ENTITY, saved.getMenuItemId());
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
        item.setFoodType(request.getFoodType());
        item.setDietaryTags(request.getDietaryTags());
        MenuItem updated = menuItemRepository.save(item);
        log.info("MenuItem {} updated", id);
        auditRecorder.record("UPDATE", ENTITY, id);
        return mapToResponse(updated);
    }

    /**
     * Removes an item from the menu for good.
     *
     * Refused while the item sits on an order the kitchen has not finished with, because those
     * orders are still being worked and read their lines back off the menu. Orders that are
     * done, billed or cancelled do not block it: their stored total is authoritative and
     * FBOrderServiceImpl already renders a missing item as "Item #n".
     *
     * To retire a dish without losing that history, mark it unavailable instead — it then
     * disappears from the ordering screens but every past order still reads correctly.
     */
    @Override
    @Transactional
    public void deleteMenuItem(Integer id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));

        long openOrders = orderRepository.findByStatusIn(UNFINISHED_ORDER_STATUSES).stream()
                .filter(order -> orderReferences(order.getItemsJson(), id))
                .count();
        if (openOrders > 0) {
            throw new BadRequestException("\"" + item.getName() + "\" is on " + openOrders
                    + " order(s) the kitchen has not finished. Mark it unavailable instead, or "
                    + "delete it once those orders are served.");
        }

        menuItemRepository.delete(item);
        log.info("MenuItem {} ({}) deleted", id, item.getName());
        auditRecorder.record("DELETE", ENTITY, id);
    }

    /** Orders still in flight, whose lines are read back off the menu as staff work them. */
    private static final List<OrderStatus> UNFINISHED_ORDER_STATUSES =
            List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.SERVED);

    /**
     * Whether a stored cart references this menu item. An unparseable payload counts as a
     * reference — better to refuse a delete than to break an order we could not read.
     */
    private boolean orderReferences(String itemsJson, Integer menuItemId) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return false;
        }
        try {
            List<Map<String, Object>> lines =
                    objectMapper.readValue(itemsJson, new TypeReference<List<Map<String, Object>>>() {});
            return lines.stream()
                    .map(line -> line.get("itemId"))
                    .filter(Number.class::isInstance)
                    .anyMatch(rawId -> ((Number) rawId).intValue() == menuItemId);
        } catch (Exception e) {
            log.warn("Could not parse itemsJson while checking references to menu item {}: {}",
                    menuItemId, e.getMessage());
            return true;
        }
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
        auditRecorder.record("UPDATE_AVAILABILITY", ENTITY, id);
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
        auditRecorder.record("UPDATE_PRICE", ENTITY, id);
        return mapToResponse(updated);
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .category(item.getCategory())
                .price(item.getPrice())
                .foodType(item.getFoodType())
                .isAvailable(item.getIsAvailable())
                .dietaryTags(item.getDietaryTags())
                .build();
    }
}