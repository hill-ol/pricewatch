package io.github.hillol.pricewatch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import org.hibernate.annotations.ColumnDefault;

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

  private String productUrl;

  /**
   * Optimistic lock: every save bumps this, and saving a copy loaded before someone else's
   * save fails instead of silently overwriting their change. The column default lets
   * Hibernate add the column to an existing database (existing rows start at 0).
   */
  @Version
  @ColumnDefault("0")
  @Column(nullable = false)
  private long version;

  protected WatchedItem() {
    // required by JPA
  }

  public WatchedItem(String storeDomain, String variantId, String name, BigDecimal targetPrice, String productUrl) {
    this.storeDomain = storeDomain;
    this.variantId = variantId;
    this.name = name;
    this.targetPrice = targetPrice;
    this.productUrl = productUrl;
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

  public String getProductUrl() {
    return productUrl;
  }

  public void setTargetPrice(BigDecimal targetPrice) {
    this.targetPrice = targetPrice;
  }

  /**
   * Changes the target and keeps the alert state consistent with it. If the last known
   * price is now above the new target, the alert is re-armed so the next drop to the new
   * target sends an email. Otherwise the alert state is left alone, which avoids a
   * duplicate email when the item was already alerted and is still at target.
   */
  public void updateTargetPrice(BigDecimal newTarget) {
    this.targetPrice = newTarget;
    if (lastPrice != null && lastPrice.compareTo(newTarget) > 0) {
      this.alerted = false;
    }
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