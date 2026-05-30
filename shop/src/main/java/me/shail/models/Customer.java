package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Entity
@ToString(callSuper = true)
@Table(name = "customers")
public class Customer extends AbstractEntity {
    @NotNull
    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name")
    public String lastName;

    @NotNull
    @Column(name = "email", nullable = false)
    public String email;

    @Column(name = "telephone")
    public String telephone;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Cart> carts = new HashSet<>();

    @Column(name = "enabled", nullable = false)
    public Boolean enabled;

    public Customer(@NotNull String firstName,
                    String lastName,
                    @Email @NotNull String email,
                    String telephone,
                    Set<Cart> carts,
                    Boolean enabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.telephone = telephone;
        this.carts = carts;
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Customer customer)) return false;
        return Objects.equals(firstName, customer.firstName)
                && Objects.equals(lastName, customer.lastName)
                && Objects.equals(email, customer.email)
                && Objects.equals(telephone, customer.telephone)
                && Objects.equals(carts, customer.carts)
                && Objects.equals(enabled, customer.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, telephone, enabled);
    }
}
