package me.shail.helpers.data.faker;

import me.shail.dtos.PaymentDto;
import me.shail.helpers.Constants;
import me.shail.helpers.data.faker.base.EntityDtoFaker;
import me.shail.models.enums.PaymentStatus;
import net.datafaker.Faker;

import java.util.List;

public class PaymentDtoFaker implements EntityDtoFaker<PaymentDto> {
    private final static Faker faker = Constants.FAKER;

    private PaymentDto generatePayment() {
        return new PaymentDto(
                null,
                faker.commerce().promotionCode(),
                faker.options().nextElement(PaymentStatus.values()).toString(),
                null
        );
    }

    @Override
    public PaymentDto generate() {
        return generatePayment();
    }

    public List<PaymentDto> generate(int count) {
        return faker.collection(this::generatePayment).len(count).generate();
    }
}
