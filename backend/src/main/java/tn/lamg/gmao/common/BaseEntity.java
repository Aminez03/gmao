package tn.lamg.gmao.common;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    public UUID id = UUID.randomUUID();

    @org.hibernate.annotations.Generated
    @Column(
        name = "display_number",
        nullable = false,
        insertable = false,
        updatable = false,
        length = 40
    )
    public String displayNumber;

    @Version
    public long version;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    @Column(nullable = false)
    public Instant updatedAt = Instant.now();

    public UUID createdBy;
    public boolean archived;

    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }
}
