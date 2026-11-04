package io.github.hillol.pricewatch.service;

public class PriceFetchException extends RuntimeException {
  public PriceFetchException(String message) {
    super(message);
  }
}