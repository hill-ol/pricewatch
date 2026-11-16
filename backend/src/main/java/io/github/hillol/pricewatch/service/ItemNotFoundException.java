package io.github.hillol.pricewatch.service;

/** Thrown when a watched item id doesn't exist. The web layer turns this into a 404. */
public class ItemNotFoundException extends RuntimeException {

  public ItemNotFoundException(long id) {
    super("No item with id " + id);
  }
}
