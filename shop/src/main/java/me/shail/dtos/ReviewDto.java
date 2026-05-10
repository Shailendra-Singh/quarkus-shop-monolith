package me.shail.dtos;

import java.util.UUID;

public record ReviewDto(
        UUID id,
        String title,
        String description,
        Long rating
) {
}
