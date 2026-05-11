package me.shail.helpers.data.faker;

import me.shail.dtos.ReviewDto;
import me.shail.helpers.Constants;
import net.datafaker.Faker;

import java.util.List;

public class ReviewFaker {
    private final static Faker faker = Constants.FAKER;

    private ReviewDto generateReview() {
        return new ReviewDto(
                null,
                faker.lorem().sentence(3),
                faker.lorem().paragraph(2),
                faker.number().numberBetween(1L, 6L)
        );
    }

    public List<ReviewDto> generate(int count) {
        return faker.collection(this::generateReview).len(count).generate();
    }
}
