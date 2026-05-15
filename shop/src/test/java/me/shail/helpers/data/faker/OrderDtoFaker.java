package me.shail.helpers.data.faker;

import me.shail.dtos.OrderDto;
import me.shail.helpers.Constants;
import me.shail.helpers.data.faker.base.EntityDtoFaker;
import me.shail.models.enums.OrderStatus;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.util.List;

public final class OrderDtoFaker implements EntityDtoFaker<OrderDto> {
    private final static Faker faker = Constants.FAKER;

    private OrderDto generateOrder() {
        return new OrderDto(
                null,
                BigDecimal.valueOf(faker.number().randomDouble(2, 10, 1000)),
                faker.options().nextElement(OrderStatus.values()).toString(),
                faker.timeAndDate().past(30,
                        java.util.concurrent.TimeUnit.DAYS).atZone(java.time.ZoneId.systemDefault()),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public OrderDto generate() {
        return generateOrder();
    }

    public List<OrderDto> generate(int count) {
        return faker.collection(this::generateOrder).len(count).generate();
    }
}
