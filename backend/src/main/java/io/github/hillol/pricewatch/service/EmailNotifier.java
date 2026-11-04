package io.github.hillol.pricewatch.service;

import io.github.hillol.pricewatch.model.WatchedItem;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotifier implements Notifier {

  private final JavaMailSender mailSender;
  private final String fromEmail;
  private final String alertEmail;

  public EmailNotifier(
      JavaMailSender mailSender,
      @Value("${spring.mail.username}") String fromEmail,
      @Value("${pricewatch.alert-email}") String alertEmail) {
    this.mailSender = mailSender;
    this.fromEmail = fromEmail;
    this.alertEmail = alertEmail;
  }

  @Override
  public void sendPriceDrop(WatchedItem item, BigDecimal newPrice) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(alertEmail);
    message.setSubject("Price drop: " + item.getName());
    message.setText(
        item.getName() + " dropped to $" + newPrice
            + ", below your target of $" + item.getTargetPrice() + ".\n\n"
            + item.getProductUrl());
    mailSender.send(message);
  }
}