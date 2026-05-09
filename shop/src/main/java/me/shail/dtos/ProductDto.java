package me.shail.dtos;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String status,
        Integer salesCounter,
        Set<ReviewDto> reviews,
        UUID categoryId
) {
}
