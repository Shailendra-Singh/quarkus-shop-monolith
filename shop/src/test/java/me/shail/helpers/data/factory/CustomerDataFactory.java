package me.shail.helpers.data.factory;

import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.faker.CustomerDtoFaker;

import java.util.List;

public final class CustomerDataFactory {
    private final static CustomerDtoFaker _faker = new CustomerDtoFaker();

    public static CustomerDto generateCustomer() {
        return _faker.generate();
    }

    public static List<CustomerDto> generateCustomers(int count) {
        return _faker.generate(count);
    }
}
