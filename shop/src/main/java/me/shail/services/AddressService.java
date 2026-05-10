package me.shail.services;

import jakarta.enterprise.context.ApplicationScoped;
import me.shail.dtos.AddressDto;
import me.shail.models.Address;

@ApplicationScoped
public class AddressService {
    public static Address createFromDto(AddressDto addressDto) {
        return new Address(addressDto.address1(),
                addressDto.address2(),
                addressDto.city(),
                addressDto.postalCode(),
                addressDto.countryCode()
        );
    }

    public static AddressDto mapToDto(Address address) {
        return new AddressDto(
                address.address1,
                address.address2,
                address.city,
                address.postalCode,
                address.countryCode
        );
    }
}
