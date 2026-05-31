package me.shail.helpers;

import net.datafaker.Faker;

import java.util.Random;

public final class Constants {
    // Faker constants
    public static final long SEED = 54321L;
    public static final Faker FAKER = new Faker(new Random(Constants.SEED));

    // API Endpoints
    public final static String CART_ROOT_URL = "/carts";
    public final static String CUSTOMER_ROOT_URL = "/customers";
}
