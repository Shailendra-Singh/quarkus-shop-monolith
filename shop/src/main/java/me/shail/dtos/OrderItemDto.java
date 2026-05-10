package me.shail.dtos;

import java.util.UUID;

public record OrderItemDto(UUID id, Long quantity, UUID productId, UUID orderId) {
}
