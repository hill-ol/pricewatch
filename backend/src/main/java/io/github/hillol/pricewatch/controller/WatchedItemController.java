package io.github.hillol.pricewatch.controller;

import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/items")
public class WatchedItemController {

  private final WatchedItemRepository repository;
  private final PriceCheckRepository priceCheckRepository;

  public WatchedItemController(
      WatchedItemRepository repository,
      PriceCheckRepository priceCheckRepository) {
    this.repository = repository;
    this.priceCheckRepository = priceCheckRepository;
  }

  @GetMapping
  public List<WatchedItem> list() {
    return repository.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WatchedItem create(@RequestBody CreateItemRequest request) {
    if (request.targetPrice() == null || request.targetPrice().signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetPrice must be positive");
    }
    WatchedItem item = new WatchedItem(
        request.storeDomain(),
        request.variantId(),
        request.name(),
        request.targetPrice(),
        request.productUrl());
    return repository.save(item);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void delete(@PathVariable Long id) {
    WatchedItem item = repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No item with id " + id));
    priceCheckRepository.deleteByWatchedItem(item);
    repository.delete(item);
  }

  public record CreateItemRequest(
      String storeDomain,
      String variantId,
      String name,
      BigDecimal targetPrice,
      String productUrl) {}
}