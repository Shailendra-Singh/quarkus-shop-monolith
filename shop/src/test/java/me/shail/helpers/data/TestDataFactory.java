package me.shail.helpers.data;

import me.shail.dtos.*;
import me.shail.helpers.data.factory.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestDataFactory {
    public static List<CustomerDto> generateMockCustomerDtos(int count) {
        return CustomerDataFactory.generateCustomers(count);
    }

    public static CustomerDto generateMockCustomerDto() {
        return CustomerDataFactory.generateCustomer();
    }

    public static CartDto generateMockCartDto() {
        return CartDataFactory.generateCart();
    }

    public static OrderDto generateMockOrderDto(CartDto cartDto) {
        return OrderDataFactory.generateOrder(cartDto);
    }

    public static CategoryDto generateMockCategoryDto() {
        return CategoryDataFactory.generateCategory();
    }

    public static ProductDto generateMockProductDto(Set<UUID> categories) {
        return ProductDataFactory.generate(categories);
    }

    public static ProductDto generateMockProductDto(UUID categoryId) {
        return ProductDataFactory.generate(Set.of(categoryId));
    }

    public static ProductDto generateMockProductDto() {
        return ProductDataFactory.generate(null);
    }

    public static List<ReviewDto> generateMockReviewDtos(int count) {
        return ReviewDataFactory.generate(count);
    }

    public static ReviewDto generateMockReviewDto() {
        return ReviewDataFactory.generate();
    }
}