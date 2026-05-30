package me.shail.helpers.data.faker;

import me.shail.dtos.OrderItemDto;
import me.shail.helpers.Constants;
import net.datafaker.Faker;

import java.util.UUID;

public class OrderItemDtoFaker {
    private final static Faker faker = Constants.FAKER;

    private OrderItemDto generateOrder(UUID productId, UUID orderId) {
        return new OrderItemDto(
                null,
                null,
                faker.random().nextLong(1, 10),
                productId,
                orderId
        );
    }

    public OrderItemDto generate(UUID productId, UUID orderId) {
        return generateOrder(productId, orderId);
    }
}
