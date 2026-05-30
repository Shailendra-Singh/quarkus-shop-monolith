package me.shail.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(UUID id, BigDecimal unitPrice, Long quantity, UUID productId, UUID orderId) {
}
