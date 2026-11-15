package io.github.hillol.pricewatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pricewatch-test")
class PricewatchApplicationTests {

	@Test
	void contextLoads() {
	}

}
