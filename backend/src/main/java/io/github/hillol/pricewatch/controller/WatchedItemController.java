package io.github.hillol.pricewatch.controller;

import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.service.WatchedItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP in and out: validates requests and calls {@link WatchedItemService}. Errors: ApiExceptionHandler. */
@RestController
@RequestMapping("/items")
public class WatchedItemController {

  private final WatchedItemService service;

  public WatchedItemController(WatchedItemService service) {
    this.service = service;
  }

  @GetMapping
  public List<WatchedItem> list() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WatchedItem create(@Valid @RequestBody CreateItemRequest request) {
    return service.create(
        request.storeDomain(),
        request.variantId(),
        request.name(),
        request.targetPrice(),
        request.productUrl());
  }

  @GetMapping("/{id}/history")
  public List<PricePoint> history(@PathVariable long id) {
    return service.history(id).stream()
        .map(check -> new PricePoint(check.getPrice(), check.getCheckedAt()))
        .toList();
  }

  @PatchMapping("/{id}")
  public WatchedItem updateTarget(@PathVariable long id, @Valid @RequestBody UpdateTargetRequest request) {
    return service.updateTargetPrice(id, request.targetPrice());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    service.delete(id);
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
