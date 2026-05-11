package me.shail.helpers.data.faker;

import me.shail.dtos.CategoryDto;
import me.shail.helpers.Constants;
import net.datafaker.Faker;

import java.util.List;

public final class CategoryFaker {
    private final static Faker faker = Constants.FAKER;

    private CategoryDto generateCategory() {
        String category = faker.commerce().department();
        return new CategoryDto(
                null,
                category,
                category + " description",
                0L
        );
    }

    public List<CategoryDto> generate(int count) {
        return faker.collection(this::generateCategory).len(count).generate();
    }
}
