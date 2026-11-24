package tn.lamg.gmao.security;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByRefreshHash(String hash);
}
