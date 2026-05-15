package me.shail.helpers.data.faker;

import me.shail.dtos.ProductDto;
import me.shail.helpers.Constants;
import me.shail.helpers.data.faker.base.EntityDtoFaker;
import me.shail.models.enums.ProductStatus;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class ProductDtoFaker implements EntityDtoFaker<ProductDto> {
    private final static Faker faker = Constants.FAKER;

    private ProductDto generateProduct() {
        return new ProductDto(
                null,
                faker.commerce().productName(),
                faker.lorem().sentence(10),
                BigDecimal.valueOf(faker.number().randomDouble(2, 5, 2000)),
                faker.options().nextElement(ProductStatus.values()).toString(),
                faker.number().numberBetween(0, 10000),
                Collections.emptySet(),
                null
        );
    }

    @Override
    public ProductDto generate() {
        return generateProduct();
    }

    public List<ProductDto> generate(int count) {
        return faker.collection(this::generateProduct).len(count).generate();
    }
}
