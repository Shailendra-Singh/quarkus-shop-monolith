package me.shail.helpers.data.factory;

import me.shail.dtos.OrderItemDto;
import me.shail.helpers.data.faker.OrderItemDtoFaker;

import java.util.UUID;

public class OrderItemDataFactory {
    public final static OrderItemDtoFaker _faker = new OrderItemDtoFaker();

    public static OrderItemDto generateOrderItem(UUID productId, UUID orderId) {
        return _faker.generate(productId, orderId);
    }
}
