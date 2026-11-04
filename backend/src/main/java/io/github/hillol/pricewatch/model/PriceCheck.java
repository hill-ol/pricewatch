package io.github.hillol.pricewatch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class PriceCheck {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watched_item_id")
  private WatchedItem watchedItem;

  private BigDecimal price;

  private Instant checkedAt;

  protected PriceCheck() {
    // required by JPA
  }

  public PriceCheck(WatchedItem watchedItem, BigDecimal price, Instant checkedAt) {
    this.watchedItem = watchedItem;
    this.price = price;
    this.checkedAt = checkedAt;
  }

  public Long getId() {
    return id;
  }

  public WatchedItem getWatchedItem() {
    return watchedItem;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public Instant getCheckedAt() {
    return checkedAt;
  }
}