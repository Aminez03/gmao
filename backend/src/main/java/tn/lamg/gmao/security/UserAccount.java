package tn.lamg.gmao.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccount {

    @Id
    public UUID id = UUID.randomUUID();

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
