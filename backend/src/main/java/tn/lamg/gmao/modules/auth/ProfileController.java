package tn.lamg.gmao.modules.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.security.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    final CurrentUser current;
    final UserRepository users;
    final PasswordEncoder passwords;
    final JdbcTemplate jdbc;

    public ProfileController(CurrentUser c, UserRepository u, PasswordEncoder p, JdbcTemplate j) {
        current = c;
        users = u;
        passwords = p;
        jdbc = j;
    }

    public record PasswordChange(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 12, max = 72) String newPassword
    ) {}

    @PostMapping("/password")
    @Transactional
    public void password(@Valid @RequestBody PasswordChange b) {
        var u = current.get();
        ApiException.require(
            passwords.matches(b.currentPassword(), u.passwordHash),
            "PASSWORD_INVALID",
            "Mot de passe actuel incorrect."
        );
        u.passwordHash = passwords.encode(b.newPassword());
        users.save(u);
        jdbc.update("update auth_session set revoked=true where user_id=?", u.id);
    }
}
