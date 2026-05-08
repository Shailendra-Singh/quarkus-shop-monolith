package me.shail.services;

import jakarta.enterprise.context.ApplicationScoped;
import me.shail.dtos.CustomerDto;
import me.shail.models.Customer;

@ApplicationScoped
public class CustomerService {
    public static CustomerDto mapToDto(Customer customer) {
        return new CustomerDto();
    }
}
