package io.github.hillol.pricewatch;

import io.github.hillol.pricewatch.model.WatchedItem;
import io.github.hillol.pricewatch.repository.WatchedItemRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PricewatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(PricewatchApplication.class, args);
	}

	@Bean
	CommandLineRunner seedData(WatchedItemRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				repository.save(new WatchedItem(
						"the-lola-blanket.myshopify.com",
						"gid://shopify/ProductVariant/51724359991668",
						"Antique Ivory (Large)",
						new BigDecimal("350.00"),
						"https://lolablankets.com/products/antique-ivory-lola-blanket?variant=51724359991668"));
			}
		};
	}
}