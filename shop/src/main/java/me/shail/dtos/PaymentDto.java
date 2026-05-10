package me.shail.dtos;

import java.util.UUID;

public record PaymentDto(UUID id, String paymentReferenceId, String status, UUID orderId) {
}
