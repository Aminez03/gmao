package tn.lamg.gmao.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccount {

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

    public String email;
    public String name;
    public String passwordHash;
    public String role;
    public boolean active = true;
    public UUID contractorId;

    @Version
    public long version;

    public Instant createdAt = Instant.now();
}
