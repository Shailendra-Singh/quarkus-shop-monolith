package me.shail.helpers;

import net.datafaker.Faker;

import java.util.Random;

public final class Constants {
    public static final long SEED = 54321L;
    public static final Faker FAKER = new Faker(new Random(Constants.SEED));
}
