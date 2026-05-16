package me.shail.helpers.data.faker;

import me.shail.dtos.AddressDto;
import me.shail.helpers.Constants;
import me.shail.helpers.data.faker.base.EntityDtoFaker;
import net.datafaker.Faker;

import java.util.List;

public final class AddressDtoDtoFaker implements EntityDtoFaker<AddressDto> {
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

    @Override
    public AddressDto generate() {
        return generateAddress();
    }

    public List<AddressDto> generate(int count) {
        return faker.collection(this::generateAddress).len(count).generate();
    }
}
