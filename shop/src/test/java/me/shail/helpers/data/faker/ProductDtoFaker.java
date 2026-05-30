package me.shail.helpers.data.faker;

import me.shail.dtos.ProductDto;
import me.shail.helpers.Constants;
import me.shail.models.enums.ProductStatus;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProductDtoFaker {
    private final static Faker faker = Constants.FAKER;

    private ProductDto generateProduct(Set<UUID> categories) {
        return new ProductDto(
                null,
                faker.commerce().productName(),
                faker.lorem().sentence(10),
                BigDecimal.valueOf(faker.number().randomDouble(2, 5, 2000)),
                faker.options().nextElement(ProductStatus.values()).toString(),
                faker.number().numberBetween(0, 10000),
                categories != null ? categories : Collections.emptySet()
        );
    }

    public ProductDto generate(Set<UUID> categories) {
        return generateProduct(categories);
    }

    public List<ProductDto> generate(int count, Set<UUID> categories) {
        return faker.collection(() -> generate(categories)).len(count).generate();
    }
}
