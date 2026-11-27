package tn.lamg.gmao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.lamg.gmao.security.*;

@Component
public class Bootstrap implements ApplicationRunner {

    final UserRepository users;
    final PasswordEncoder passwords;
    final String email, password;

    public Bootstrap(
        UserRepository u,
        PasswordEncoder p,
        @Value("${gmao.bootstrap-email:}") String e,
        @Value("${gmao.bootstrap-password:}") String secret
    ) {
        users = u;
        passwords = p;
        email = e;
        password = secret;
    }

    public void run(ApplicationArguments args) {
        if (users.count() != 0) return;
        if (email.isBlank() || password.length() < 12) throw new IllegalStateException(
            "Premier démarrage : configurez BOOTSTRAP_EMAIL et BOOTSTRAP_PASSWORD (12 caractères minimum)."
        );
        var u = new UserAccount();
        u.email = email.toLowerCase(java.util.Locale.ROOT);
        u.name = "Administrateur clinique";
        u.passwordHash = passwords.encode(password);
        u.role = "ADMIN";
        users.save(u);
    }
}
