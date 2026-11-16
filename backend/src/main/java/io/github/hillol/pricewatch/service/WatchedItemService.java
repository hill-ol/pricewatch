package io.github.hillol.pricewatch.service;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything the app can do to watched items. Knows nothing about HTTP: it throws
 * {@link ItemNotFoundException} rather than picking a status code, and the controller
 * decides how that looks to a client.
 */
@Service
public class WatchedItemService {

  private final WatchedItemRepository items;
  private final PriceCheckRepository priceChecks;

  public WatchedItemService(WatchedItemRepository items, PriceCheckRepository priceChecks) {
    this.items = items;
    this.priceChecks = priceChecks;
  }

  /** Oldest first, so the dashboard order is stable between reloads. */
  @Transactional(readOnly = true)
  public List<WatchedItem> findAll() {
    return items.findAll(Sort.by("id"));
  }

  @Transactional
  public WatchedItem create(
      String storeDomain, String variantId, String name, BigDecimal targetPrice, String productUrl) {
    return items.save(new WatchedItem(storeDomain, variantId, name, targetPrice, productUrl));
  }

  /** See {@link WatchedItem#updateTargetPrice} for how this re-arms the alert. */
  @Transactional
  public WatchedItem updateTargetPrice(long id, BigDecimal newTarget) {
    WatchedItem item = find(id);
    item.updateTargetPrice(newTarget);
    return items.save(item);
  }

  /** Price checks first, because each one references the item. */
  @Transactional
  public void delete(long id) {
    WatchedItem item = find(id);
    priceChecks.deleteByWatchedItem(item);
    items.delete(item);
  }

  @Transactional(readOnly = true)
  public List<PriceCheck> history(long id) {
    return priceChecks.findByWatchedItemOrderByCheckedAtAsc(find(id));
  }

  private WatchedItem find(long id) {
    return items.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
  }
}
