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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
