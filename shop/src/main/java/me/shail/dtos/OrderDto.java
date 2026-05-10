package me.shail.dtos;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

public record OrderDto(
        UUID id,
        BigDecimal price,
        String status,
        ZonedDateTime shipped,
        UUID paymentId,
        AddressDto shipmentAddress,
        Set<OrderItemDto> orderItems,
        CartDto cart) {
}
