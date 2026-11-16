package io.github.hillol.pricewatch.service;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class ShopifyPriceSource implements PriceSource {

  private static final String QUERY = """
      query ($id: ID!) {
        node(id: $id) {
          ... on ProductVariant {
            price { amount }
          }
        }
      }
      """;

  private final RestClient restClient;

  @Autowired
  public ShopifyPriceSource(
      @Value("${pricewatch.shopify.connect-timeout:5s}") Duration connectTimeout,
      @Value("${pricewatch.shopify.read-timeout:10s}") Duration readTimeout) {
    this(RestClient.builder()
        .requestFactory(requestFactory(connectTimeout, readTimeout))
        .build());
  }

  ShopifyPriceSource(RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * Without timeouts, a store that accepts the connection but never answers would block
   * the scheduler thread forever, and no item would be checked again until a restart.
   */
  static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(readTimeout);
    return factory;
  }

  @Override
  public BigDecimal fetchPrice(String storeDomain, String variantId) {
    Map<String, Object> body = Map.of(
        "query", QUERY,
        "variables", Map.of("id", variantId));

    JsonNode response;
    try {
      response = restClient.post()
          .uri("https://" + storeDomain + "/api/2026-07/graphql.json")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientException e) {
      throw new PriceFetchException("Request to " + storeDomain + " failed: " + e.getMessage());
    }

    JsonNode amount = response.path("data").path("node").path("price").path("amount");
    if (amount.isMissingNode() || amount.isNull()) {
      throw new PriceFetchException("No price found for " + variantId + " on " + storeDomain);
    }
    return new BigDecimal(amount.asString());
  }
}