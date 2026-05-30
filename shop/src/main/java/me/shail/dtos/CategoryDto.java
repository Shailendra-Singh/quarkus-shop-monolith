package me.shail.dtos;

import java.util.UUID;

public record CategoryDto(UUID id, String name, String description, UUID parent_category_id) {
}
