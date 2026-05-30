package me.shail.helpers.data.factory;

import me.shail.dtos.CategoryDto;
import me.shail.helpers.data.faker.CategoryFaker;

public class CategoryDataFactory {
    private final static CategoryFaker _faker = new CategoryFaker();

    public static CategoryDto generateCategory() {
        return _faker.generate();
    }
}
