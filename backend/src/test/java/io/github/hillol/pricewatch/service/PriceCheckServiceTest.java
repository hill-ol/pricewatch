package io.github.hillol.pricewatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriceCheckServiceTest {

  @Mock private WatchedItemRepository watchedItemRepository;
  @Mock private PriceCheckRepository priceCheckRepository;
  @Mock private PriceSource priceSource;
  @Mock private Notifier notifier;

  @InjectMocks private PriceCheckService service;

  private static final AtomicLong ids = new AtomicLong();

  private static WatchedItem item(String name, String targetPrice) {
    WatchedItem item = new WatchedItem(
        "example.myshopify.com",
        "gid://shopify/ProductVariant/" + name,
        name,
        new BigDecimal(targetPrice),
        "https://example.com/products/" + name);
    // Ids are normally assigned by the database; the service reloads items by id.
    ReflectionTestUtils.setField(item, "id", ids.incrementAndGet());
    return item;
  }

  /** The check loads these items, then reloads each by id before saving (unchanged here). */
  private void givenWatched(WatchedItem... items) {
    when(watchedItemRepository.findAll()).thenReturn(List.of(items));
    for (WatchedItem item : items) {
      // lenient: an item whose price fetch fails is never reloaded.
      lenient().when(watchedItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
    }
  }

  private void givenCurrentPrice(WatchedItem item, String price) {
    when(priceSource.fetchPrice(item.getStoreDomain(), item.getVariantId()))
        .thenReturn(new BigDecimal(price));
  }

  @Test
  void sendsAlertWhenPriceDropsToTarget() {
    WatchedItem item = item("blanket", "100.00");
    givenWatched(item);
    givenCurrentPrice(item, "100.00");

    service.checkAll();

    verify(notifier).sendPriceDrop(item, new BigDecimal("100.00"));
    assertThat(item.isAlerted()).isTrue();
    assertThat(item.getLastPrice()).isEqualByComparingTo("100.00");
    verify(priceCheckRepository).save(any(PriceCheck.class));
    verify(watchedItemRepository).save(item);
  }

  @Test
  void doesNotAlertWhenPriceIsAboveTarget() {
    WatchedItem item = item("blanket", "100.00");
    givenWatched(item);
    givenCurrentPrice(item, "120.00");

    service.checkAll();

    verifyNoInteractions(notifier);
    assertThat(item.isAlerted()).isFalse();
    assertThat(item.getLastPrice()).isEqualByComparingTo("120.00");
  }

  @Test
  void doesNotAlertTwiceForSameDrop() {
    WatchedItem item = item("blanket", "100.00");
    item.setAlerted(true);
    givenWatched(item);
    givenCurrentPrice(item, "90.00");

    service.checkAll();

    verifyNoInteractions(notifier);
    assertThat(item.isAlerted()).isTrue();
  }

  @Test
  void resetsAlertWhenPriceRisesAboveTarget() {
    WatchedItem item = item("blanket", "100.00");
    item.setAlerted(true);
    givenWatched(item);
    givenCurrentPrice(item, "110.00");

    service.checkAll();

    verifyNoInteractions(notifier);
    assertThat(item.isAlerted()).isFalse();
  }

  @Test
  void leavesAlertUnsentWhenNotifierFailsSoNextRunRetries() {
    WatchedItem item = item("blanket", "100.00");
    givenWatched(item);
    givenCurrentPrice(item, "90.00");
    doThrow(new RuntimeException("SMTP down")).when(notifier).sendPriceDrop(any(), any());

    service.checkAll();

    assertThat(item.isAlerted()).isFalse();
    assertThat(item.getLastPrice()).isEqualByComparingTo("90.00");
    verify(watchedItemRepository).save(item);
  }

  @Test
  void oneFailingItemDoesNotStopTheOthers() {
    WatchedItem broken = item("broken", "100.00");
    WatchedItem working = item("working", "100.00");
    givenWatched(broken, working);
    when(priceSource.fetchPrice(broken.getStoreDomain(), broken.getVariantId()))
        .thenThrow(new PriceFetchException("store is down"));
    givenCurrentPrice(working, "90.00");

    service.checkAll();

    verify(notifier).sendPriceDrop(working, new BigDecimal("90.00"));
    verify(notifier, never()).sendPriceDrop(eq(broken), any());
    verify(watchedItemRepository, never()).save(broken);
  }

  @Test
  void appliesPriceToFreshCopySoATargetChangeDuringTheCheckIsKept() {
    WatchedItem snapshot = item("blanket", "100.00");
    when(watchedItemRepository.findAll()).thenReturn(List.of(snapshot));
    givenCurrentPrice(snapshot, "90.00");
    // While the price was being fetched, the user lowered the target to 80.
    WatchedItem fresh = item("blanket", "80.00");
    ReflectionTestUtils.setField(fresh, "id", snapshot.getId());
    when(watchedItemRepository.findById(snapshot.getId())).thenReturn(Optional.of(fresh));

    service.checkAll();

    verify(watchedItemRepository).save(fresh);
    verify(watchedItemRepository, never()).save(snapshot);
    assertThat(fresh.getTargetPrice()).isEqualByComparingTo("80.00");
    assertThat(fresh.getLastPrice()).isEqualByComparingTo("90.00");
    verifyNoInteractions(notifier);
  }

  @Test
  void skipsItemRemovedDuringTheCheck() {
    WatchedItem item = item("blanket", "100.00");
    when(watchedItemRepository.findAll()).thenReturn(List.of(item));
    givenCurrentPrice(item, "90.00");
    when(watchedItemRepository.findById(item.getId())).thenReturn(Optional.empty());

    service.checkAll();

    verifyNoInteractions(priceCheckRepository, notifier);
    verify(watchedItemRepository, never()).save(any());
  }
}
