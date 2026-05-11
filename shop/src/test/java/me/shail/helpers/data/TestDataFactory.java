package me.shail.helpers.data;

import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.factory.CustomerDataFactory;

import java.util.List;

public final class TestDataFactory {
    public static List<CustomerDto> generateMockCustomers() {
        return CustomerDataFactory.generateCustomers();
    }
}
