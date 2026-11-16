package io.github.hillol.pricewatch.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hillol.pricewatch.model.PriceCheck;
import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.service.ItemNotFoundException;
import io.github.hillol.pricewatch.service.WatchedItemService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

/**
 * Covers the HTTP layer only: request validation, status codes and JSON shape. What the
 * operations actually do to the database is covered by WatchedItemServiceTest.
 */
@WebMvcTest(WatchedItemController.class)
class WatchedItemControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WatchedItemService service;

  private static final String VALID_ITEM = """
      {
        "storeDomain": "example.myshopify.com",
        "variantId": "gid://shopify/ProductVariant/123",
        "name": "Blanket",
        "targetPrice": 100.00,
        "productUrl": "https://example.com/products/blanket?variant=123"
      }
      """;

  private static WatchedItem blanket(String targetPrice) {
    return new WatchedItem(
        "example.myshopify.com", "gid://shopify/ProductVariant/123", "Blanket",
        new BigDecimal(targetPrice), "https://example.com/products/blanket");
  }

  /** Makes service.create return an item built from whatever the controller passed in. */
  private void givenCreateEchoesInput() {
    when(service.create(anyString(), anyString(), anyString(), any(), anyString()))
        .thenAnswer(inv -> new WatchedItem(
            inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
            inv.getArgument(3), inv.getArgument(4)));
  }

  private ResultActions postItem(String storeDomain, String variantId, String productUrl) throws Exception {
    return postJson("""
        {
          "storeDomain": "%s",
          "variantId": "%s",
          "name": "Blanket",
          "targetPrice": 100.00,
          "productUrl": "%s"
        }
        """.formatted(storeDomain, variantId, productUrl));
  }

  private ResultActions postJson(String json) throws Exception {
    return mockMvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private ResultActions patchTarget(long id, String targetPrice) throws Exception {
    return mockMvc.perform(patch("/items/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetPrice\": " + targetPrice + "}"));
  }

  // --- POST /items ---

  @Test
  void createsValidItem() throws Exception {
    givenCreateEchoesInput();

    postJson(VALID_ITEM)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Blanket"))
        .andExpect(jsonPath("$.targetPrice").value(100.00));

    verify(service).create(
        "example.myshopify.com",
        "gid://shopify/ProductVariant/123",
        "Blanket",
        new BigDecimal("100.00"),
        "https://example.com/products/blanket?variant=123");
  }

  @ParameterizedTest
  @ValueSource(strings = {"example.myshopify.com", "lolablankets.com", "shop.example.co.uk"})
  void acceptsStoreHostnames(String storeDomain) throws Exception {
    givenCreateEchoesInput();

    postItem(storeDomain, "gid://shopify/ProductVariant/123", "https://example.com/p")
        .andExpect(status().isCreated());
  }

  @Test
  void rejectsMissingFields() throws Exception {
    postJson("""
        {"name": "Blanket", "targetPrice": 100.00}
        """)
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
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

    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"javascript:alert(1)", "ftp://example.com/p", "example.com/p", "https://"})
  void rejectsProductUrlsThatAreNotHttp(String productUrl) throws Exception {
    postItem("example.myshopify.com", "gid://shopify/ProductVariant/123", productUrl)
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"123", "gid://shopify/Product/123", "gid://shopify/ProductVariant/abc"})
  void rejectsMalformedVariantIds(String variantId) throws Exception {
    postItem("example.myshopify.com", variantId, "https://example.com/p")
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void rejectsNameLongerThanTheDatabaseColumn() throws Exception {
    postJson(VALID_ITEM.replace("\"Blanket\"", "\"" + "x".repeat(256) + "\""))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void rejectsNonPositiveTargetPrice() throws Exception {
    postJson(VALID_ITEM.replace("100.00", "0"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  // --- GET /items/{id}/history ---

  @Test
  void returnsPriceHistoryAsPricePoints() throws Exception {
    WatchedItem item = blanket("100.00");
    when(service.history(1L)).thenReturn(List.of(
        new PriceCheck(item, new BigDecimal("120.00"), Instant.parse("2026-11-01T00:00:00Z")),
        new PriceCheck(item, new BigDecimal("95.00"), Instant.parse("2026-11-02T00:00:00Z"))));

    mockMvc.perform(get("/items/1/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].price").value(120.00))
        .andExpect(jsonPath("$[0].checkedAt").value("2026-11-01T00:00:00Z"))
        .andExpect(jsonPath("$[1].price").value(95.00));
  }

  @Test
  void historyReturns404ForUnknownItem() throws Exception {
    when(service.history(99L)).thenThrow(new ItemNotFoundException(99L));

    mockMvc.perform(get("/items/99/history"))
        .andExpect(status().isNotFound());
  }

  // --- PATCH /items/{id} ---

  @Test
  void updatesTargetPrice() throws Exception {
    when(service.updateTargetPrice(1L, new BigDecimal("80.00"))).thenReturn(blanket("80.00"));

    patchTarget(1L, "80.00")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetPrice").value(80.00));
  }

  @Test
  void rejectsNonPositiveTargetOnUpdate() throws Exception {
    patchTarget(1L, "-5")
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void updateReturns409WhenAPriceCheckSavedTheItemFirst() throws Exception {
    when(service.updateTargetPrice(1L, new BigDecimal("80.00")))
        .thenThrow(new ObjectOptimisticLockingFailureException(WatchedItem.class, 1L));

    patchTarget(1L, "80.00")
        .andExpect(status().isConflict());
  }

  @Test
  void updateReturns404ForUnknownItem() throws Exception {
    when(service.updateTargetPrice(99L, new BigDecimal("80.00"))).thenThrow(new ItemNotFoundException(99L));

    patchTarget(99L, "80.00")
        .andExpect(status().isNotFound());
  }

  // --- DELETE /items/{id} ---

  @Test
  void deletesItem() throws Exception {
    mockMvc.perform(delete("/items/1"))
        .andExpect(status().isNoContent());

    verify(service).delete(1L);
  }

  @Test
  void deleteReturns404ForUnknownItem() throws Exception {
    doThrow(new ItemNotFoundException(99L)).when(service).delete(99L);

    mockMvc.perform(delete("/items/99"))
        .andExpect(status().isNotFound());
  }
}
