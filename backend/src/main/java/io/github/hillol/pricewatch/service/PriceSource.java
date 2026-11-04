package io.github.hillol.pricewatch.service;

import java.math.BigDecimal;

public interface PriceSource {
  BigDecimal fetchPrice(String storeDomain, String variantId);
}