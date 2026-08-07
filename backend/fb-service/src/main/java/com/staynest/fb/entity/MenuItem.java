package com.staynest.fb.entity;

import com.staynest.fb.enums.FoodType;
import com.staynest.fb.enums.MenuCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MenuItemID")
    private Integer menuItemId;

    @Column(name = "Name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "Category", nullable = false)
    private MenuCategory category;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Nullable because items created before this column existed have no value — the menu
     * shows those as unspecified rather than guessing that they are vegetarian.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "FoodType", length = 10)
    private FoodType foodType;

    @Column(name = "IsAvailable")
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "DietaryTags", length = 200)
    private String dietaryTags;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}