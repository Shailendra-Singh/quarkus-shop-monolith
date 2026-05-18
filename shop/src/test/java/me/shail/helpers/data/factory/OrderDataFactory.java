package me.shail.helpers.data.factory;

import me.shail.dtos.CartDto;
import me.shail.dtos.OrderDto;
import me.shail.helpers.data.faker.OrderDtoFaker;

public class OrderDataFactory {
    private final static OrderDtoFaker _faker = new OrderDtoFaker();

    public static OrderDto generateOrder(CartDto cartDto) {
        return _faker.generate(cartDto);
    }
}
