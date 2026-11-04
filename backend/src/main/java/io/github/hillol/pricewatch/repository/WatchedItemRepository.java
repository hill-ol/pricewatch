package io.github.hillol.pricewatch.repository;

import io.github.hillol.pricewatch.model.WatchedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchedItemRepository extends JpaRepository<WatchedItem, Long> {
}