package io.github.hillol.pricewatch;

import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.service.Notifier;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailNotifierTest {

  @Autowired
  private Notifier notifier;

  @Test
  void sendsTestEmail() {
    WatchedItem item = new WatchedItem(
        "the-lola-blanket.myshopify.com",
        "gid://shopify/ProductVariant/51724359991668",
        "Antique Ivory (Large)",
        new BigDecimal("350.00"),
        "https://lolablankets.com/products/antique-ivory-lola-blanket?variant=51724359991668");

    notifier.sendPriceDrop(item, new BigDecimal("325.0"));
  }
}