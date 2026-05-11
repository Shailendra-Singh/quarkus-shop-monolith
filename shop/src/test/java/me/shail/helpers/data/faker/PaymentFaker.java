package me.shail.helpers.data.faker;

import me.shail.dtos.PaymentDto;
import me.shail.helpers.Constants;
import me.shail.models.enums.PaymentStatus;
import net.datafaker.Faker;

import java.util.List;

public class PaymentFaker {
    private final static Faker faker = Constants.FAKER;

    private PaymentDto generatePayment() {
        return new PaymentDto(
                null,
                faker.commerce().promotionCode(),
                faker.options().nextElement(PaymentStatus.values()).toString(),
                null
        );
    }

    public List<PaymentDto> generate(int count) {
        return faker.collection(this::generatePayment).len(count).generate();
    }
}
