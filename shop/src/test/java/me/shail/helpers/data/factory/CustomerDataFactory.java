package me.shail.helpers.data.factory;

import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.faker.CustomerFaker;

import java.util.List;

public final class CustomerDataFactory {
    private final List<CustomerDto> mockCustomers;

    private CustomerDataFactory() {
        mockCustomers = List.copyOf(loadCustomers());
    }

    public static List<CustomerDto> generateCustomers() {
        return Holder.INSTANCE.mockCustomers;
    }

    private static CustomerDataFactory getInstance() {
        return Holder.INSTANCE;
    }

    private List<CustomerDto> loadCustomers() {
        return (new CustomerFaker()).generate(10);
    }

    private static class Holder {
        private static final CustomerDataFactory INSTANCE = new CustomerDataFactory();
    }
}
