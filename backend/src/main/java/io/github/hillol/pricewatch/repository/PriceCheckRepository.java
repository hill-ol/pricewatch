package io.github.hillol.pricewatch.repository;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCheckRepository extends JpaRepository<PriceCheck, Long> {

  void deleteByWatchedItem(WatchedItem watchedItem);

  List<PriceCheck> findByWatchedItemOrderByCheckedAtAsc(WatchedItem watchedItem);
}