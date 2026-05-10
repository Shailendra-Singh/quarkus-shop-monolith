package me.shail.dtos;

import java.util.UUID;

public record CartDto(UUID id, CustomerDto customerDto, String status) {
}
