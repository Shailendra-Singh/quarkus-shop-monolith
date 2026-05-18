package me.shail.helpers.data;

import me.shail.dtos.CartDto;
import me.shail.dtos.CustomerDto;
import me.shail.dtos.OrderDto;
import me.shail.helpers.data.factory.CartDataFactory;
import me.shail.helpers.data.factory.CustomerDataFactory;
import me.shail.helpers.data.factory.OrderDataFactory;

import java.util.List;

public final class TestDataFactory {
    public static List<CustomerDto> generateMockCustomerDtos(int count) {
        return CustomerDataFactory.generateCustomers(count);
    }

    public static CustomerDto generateMockCustomerDto() {
        return CustomerDataFactory.generateCustomer();
    }

    public static CartDto generateMockCartDto() {
        return CartDataFactory.generateCart();
    }

    public static OrderDto generateMockOrderDto(CartDto cartDto) {
        return OrderDataFactory.generateOrder(cartDto);
    }
}
