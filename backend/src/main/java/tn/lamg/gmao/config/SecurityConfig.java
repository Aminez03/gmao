package tn.lamg.gmao.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKeySpec signingKey(@Value("${gmao.jwt-secret}") String secret) {
        if (secret.length() < 48) throw new IllegalStateException(
            "JWT_SECRET doit contenir au moins 48 caractÃ¨res."
        );
        return new SecretKeySpec(
            secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256"
        );
    }

    @Bean
    JwtEncoder encoder(SecretKeySpec key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder decoder(SecretKeySpec key) {
        var d = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        d.setJwtValidator(JwtValidators.createDefaultWithIssuer("gmao-polyclinique"));
        return d;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        // Authentication uses explicit bearer headers; refresh cookie endpoints also verify Origin.
        return http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a ->
                a
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus"
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> {}))
            .build();
    }
}
