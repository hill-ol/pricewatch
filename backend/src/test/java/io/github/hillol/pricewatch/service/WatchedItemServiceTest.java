package io.github.hillol.pricewatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Runs the service against a real in-memory database, so queries and cascades are exercised. */
@DataJpaTest
@Import(WatchedItemService.class)
class WatchedItemServiceTest {

  @Autowired private WatchedItemService service;
  @Autowired private WatchedItemRepository items;
  @Autowired private PriceCheckRepository priceChecks;

  private WatchedItem createBlanket(String name, String targetPrice) {
    return service.create(
        "example.myshopify.com", "gid://shopify/ProductVariant/1", name,
        new BigDecimal(targetPrice), "https://example.com/p");
  }

  private void recordCheck(WatchedItem item, String price, String checkedAt) {
    priceChecks.save(new PriceCheck(item, new BigDecimal(price), Instant.parse(checkedAt)));
  }

  @Test
  void createSavesAndAssignsAnId() {
    WatchedItem created = createBlanket("Blanket", "100.00");

    assertThat(created.getId()).isNotNull();
    assertThat(items.findById(created.getId())).isPresent();
  }

  @Test
  void findAllReturnsItemsInCreationOrder() {
    createBlanket("First", "100.00");
    createBlanket("Second", "100.00");
    createBlanket("Third", "100.00");

    assertThat(service.findAll())
        .extracting(WatchedItem::getName)
        .containsExactly("First", "Second", "Third");
  }

  @Test
  void historyIsOldestFirst() {
    WatchedItem item = createBlanket("Blanket", "100.00");
    recordCheck(item, "95.00", "2026-11-02T00:00:00Z");
    recordCheck(item, "120.00", "2026-11-01T00:00:00Z");

    assertThat(service.history(item.getId()))
        .extracting(PriceCheck::getPrice)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("120.00"), new BigDecimal("95.00"));
  }

  @Test
  void updateTargetPriceSavesTheNewTarget() {
    WatchedItem item = createBlanket("Blanket", "100.00");

    service.updateTargetPrice(item.getId(), new BigDecimal("80.00"));

    assertThat(items.findById(item.getId()).orElseThrow().getTargetPrice())
        .isEqualByComparingTo("80.00");
  }

  @Test
  void deleteRemovesTheItemAndOnlyItsHistory() {
    WatchedItem doomed = createBlanket("Doomed", "100.00");
    WatchedItem kept = createBlanket("Kept", "100.00");
    recordCheck(doomed, "99.00", "2026-11-01T00:00:00Z");
    recordCheck(kept, "98.00", "2026-11-01T00:00:00Z");

    service.delete(doomed.getId());

    assertThat(items.findById(doomed.getId())).isEmpty();
    assertThat(priceChecks.findAll())
        .extracting(check -> check.getWatchedItem().getName())
        .containsExactly("Kept");
  }

  @Test
  void unknownIdThrowsItemNotFoundEverywhere() {
    assertThatThrownBy(() -> service.history(999L)).isInstanceOf(ItemNotFoundException.class);
    assertThatThrownBy(() -> service.updateTargetPrice(999L, BigDecimal.TEN))
        .isInstanceOf(ItemNotFoundException.class);
    assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(ItemNotFoundException.class);
  }
}
