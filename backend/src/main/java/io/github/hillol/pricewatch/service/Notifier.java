package io.github.hillol.pricewatch.service;

import io.github.hillol.pricewatch.model.WatchedItem;
import java.math.BigDecimal;

public interface Notifier {
  void sendPriceDrop(WatchedItem item, BigDecimal newPrice);
}