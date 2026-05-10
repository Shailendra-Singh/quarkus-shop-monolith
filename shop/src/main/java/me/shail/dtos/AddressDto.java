package me.shail.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressDto(String address1,
                         String address2,
                         String city,
                         @NotNull
                         String postalCode,
                         @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
                         String countryCode) {
}
