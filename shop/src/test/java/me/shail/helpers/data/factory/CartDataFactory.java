package me.shail.helpers.data.factory;

import me.shail.dtos.CartDto;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.Constants;
import me.shail.models.enums.CartStatus;
import net.datafaker.Faker;

import java.util.ArrayList;
import java.util.List;

public final class CartDataFactory {
    private final static Faker faker = Constants.FAKER;

    public List<CartDto> generateCartsFromCustomers(List<CustomerDto> customerDtos) {
        List<CartDto> carts = new ArrayList<>();
        for (var customer : customerDtos) {
            var cart = new CartDto(null,
                    customer,
                    String.valueOf(faker.options().nextElement(CartStatus.values())));
            carts.add(cart);
        }
        return carts;
    }
}
