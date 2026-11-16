package io.github.hillol.pricewatch.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WatchedItemTest {

  private static WatchedItem item(String target, String lastPrice, boolean alerted) {
    WatchedItem item = new WatchedItem(
        "example.myshopify.com", "gid://shopify/ProductVariant/1", "Blanket",
        new BigDecimal(target), "https://example.com/p");
    if (lastPrice != null) item.setLastPrice(new BigDecimal(lastPrice));
    item.setAlerted(alerted);
    return item;
  }

  @Test
  void loweringTargetBelowCurrentPriceReArmsAlert() {
    WatchedItem item = item("350.00", "325.00", true);

    item.updateTargetPrice(new BigDecimal("300.00"));

    assertThat(item.getTargetPrice()).isEqualByComparingTo("300.00");
    assertThat(item.isAlerted()).isFalse();
  }

  @Test
  void raisingTargetAbovePriceLeavesUnsentAlertForNextCheck() {
    WatchedItem item = item("250.00", "275.00", false);

    item.updateTargetPrice(new BigDecimal("300.00"));

    assertThat(item.getTargetPrice()).isEqualByComparingTo("300.00");
    assertThat(item.isAlerted()).isFalse();
  }

  @Test
  void stayingAtTargetKeepsAlertSoNoDuplicateEmail() {
    WatchedItem item = item("350.00", "325.00", true);

    item.updateTargetPrice(new BigDecimal("340.00"));

    assertThat(item.isAlerted()).isTrue();
  }

  @Test
  void updatingBeforeFirstCheckJustChangesTarget() {
    WatchedItem item = item("100.00", null, false);

    item.updateTargetPrice(new BigDecimal("90.00"));

    assertThat(item.getTargetPrice()).isEqualByComparingTo("90.00");
    assertThat(item.isAlerted()).isFalse();
  }
}
