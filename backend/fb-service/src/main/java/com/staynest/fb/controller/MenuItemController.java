package com.staynest.fb.controller;

import com.staynest.fb.dto.ApiResponse;
import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.enums.MenuCategory;
import com.staynest.fb.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", menuItemService.addMenuItem(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAll(
            @RequestParam(required = false) MenuCategory category,
            @RequestParam(required = false) Boolean available) {
        if (category != null) {
            return ResponseEntity.ok(ApiResponse.success(menuItemService.getMenuItemsByCategory(category)));
        }
        if (available != null) {
            return ResponseEntity.ok(ApiResponse.success(menuItemService.getAvailableMenuItems()));
        }
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getAllMenuItems()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getMenuItemById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Menu item updated", menuItemService.updateMenuItem(id, request)));
    }

    // Refused while the dish is on an unfinished order — see MenuItemServiceImpl#deleteMenuItem.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted", null));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateAvailability(
            @PathVariable Integer id,
            @RequestParam Boolean value) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.updateAvailability(id, value)));
    }

    @PatchMapping("/{id}/price")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updatePrice(
            @PathVariable Integer id,
            @RequestParam BigDecimal value) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.updatePrice(id, value)));
    }
}