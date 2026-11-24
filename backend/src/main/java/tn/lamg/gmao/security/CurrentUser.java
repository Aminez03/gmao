package tn.lamg.gmao.security;

import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import tn.lamg.gmao.common.ApiException;

@Component
public class CurrentUser {

    private final UserRepository users;
    private final SessionRepository sessions;

    public CurrentUser(UserRepository users, SessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    public UserAccount get() {
        var request =
            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String cacheKey = CurrentUser.class.getName() + ".account";
        if (
            request != null && request.getAttribute(cacheKey, 0) instanceof UserAccount cached
        ) return cached;
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof Jwt jwt)) throw new ApiException(
            401,
            "UNAUTHENTICATED",
            "Connexion requise."
        );
        var session = sessions
            .findById(UUID.fromString(jwt.getClaimAsString("sid")))
            .orElseThrow(() -> new ApiException(401, "SESSION_REVOKED", "Session expirÃ©e."));
        if (
            session.revoked || session.expiresAt.isBefore(java.time.Instant.now())
        ) throw new ApiException(401, "SESSION_REVOKED", "Session expirÃ©e.");
        var u = users
            .findById(UUID.fromString(jwt.getSubject()))
            .orElseThrow(() -> new ApiException(401, "UNAUTHENTICATED", "Compte absent."));
        if (!u.active || !session.userId.equals(u.id)) throw new ApiException(
            401,
            "ACCOUNT_DISABLED",
            "Compte dÃ©sactivÃ©."
        );
        if (request != null) request.setAttribute(cacheKey, u, 0);
        return u;
    }

    public boolean has(String... roles) {
        return Set.of(roles).contains(get().role);
    }

    public void require(String... roles) {
        if (!has(roles)) throw new ApiException(
            403,
            "FORBIDDEN",
            "Action non autorisÃ©e pour votre rÃ´le."
        );
    }
}
