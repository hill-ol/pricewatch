package io.github.hillol.pricewatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.hillol.pricewatch.model.WatchedItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailNotifierTest {

  @Test
  void sendsEmailWithPriceDetails() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    EmailNotifier notifier = new EmailNotifier(mailSender, "from@example.com", "to@example.com");
    WatchedItem item = new WatchedItem(
        "example.myshopify.com",
        "gid://shopify/ProductVariant/123",
        "Antique Ivory (Large)",
        new BigDecimal("350.00"),
        "https://example.com/products/blanket?variant=123");

    notifier.sendPriceDrop(item, new BigDecimal("325.00"));

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage message = captor.getValue();

    assertThat(message.getFrom()).isEqualTo("from@example.com");
    assertThat(message.getTo()).containsExactly("to@example.com");
    assertThat(message.getSubject()).isEqualTo("Price drop: Antique Ivory (Large)");
    assertThat(message.getText())
        .contains("$325.00", "$350.00", "https://example.com/products/blanket?variant=123");
  }
}
