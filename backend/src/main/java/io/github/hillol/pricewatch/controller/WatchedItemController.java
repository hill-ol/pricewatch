package io.github.hillol.pricewatch.controller;

import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
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
  public WatchedItem create(@Valid @RequestBody CreateItemRequest request) {
    WatchedItem item = new WatchedItem(
        request.storeDomain(),
        request.variantId(),
        request.name(),
        request.targetPrice(),
        request.productUrl());
    return repository.save(item);
  }

  @GetMapping("/{id}/history")
  public List<PricePoint> history(@PathVariable Long id) {
    return priceCheckRepository.findByWatchedItemOrderByCheckedAtAsc(findItem(id)).stream()
        .map(check -> new PricePoint(check.getPrice(), check.getCheckedAt()))
        .toList();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void delete(@PathVariable Long id) {
    WatchedItem item = findItem(id);
    priceCheckRepository.deleteByWatchedItem(item);
    repository.delete(item);
  }

  private WatchedItem findItem(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No item with id " + id));
  }

  public record CreateItemRequest(
      @NotBlank String storeDomain,
      @NotBlank String variantId,
      @NotBlank String name,
      @NotNull @Positive BigDecimal targetPrice,
      @NotBlank String productUrl) {}

  public record PricePoint(BigDecimal price, Instant checkedAt) {}
}
