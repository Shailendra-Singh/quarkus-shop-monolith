package me.shail.helpers.data.factory;

import me.shail.dtos.ProductDto;
import me.shail.helpers.data.faker.ProductDtoFaker;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProductDataFactory {
    private final static ProductDtoFaker _faker = new ProductDtoFaker();

    public static List<ProductDto> generate(int count, Set<UUID> categories) {
        return _faker.generate(count, categories);
    }

    public static ProductDto generate(Set<UUID> categories) {
        return _faker.generate(categories);
    }
}
