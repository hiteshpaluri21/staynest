package com.staynest.fb.repository;

import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findByCategory(MenuCategory category);
    List<MenuItem> findByIsAvailable(Boolean isAvailable);
}