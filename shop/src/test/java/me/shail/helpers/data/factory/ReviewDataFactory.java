package me.shail.helpers.data.factory;

import me.shail.dtos.ReviewDto;
import me.shail.helpers.data.faker.ReviewDtoFaker;

import java.util.List;

public class ReviewDataFactory {
    private final static ReviewDtoFaker _faker = new ReviewDtoFaker();

    public static List<ReviewDto> generate(int count) {
        return _faker.generate(count);
    }

    public static ReviewDto generate() {
        return _faker.generate();
    }
}