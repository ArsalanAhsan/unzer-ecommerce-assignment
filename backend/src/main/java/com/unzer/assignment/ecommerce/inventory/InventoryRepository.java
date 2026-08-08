package com.unzer.assignment.ecommerce.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Inventory i
        SET i.availableQuantity = i.availableQuantity - :quantity,
            i.reservedQuantity = i.reservedQuantity + :quantity
        WHERE i.productId = :productId
          AND i.availableQuantity >= :quantity
    """)
    int reserve(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Inventory i
        SET i.availableQuantity = i.availableQuantity + :quantity,
            i.reservedQuantity = i.reservedQuantity - :quantity
        WHERE i.productId = :productId
          AND i.reservedQuantity >= :quantity
    """)
    int release(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Inventory i
        SET i.reservedQuantity = i.reservedQuantity - :quantity
        WHERE i.productId = :productId
          AND i.reservedQuantity >= :quantity
    """)
    int confirm(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}