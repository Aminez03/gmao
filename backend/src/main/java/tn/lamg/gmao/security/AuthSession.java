package tn.lamg.gmao.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_session")
public class AuthSession {

    @Id
    public UUID id = UUID.randomUUID();

    public UUID userId;
    public String refreshHash;
    public Instant expiresAt;
    public boolean revoked;
}
