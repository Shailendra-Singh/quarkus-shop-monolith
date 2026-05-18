package me.shail.helpers.data.factory;

import me.shail.dtos.CartDto;
import me.shail.helpers.Constants;
import me.shail.models.enums.CartStatus;
import net.datafaker.Faker;

import java.util.UUID;

public final class CartDataFactory {
    private final static Faker faker = Constants.FAKER;

    public static CartDto generateCart() {
        return new CartDto(UUID.randomUUID(), null, CartStatus.NEW.name());
    }
}
