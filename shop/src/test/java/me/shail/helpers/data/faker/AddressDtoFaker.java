package me.shail.helpers.data.faker;

import me.shail.dtos.AddressDto;
import me.shail.helpers.Constants;
import net.datafaker.Faker;

import java.util.List;

public final class AddressDtoFaker {
    private final static Faker faker = Constants.FAKER;

    private AddressDto generateAddress() {
        return new AddressDto(
                faker.address().streetAddress(),
                faker.address().secondaryAddress(),
                faker.address().city(),
                faker.address().postcode(),
                faker.address().countryCode()
        );
    }

    public AddressDto generate() {
        return generateAddress();
    }

    public List<AddressDto> generate(int count) {
        return faker.collection(this::generateAddress).len(count).generate();
    }
}
