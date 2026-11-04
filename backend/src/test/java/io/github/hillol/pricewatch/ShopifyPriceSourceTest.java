package io.github.hillol.pricewatch;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.hillol.pricewatch.service.PriceSource;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShopifyPriceSourceTest {

  @Autowired
  private PriceSource priceSource;

  @Test
  void fetchesLolaBlanketPrice() {
    BigDecimal price = priceSource.fetchPrice(
        "the-lola-blanket.myshopify.com",
        "gid://shopify/ProductVariant/51724359991668");
    System.out.println("Price: " + price);
    assertNotNull(price);
  }
}