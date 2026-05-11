package me.shail.helpers.data.faker;

import me.shail.dtos.CustomerDto;
import me.shail.helpers.Constants;
import net.datafaker.Faker;

import java.util.List;

public final class CustomerFaker {
    private final static Faker faker = Constants.FAKER;

    private CustomerDto generateCustomer() {
        return new CustomerDto(
                null,
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().emailAddress(),
                faker.phoneNumber().cellPhone()
        );
    }

    public List<CustomerDto> generate(int count) {
        return faker.collection(this::generateCustomer).len(count).generate();
    }
}
