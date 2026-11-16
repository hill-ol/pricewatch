package io.github.hillol.pricewatch.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.PriceCheckRepository;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(WatchedItemController.class)
class WatchedItemControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WatchedItemRepository repository;
  @MockitoBean private PriceCheckRepository priceCheckRepository;

  private static final String VALID_ITEM = """
      {
        "storeDomain": "example.myshopify.com",
        "variantId": "gid://shopify/ProductVariant/123",
        "name": "Blanket",
        "targetPrice": 100.00,
        "productUrl": "https://example.com/products/blanket?variant=123"
      }
      """;

  private ResultActions postItem(String storeDomain, String variantId, String productUrl) throws Exception {
    return mockMvc.perform(post("/items")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "storeDomain": "%s",
              "variantId": "%s",
              "name": "Blanket",
              "targetPrice": 100.00,
              "productUrl": "%s"
            }
            """.formatted(storeDomain, variantId, productUrl)));
  }

  @Test
  void createsValidItem() throws Exception {
    when(repository.save(any(WatchedItem.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content(VALID_ITEM))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Blanket"));
  }

  @Test
  void rejectsMissingFields() throws Exception {
    mockMvc.perform(post("/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Blanket", "targetPrice": 100.00}
                """))
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"example.myshopify.com", "lolablankets.com", "shop.example.co.uk"})
  void acceptsStoreHostnames(String storeDomain) throws Exception {
    when(repository.save(any(WatchedItem.class))).thenAnswer(inv -> inv.getArgument(0));

    postItem(storeDomain, "gid://shopify/ProductVariant/123", "https://example.com/p")
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "localhost",
      "127.0.0.1",
      "169.254.169.254",
      "evil.com:8080",
      "evil.com/path",
      "user@evil.com",
      "https://evil.com",
      "evil",
      "-bad.com"})
  void rejectsStoreDomainsThatAreNotPlainHostnames(String storeDomain) throws Exception {
    postItem(storeDomain, "gid://shopify/ProductVariant/123", "https://example.com/p")
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"javascript:alert(1)", "ftp://example.com/p", "example.com/p", "https://"})
  void rejectsProductUrlsThatAreNotHttp(String productUrl) throws Exception {
    postItem("example.myshopify.com", "gid://shopify/ProductVariant/123", productUrl)
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"123", "gid://shopify/Product/123", "gid://shopify/ProductVariant/abc"})
  void rejectsMalformedVariantIds(String variantId) throws Exception {
    postItem("example.myshopify.com", variantId, "https://example.com/p")
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @Test
  void rejectsNameLongerThanTheDatabaseColumn() throws Exception {
    mockMvc.perform(post("/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_ITEM.replace("\"Blanket\"", "\"" + "x".repeat(256) + "\"")))
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @Test
  void rejectsNonPositiveTargetPrice() throws Exception {
    mockMvc.perform(post("/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_ITEM.replace("100.00", "0")))
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @Test
  void returnsPriceHistoryInOrder() throws Exception {
    WatchedItem item = new WatchedItem(
        "example.myshopify.com", "gid://shopify/ProductVariant/123", "Blanket",
        new BigDecimal("100.00"), "https://example.com/products/blanket");
    when(repository.findById(1L)).thenReturn(Optional.of(item));
    when(priceCheckRepository.findByWatchedItemOrderByCheckedAtAsc(item)).thenReturn(List.of(
        new PriceCheck(item, new BigDecimal("120.00"), Instant.parse("2026-11-01T00:00:00Z")),
        new PriceCheck(item, new BigDecimal("95.00"), Instant.parse("2026-11-02T00:00:00Z"))));

    mockMvc.perform(get("/items/1/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].price").value(120.00))
        .andExpect(jsonPath("$[1].price").value(95.00));
  }

  @Test
  void updatesTargetPrice() throws Exception {
    WatchedItem item = new WatchedItem(
        "example.myshopify.com", "gid://shopify/ProductVariant/123", "Blanket",
        new BigDecimal("100.00"), "https://example.com/products/blanket");
    when(repository.findById(1L)).thenReturn(Optional.of(item));
    when(repository.save(any(WatchedItem.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(patch("/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"targetPrice": 80.00}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetPrice").value(80.00));

    verify(repository).save(item);
  }

  @Test
  void rejectsNonPositiveTargetOnUpdate() throws Exception {
    mockMvc.perform(patch("/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"targetPrice": -5}
                """))
        .andExpect(status().isBadRequest());

    verify(repository, never()).save(any());
  }

  @Test
  void updateReturns409WhenAPriceCheckSavedTheItemFirst() throws Exception {
    WatchedItem item = new WatchedItem(
        "example.myshopify.com", "gid://shopify/ProductVariant/123", "Blanket",
        new BigDecimal("100.00"), "https://example.com/products/blanket");
    when(repository.findById(1L)).thenReturn(Optional.of(item));
    when(repository.save(any(WatchedItem.class)))
        .thenThrow(new ObjectOptimisticLockingFailureException(WatchedItem.class, 1L));

    mockMvc.perform(patch("/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"targetPrice": 80.00}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void updateReturns404ForUnknownItem() throws Exception {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(patch("/items/99")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"targetPrice": 80.00}
                """))
        .andExpect(status().isNotFound());
  }

  @Test
  void historyReturns404ForUnknownItem() throws Exception {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/items/99/history"))
        .andExpect(status().isNotFound());
  }
}
