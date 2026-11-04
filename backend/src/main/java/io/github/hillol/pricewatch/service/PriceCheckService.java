package io.github.hillol.pricewatch.service;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PriceCheckService {

  private static final Logger log = LoggerFactory.getLogger(PriceCheckService.class);

  private final WatchedItemRepository watchedItemRepository;
  private final PriceCheckRepository priceCheckRepository;
  private final PriceSource priceSource;
  private final Notifier notifier;

  public PriceCheckService(
      WatchedItemRepository watchedItemRepository,
      PriceCheckRepository priceCheckRepository,
      PriceSource priceSource,
      Notifier notifier) {
    this.watchedItemRepository = watchedItemRepository;
    this.priceCheckRepository = priceCheckRepository;
    this.priceSource = priceSource;
    this.notifier = notifier;
  }

  @Scheduled(initialDelayString = "PT10S", fixedDelayString = "${pricewatch.check-interval:PT6H}")
  public void checkAll() {
    List<WatchedItem> items = watchedItemRepository.findAll();
    log.info("Checking {} watched items", items.size());

    for (WatchedItem item : items) {
      try {
        checkOne(item);
      } catch (RuntimeException e) {
        log.warn("Price check failed for {}: {}", item.getName(), e.getMessage());
      }
    }
  }

  private void checkOne(WatchedItem item) {
    BigDecimal price = priceSource.fetchPrice(item.getStoreDomain(), item.getVariantId());
    priceCheckRepository.save(new PriceCheck(item, price, Instant.now()));
    item.setLastPrice(price);
    handleAlert(item, price);
    watchedItemRepository.save(item);
    log.info("{} is ${} (target ${})", item.getName(), price, item.getTargetPrice());
  }

  private void handleAlert(WatchedItem item, BigDecimal price) {
    boolean atOrBelowTarget = price.compareTo(item.getTargetPrice()) <= 0;

    if (atOrBelowTarget && !item.isAlerted()) {
      try {
        notifier.sendPriceDrop(item, price);
        item.setAlerted(true);
        log.info("Alert sent for {}", item.getName());
      } catch (RuntimeException e) {
        log.warn("Alert failed for {}, will retry next run: {}", item.getName(), e.getMessage());
      }
    } else if (!atOrBelowTarget && item.isAlerted()) {
      item.setAlerted(false);
    }
  }
}