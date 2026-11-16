package io.github.hillol.pricewatch.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hillol.pricewatch.model.WatchedItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DataJpaTest
class WatchedItemRepositoryTest {

  @Autowired private WatchedItemRepository repository;
  @Autowired private TestEntityManager entityManager;

  @Test
  void staleCopyCannotOverwriteANewerSave() {
    Long id = entityManager.persistAndGetId(new WatchedItem(
        "example.myshopify.com", "gid://shopify/ProductVariant/1", "Blanket",
        new BigDecimal("100.00"), "https://example.com/p"), Long.class);
    entityManager.flush();
    entityManager.clear();

    // The price check loads its copy...
    WatchedItem staleCopy = repository.findById(id).orElseThrow();
    entityManager.detach(staleCopy);

    // ...the user changes the target in the meantime...
    WatchedItem userCopy = repository.findById(id).orElseThrow();
    userCopy.updateTargetPrice(new BigDecimal("80.00"));
    repository.saveAndFlush(userCopy);
    entityManager.clear();

    // ...so the price check's save must fail instead of reverting the target to 100.
    staleCopy.setLastPrice(new BigDecimal("90.00"));
    assertThatThrownBy(() -> repository.saveAndFlush(staleCopy))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);

    entityManager.clear();
    assertThat(repository.findById(id).orElseThrow().getTargetPrice()).isEqualByComparingTo("80.00");
  }
}
