package io.github.hillol.pricewatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceCheckServiceTest {

  @Mock private WatchedItemRepository watchedItemRepository;
  @Mock private PriceCheckRepository priceCheckRepository;
  @Mock private PriceSource priceSource;
  @Mock private Notifier notifier;

  @InjectMocks private PriceCheckService service;

  private static WatchedItem item(String name, String targetPrice) {
    return new WatchedItem(
        "example.myshopify.com",
        "gid://shopify/ProductVariant/" + name,
        name,
        new BigDecimal(targetPrice),
        "https://example.com/products/" + name);
  }

  private void givenCurrentPrice(WatchedItem item, String price) {
    when(priceSource.fetchPrice(item.getStoreDomain(), item.getVariantId()))
        .thenReturn(new BigDecimal(price));
  }

  @Test
  void sendsAlertWhenPriceDropsToTarget() {
    WatchedItem item = item("blanket", "100.00");
    when(watchedItemRepository.findAll()).thenReturn(List.of(item));
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
    when(watchedItemRepository.findAll()).thenReturn(List.of(item));
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
    when(watchedItemRepository.findAll()).thenReturn(List.of(item));
    givenCurrentPrice(item, "90.00");

    service.checkAll();

    verifyNoInteractions(notifier);
    assertThat(item.isAlerted()).isTrue();
  }

  @Test
  void resetsAlertWhenPriceRisesAboveTarget() {
    WatchedItem item = item("blanket", "100.00");
    item.setAlerted(true);
    when(watchedItemRepository.findAll()).thenReturn(List.of(item));
    givenCurrentPrice(item, "110.00");

    service.checkAll();

    verifyNoInteractions(notifier);
    assertThat(item.isAlerted()).isFalse();
  }

  @Test
  void leavesAlertUnsentWhenNotifierFailsSoNextRunRetries() {
    WatchedItem item = item("blanket", "100.00");
    when(watchedItemRepository.findAll()).thenReturn(List.of(item));
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
    when(watchedItemRepository.findAll()).thenReturn(List.of(broken, working));
    when(priceSource.fetchPrice(broken.getStoreDomain(), broken.getVariantId()))
        .thenThrow(new PriceFetchException("store is down"));
    givenCurrentPrice(working, "90.00");

    service.checkAll();

    verify(notifier).sendPriceDrop(working, new BigDecimal("90.00"));
    verify(notifier, never()).sendPriceDrop(eq(broken), any());
    verify(watchedItemRepository, never()).save(broken);
  }
}
