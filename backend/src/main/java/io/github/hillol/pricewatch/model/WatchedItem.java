package io.github.hillol.pricewatch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;

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

  protected WatchedItem() {
    // required by JPA
  }

  public WatchedItem(String storeDomain, String variantId, String name, BigDecimal targetPrice) {
    this.storeDomain = storeDomain;
    this.variantId = variantId;
    this.name = name;
    this.targetPrice = targetPrice;
  }

  public Long getId() {
    return id;
  }

  public String getStoreDomain() {
    return storeDomain;
  }

  public String getVariantId() {
    return variantId;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getTargetPrice() {
    return targetPrice;
  }

  public void setTargetPrice(BigDecimal targetPrice) {
    this.targetPrice = targetPrice;
  }

  public BigDecimal getLastPrice() {
    return lastPrice;
  }

  public void setLastPrice(BigDecimal lastPrice) {
    this.lastPrice = lastPrice;
  }

  public boolean isAlerted() {
    return alerted;
  }

  public void setAlerted(boolean alerted) {
    this.alerted = alerted;
  }
}