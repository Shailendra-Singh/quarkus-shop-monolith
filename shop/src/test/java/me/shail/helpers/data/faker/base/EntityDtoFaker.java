package me.shail.helpers.data.faker.base;

import java.util.List;

public interface EntityDtoFaker<T> {
    T generate();

    List<T> generate(int count);
}
