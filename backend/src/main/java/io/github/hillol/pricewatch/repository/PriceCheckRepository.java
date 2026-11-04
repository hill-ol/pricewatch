package io.github.hillol.pricewatch.repository;

import io.github.hillol.pricewatch.model.PriceCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCheckRepository extends JpaRepository<PriceCheck, Long> {
}