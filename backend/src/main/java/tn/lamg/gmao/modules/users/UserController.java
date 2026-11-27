package tn.lamg.gmao.modules.users;

import jakarta.validation.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.security.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    final UserRepository users;
    final CurrentUser current;
    final PasswordEncoder passwords;
    final CommandService commands;
    final JdbcTemplate jdbc;
    final Events events;
    static final Set<String> ROLES = Set.of(
        "ADMIN",
        "RESPONSABLE_TECHNIQUE",
        "TECHNICIEN",
        "SOUS_TRAITANT"
    );

    public UserController(
        UserRepository u,
        CurrentUser c,
        PasswordEncoder p,
        CommandService s,
        JdbcTemplate j,
        Events e
    ) {
        users = u;
        current = c;
        passwords = p;
        commands = s;
        jdbc = j;
        events = e;
    }

    public record UserInput(
        @Email @NotBlank String email,
        @NotBlank @Size(max = 160) String name,
        @NotBlank String role,
        Boolean active,
        UUID contractorId,
        @Size(min = 12, max = 72) String password
    ) {}

    @GetMapping
    public Object list() {
        current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
        return Map.of(
            "items",
            users
                .findAll()
                .stream()
                .map(u -> {
                    if (current.has("ADMIN")) return AuthController.profile(u);
                    return Map.<String, Object>of(
                        "id",
                        u.id,
                        "displayNumber",
                        u.displayNumber,
                        "name",
                        u.name,
                        "role",
                        u.role,
                        "active",
                        u.active
                    );
                })
                .toList()
        );
    }

    @PostMapping
    public Object create(
        @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody UserInput input
    ) {
        current.require("ADMIN");
        return commands.run(key, "users/create", input, () -> {
            ApiException.require(
                input.password() != null,
                "PASSWORD_REQUIRED",
                "Mot de passe initial obligatoire, à transmettre par canal sécurisé."
            );
            var u = new UserAccount();
            apply(u, input);
            u.passwordHash = passwords.encode(input.password());
            u = users.saveAndFlush(u);
            events.account(u, "CREATE_USER");
            return AuthController.profile(u);
        });
    }

    @PutMapping("/{id}")
    public Object update(
        @PathVariable UUID id,
        @RequestHeader("Idempotency-Key") UUID key,
        @RequestHeader("If-Match") String match,
        @Valid @RequestBody UserInput input
    ) {
        current.require("ADMIN");
        return commands.run(key, "users/" + id + "/" + match, input, () -> {
            var u = users.findById(id).orElseThrow();
            if (!match.equals("\"" + u.version + "\"")) throw new ApiException(
                412,
                "VERSION_CONFLICT",
                "Compte modifié."
            );
            if (
                u.role.equals("ADMIN") &&
                (!input.role().equals("ADMIN") || Boolean.FALSE.equals(input.active()))
            ) ApiException.require(
                users
                    .findAll()
                    .stream()
                    .filter(a -> a.active && a.role.equals("ADMIN"))
                    .count() > 1,
                "LAST_ADMIN",
                "Conservez au moins un administrateur actif."
            );
            apply(u, input);
            if (input.password() != null) u.passwordHash = passwords.encode(input.password());
            users.saveAndFlush(u);
            events.account(u, "UPDATE_USER");
            jdbc.update("update auth_session set revoked=true where user_id=?", u.id);
            return AuthController.profile(u);
        });
    }

    void apply(UserAccount u, UserInput input) {
        ApiException.require(ROLES.contains(input.role()), "ROLE_INVALID", "Rôle invalide.");
        ApiException.require(
            !input.role().equals("SOUS_TRAITANT") || input.contractorId() != null,
            "CONTRACTOR_REQUIRED",
            "Entreprise obligatoire."
        );
        u.email = input.email().trim().toLowerCase(Locale.ROOT);
        u.name = input.name();
        u.role = input.role();
        u.active = !Boolean.FALSE.equals(input.active());
        u.contractorId = input.contractorId();
    }
}
