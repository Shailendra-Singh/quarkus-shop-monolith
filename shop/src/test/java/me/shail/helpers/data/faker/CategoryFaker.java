package me.shail.helpers.data.faker;

import me.shail.dtos.CategoryDto;
import me.shail.helpers.Constants;
import me.shail.helpers.data.faker.base.EntityDtoFaker;
import net.datafaker.Faker;

import java.util.List;

public final class CategoryFaker implements EntityDtoFaker<CategoryDto> {
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

    @Override
    public CategoryDto generate() {
        return generateCategory();
    }

    public List<CategoryDto> generate(int count) {
        return faker.collection(this::generateCategory).len(count).generate();
    }
}
