package com.staynest.fb.repository;

import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findByCategory(MenuCategory category);
    List<MenuItem> findByIsAvailable(Boolean isAvailable);

    /**
     * Used to keep the menu free of duplicates. Case-insensitive, because "Masala Dosa"
     * and "masala dosa" are the same dish to a guest reading the menu.
     */
    Optional<MenuItem> findByNameIgnoreCase(String name);
}