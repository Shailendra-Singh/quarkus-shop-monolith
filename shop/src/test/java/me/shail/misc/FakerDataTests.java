package me.shail.misc;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import me.shail.helpers.data.TestDataFactory;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FakerDataTests {
    @Test
    @TestTransaction
    void generateFakeCustomers() {
        // arrange
        var customers = TestDataFactory.generateMockCustomers();
        // assert
        assert (customers.size() == 10);
    }
}
