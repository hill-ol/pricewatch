package io.github.hillol.pricewatch.controller;

import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

  @PatchMapping("/{id}")
  public WatchedItem updateTarget(@PathVariable Long id, @Valid @RequestBody UpdateTargetRequest request) {
    WatchedItem item = findItem(id);
    item.updateTargetPrice(request.targetPrice());
    return repository.save(item);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void delete(@PathVariable Long id) {
    WatchedItem item = findItem(id);
    priceCheckRepository.deleteByWatchedItem(item);
    repository.delete(item);
  }

  /**
   * A price check saved the same item between our load and our save. Tell the client to
   * retry rather than returning a 500 or overwriting the newer data.
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public void handleConcurrentUpdate() {
  }

  private WatchedItem findItem(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No item with id " + id));
  }

  /**
   * storeDomain becomes the host the server calls, so only a plain public hostname is allowed:
   * no scheme, port, path, credentials, "localhost" or IP address. productUrl is rendered as a
   * link in the UI, so only http(s) URLs are allowed (this blocks "javascript:" links). The
   * 255-character limits match the database columns, so long input is a 400 instead of a 500.
   */
  public record CreateItemRequest(
      @NotBlank
      @Size(max = 253)
      @Pattern(
          regexp = "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}",
          message = "must be a store hostname like example.myshopify.com")
      String storeDomain,

      @NotBlank
      @Pattern(
          regexp = "gid://shopify/ProductVariant/\\d+",
          message = "must look like gid://shopify/ProductVariant/123")
      String variantId,

      @NotBlank
      @Size(max = 255)
      String name,

      @NotNull
      @Positive
      BigDecimal targetPrice,

      @NotBlank
      @Size(max = 255)
      @Pattern(regexp = "https?://\\S+", message = "must be an http(s) URL")
      String productUrl) {}

  public record UpdateTargetRequest(@NotNull @Positive BigDecimal targetPrice) {}

  public record PricePoint(BigDecimal price, Instant checkedAt) {}
}
