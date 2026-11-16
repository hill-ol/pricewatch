package io.github.hillol.pricewatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class ShopifyPriceSourceTest {

  private static final String STORE = "example.myshopify.com";
  private static final String VARIANT = "gid://shopify/ProductVariant/123";
  // Deliberately not the default, so the tests prove the configured version is the one used.
  private static final String API_VERSION = "2025-10";
  private static final String ENDPOINT = "https://example.myshopify.com/api/2025-10/graphql.json";

  private MockRestServiceServer server;
  private ShopifyPriceSource priceSource;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    priceSource = new ShopifyPriceSource(builder.build(), API_VERSION);
  }

  @AfterEach
  void verifyExpectedRequestsWereMade() {
    server.verify();
  }

  @Test
  void returnsPriceFromGraphqlResponse() {
    expectShopifyRequest()
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.variables.id").value(VARIANT))
        .andRespond(json("""
            {"data": {"node": {"price": {"amount": "325.0"}}}}
            """));

    assertThat(priceSource.fetchPrice(STORE, VARIANT)).isEqualByComparingTo("325.00");
  }

  @Test
  void throwsWhenVariantDoesNotExist() {
    expectShopifyRequest().andRespond(json("""
        {"data": {"node": null}}
        """));

    assertFetchFailsMentioning(VARIANT);
  }

  @Test
  void throwsWhenStoreReturnsError() {
    expectShopifyRequest().andRespond(withServerError());

    assertFetchFailsMentioning(STORE);
  }

  @Test
  void wrapsTimeoutInPriceFetchException() {
    expectShopifyRequest().andRespond(withException(new SocketTimeoutException("Read timed out")));

    assertFetchFailsMentioning(STORE);
  }

  @Test
  void requestFactoryGivesUpOnServerThatNeverResponds() throws IOException {
    RestClient client = RestClient.builder()
        .requestFactory(ShopifyPriceSource.requestFactory(Duration.ofSeconds(1), Duration.ofMillis(200)))
        .build();

    try (HangingServer hanging = HangingServer.start()) {
      long start = System.nanoTime();

      assertThatThrownBy(() -> client.get().uri(hanging.url()).retrieve().toBodilessEntity())
          .isInstanceOf(ResourceAccessException.class);
      assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "2026-7", "latest", "2026-07/../admin"})
  void rejectsMalformedApiVersionAtStartup(String apiVersion) {
    assertThatThrownBy(() -> new ShopifyPriceSource(RestClient.create(), apiVersion))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pricewatch.shopify.api-version");
  }

  // --- helpers ---

  private ResponseActions expectShopifyRequest() {
    return server.expect(requestTo(ENDPOINT));
  }

  private static ResponseCreator json(String body) {
    return withSuccess(body, MediaType.APPLICATION_JSON);
  }

  private void assertFetchFailsMentioning(String expectedInMessage) {
    assertThatThrownBy(() -> priceSource.fetchPrice(STORE, VARIANT))
        .isInstanceOf(PriceFetchException.class)
        .hasMessageContaining(expectedInMessage);
  }

  /** A local HTTP server that accepts every request and never answers until it is closed. */
  private static final class HangingServer implements AutoCloseable {

    private final HttpServer server;
    private final CountDownLatch release = new CountDownLatch(1);

    private HangingServer() throws IOException {
      server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext("/", exchange -> {
        try {
          release.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        exchange.close();
      });
    }

    static HangingServer start() throws IOException {
      HangingServer hanging = new HangingServer();
      hanging.server.start();
      return hanging;
    }

    String url() {
      return "http://localhost:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
      release.countDown();
      server.stop(0);
    }
  }
}