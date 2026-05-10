package me.shail.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Address {
    @Column(name = "address_1")
    public String address1;

    @Column(name = "address_2")
    public String address2;

    @Column(name = "city")
    public String city;

    @NotNull
    @Size(max = 10)
    @Column(name = "postal_code", length = 10, nullable = false)
    public String postalCode;

    @NotNull
    @Size(max = 2)
    @Column(name = "country_code", length = 2, nullable = false)
    public String countryCode;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Address address)) return false;
        return Objects.equals(address1, address.address1) &&
                Objects.equals(address2, address.address2) &&
                Objects.equals(city, address.city) &&
                Objects.equals(postalCode, address.postalCode) &&
                Objects.equals(countryCode, address.countryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address1, address2, city, postalCode, countryCode);
    }
}
