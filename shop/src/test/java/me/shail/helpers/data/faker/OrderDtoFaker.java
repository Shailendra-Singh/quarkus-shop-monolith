package me.shail.helpers.data.faker;

import me.shail.dtos.CartDto;
import me.shail.dtos.OrderDto;
import me.shail.helpers.Constants;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Collections;

public final class OrderDtoFaker {
    private final static Faker faker = Constants.FAKER;
    private final static AddressDtoFaker addressFaker = new AddressDtoFaker();

    private OrderDto generateOrder(CartDto cartDto) {
        return new OrderDto(
                null,
                BigDecimal.valueOf(faker.number().randomDouble(2, 10, 1000)),
                null,
                faker.timeAndDate().past(30,
                        java.util.concurrent.TimeUnit.DAYS).atZone(ZoneOffset.UTC),
                null,
                addressFaker.generate(),
                Collections.emptySet(),
                cartDto
        );
    }

    public OrderDto generate(CartDto cartDto) {
        return generateOrder(cartDto);
    }
}
