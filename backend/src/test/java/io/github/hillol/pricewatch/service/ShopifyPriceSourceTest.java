package io.github.hillol.pricewatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ShopifyPriceSourceTest {

  private static final String STORE = "example.myshopify.com";
  private static final String VARIANT = "gid://shopify/ProductVariant/123";
  private static final String ENDPOINT = "https://example.myshopify.com/api/2026-07/graphql.json";

  private MockRestServiceServer server;
  private ShopifyPriceSource priceSource;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    priceSource = new ShopifyPriceSource(builder.build());
  }

  @Test
  void returnsPriceFromGraphqlResponse() {
    server.expect(requestTo(ENDPOINT))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.variables.id").value(VARIANT))
        .andRespond(withSuccess(
            """
            {"data": {"node": {"price": {"amount": "325.0"}}}}
            """,
            MediaType.APPLICATION_JSON));

    assertThat(priceSource.fetchPrice(STORE, VARIANT)).isEqualByComparingTo("325.00");
    server.verify();
  }

  @Test
  void throwsWhenVariantDoesNotExist() {
    server.expect(requestTo(ENDPOINT))
        .andRespond(withSuccess(
            """
            {"data": {"node": null}}
            """,
            MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> priceSource.fetchPrice(STORE, VARIANT))
        .isInstanceOf(PriceFetchException.class)
        .hasMessageContaining(VARIANT);
  }

  @Test
  void throwsWhenStoreReturnsError() {
    server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

    assertThatThrownBy(() -> priceSource.fetchPrice(STORE, VARIANT))
        .isInstanceOf(PriceFetchException.class)
        .hasMessageContaining(STORE);
  }
}
