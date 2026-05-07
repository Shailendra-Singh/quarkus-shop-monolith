package me.shail.models.base;


import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class AbstractEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue // Hibernate will detect UUID and use a random generator
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false)
    public Instant createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    public Instant lastModifiedDate;
}
