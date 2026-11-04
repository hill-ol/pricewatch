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
}