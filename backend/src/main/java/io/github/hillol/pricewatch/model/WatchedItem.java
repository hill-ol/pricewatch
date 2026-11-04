package io.github.hillol.pricewatch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import java.math.BigDecimal;
import jakarta.persistence.Id;

@Entity
public class WatchedItem {

  @Id
  @GeneratedValue
  private Long id;
  private String storeDomain;
  private String variantId;
  private String name;
  private BigDecimal targetPrice;
  private BigDecimal lastPrice;
  private boolean alerted;
}