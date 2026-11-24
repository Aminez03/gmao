package tn.lamg.gmao.security;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.ApiException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    final UserRepository users;
    final SessionRepository sessions;
    final PasswordEncoder passwords;
    final JwtEncoder encoder;
    final CurrentUser current;
    final JdbcTemplate jdbc;
    final JavaMailSender mail;
    final Set<String> origins;
    final boolean secure;
    final boolean mailEnabled;
    final TurnstileService turnstile;
    private final Map<String, java.util.ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public AuthController(
        UserRepository u,
        SessionRepository s,
        PasswordEncoder p,
        JwtEncoder e,
        CurrentUser c,
        JdbcTemplate j,
        JavaMailSender m,
        TurnstileService turnstile,
        @Value("${gmao.public-origins:${gmao.public-origin}}") String allowedOrigins,
        @Value("${gmao.secure-cookie:true}") boolean secure,
        @Value("${gmao.mail-enabled:false}") boolean enabled
    ) {
        users = u;
        sessions = s;
        passwords = p;
        encoder = e;
        current = c;
        jdbc = j;
        mail = m;
        this.turnstile = turnstile;
        origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.secure = secure;
        mailEnabled = enabled;
    }

    public record Login(
        @Email @NotBlank String email,
        @NotBlank String password,
        String turnstileToken
    ) {}

    public record Reset(@NotBlank String token, @Size(min = 12, max = 72) String password) {}

    public record Forgot(@Email @NotBlank String email) {}

    @PostMapping("/login")
    @Transactional
    public Map<String, Object> login(
        @Valid @RequestBody Login body,
        HttpServletRequest req,
        HttpServletResponse response
    ) {
        checkOrigin(req);
        limit(req.getRemoteAddr());
        turnstile.verify(body.turnstileToken(), req.getRemoteAddr());
        var u = users.findByEmail(body.email().trim().toLowerCase(Locale.ROOT));
        if (
            u.isEmpty() ||
            !u.get().active ||
            !passwords.matches(body.password(), u.get().passwordHash)
        ) throw new ApiException(401, "BAD_CREDENTIALS", "Identifiants incorrects.");
        return issue(u.get(), response);
    }

    @PostMapping("/refresh")
    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> refresh(
        @CookieValue(name = "gmao_refresh", defaultValue = "") String refresh,
        HttpServletRequest req,
        HttpServletResponse response
    ) {
        checkOrigin(req);
        var s = sessions
            .findByRefreshHash(hash(refresh))
            .orElseThrow(() -> new ApiException(401, "SESSION_EXPIRED", "Reconnectez-vous."));
        jdbc.queryForObject("select id from app_user where id=? for update", UUID.class, s.userId);
        // Refresh is checked again after serialization; two uses cannot both rotate successfully.
        var revoked = jdbc.queryForObject(
            "select revoked from auth_session where id=?",
            Boolean.class,
            s.id
        );
        if (Boolean.TRUE.equals(revoked)) {
            jdbc.update("update auth_session set revoked=true where user_id=?", s.userId);
            throw new ApiException(401, "SESSION_REUSED", "Session r?utilis?e : reconnectez-vous.");
        }
        if (s.expiresAt.isBefore(Instant.now())) throw new ApiException(
            401,
            "SESSION_EXPIRED",
            "Reconnectez-vous."
        );
        var u = users.findById(s.userId).orElseThrow();
        if (!u.active) throw new ApiException(401, "ACCOUNT_DISABLED", "Compte dÃ©sactivÃ©.");
        s.revoked = true;
        sessions.saveAndFlush(s);
        return issue(u, response);
    }

    @PostMapping("/logout")
    @Transactional
    public void logout(HttpServletRequest req, HttpServletResponse response) {
        checkOrigin(req);
        var u = current.get();
        jdbc.update("update auth_session set revoked=true where user_id=?", u.id);
        cookie(response, "", Duration.ZERO);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return profile(current.get());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public void forgot(@Valid @RequestBody Forgot body, HttpServletRequest req) {
        checkOrigin(req);
        limit(req.getRemoteAddr());
        if (!mailEnabled) return;
        users.findByEmail(body.email().toLowerCase(Locale.ROOT)).ifPresent(u -> {
            String raw = random();
            jdbc.update(
                "insert into reset_token(id,user_id,hash,expires_at) values(?,?,?,?)",
                UUID.randomUUID(),
                u.id,
                hash(raw),
                java.sql.Timestamp.from(Instant.now().plusSeconds(1800))
            );
            var m = new SimpleMailMessage();
            m.setTo(u.email);
            m.setSubject("RÃ©initialisation GMAO");
            m.setText(origin + "/reset-password?token=" + raw);
            mail.send(m);
        });
    }

    @PostMapping("/reset-password")
    @Transactional
    public void reset(@Valid @RequestBody Reset body, HttpServletRequest req) {
        checkOrigin(req);
        limit(req.getRemoteAddr());
        var rows = jdbc.queryForList(
            "select id,user_id from reset_token where hash=? and used=false and expires_at>now() for update",
            hash(body.token())
        );
        if (rows.isEmpty()) throw new ApiException(
            422,
            "INVALID_TOKEN",
            "Lien expirÃ© ou dÃ©jÃ  utilisÃ©."
        );
        var row = rows.getFirst();
        var u = users.findById((UUID) row.get("user_id")).orElseThrow();
        u.passwordHash = passwords.encode(body.password());
        users.save(u);
        jdbc.update("update reset_token set used=true where id=?", row.get("id"));
        jdbc.update("update auth_session set revoked=true where user_id=?", u.id);
    }

    private Map<String, Object> issue(UserAccount u, HttpServletResponse response) {
        String raw = random();
        var s = new AuthSession();
        s.userId = u.id;
        s.refreshHash = hash(raw);
        s.expiresAt = Instant.now().plus(Duration.ofDays(7));
        sessions.save(s);
        cookie(response, raw, Duration.ofDays(7));
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
            .issuer("gmao-polyclinique")
            .subject(u.id.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(600))
            .claim("sid", s.id.toString())
            .build();
        String jwt = encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
        return Map.of("accessToken", jwt, "user", profile(u));
    }

    public static Map<String, Object> profile(UserAccount u) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", u.id);
        m.put("email", u.email);
        m.put("name", u.name);
        m.put("role", u.role);
        m.put("active", u.active);
        m.put("contractorId", u.contractorId);
        m.put("version", u.version);
        return m;
    }

    void checkOrigin(HttpServletRequest req) {
        if (!origins.contains(req.getHeader("Origin"))) throw new ApiException(
            403,
            "ORIGIN_DENIED",
            "Origine de la requÃªte non autorisÃ©e."
        );
    }

    private void cookie(HttpServletResponse response, String value, Duration age) {
        response.addHeader(
            "Set-Cookie",
            ResponseCookie.from("gmao_refresh", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(age)
                .build()
                .toString()
        );
    }

    private synchronized void limit(String key) {
        Instant now = Instant.now();
        if (attempts.size() > 10000) attempts
            .entrySet()
            .removeIf(
                e ->
                    e.getValue().isEmpty() || e.getValue().getLast().isBefore(now.minusSeconds(900))
            );
        var q = attempts.computeIfAbsent(key, k -> new java.util.ArrayDeque<>());
        while (!q.isEmpty() && q.getFirst().isBefore(now.minusSeconds(900))) q.removeFirst();
        if (q.size() >= 30) throw new ApiException(
            429,
            "TOO_MANY_ATTEMPTS",
            "Trop de tentatives. RÃ©essayez plus tard."
        );
        q.add(now);
    }

    private static String random() {
        byte[] b = new byte[48];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
