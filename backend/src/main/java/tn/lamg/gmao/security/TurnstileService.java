package tn.lamg.gmao.security;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.lamg.gmao.common.ApiException;

@Service
public class TurnstileService {

    private static final String SITEVERIFY_URL =
        "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestClient client;
    private final String secret;

    public TurnstileService(
        RestClient.Builder builder,
        @Value("${gmao.turnstile-secret:}") String secret
    ) {
        this.client = builder.build();
        this.secret = secret.trim();
    }

    public void verify(String token, String remoteIp) {
        // CAPTCHA is optional in local development; configure the secret to enforce it.
        if (secret.isBlank()) return;
        if (token == null || token.isBlank() || token.length() > 2048) {
            throw invalidToken();
        }

        try {
            var result = client
                .post()
                .uri(SITEVERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("secret", secret, "response", token, "remoteip", remoteIp))
                .retrieve()
                .body(SiteverifyResponse.class);
            if (result == null || !result.success()) throw invalidToken();
        } catch (RestClientException exception) {
            throw new ApiException(
                503,
                "TURNSTILE_UNAVAILABLE",
                "La vérification Cloudflare est temporairement indisponible. Réessayez."
            );
        }
    }

    private static ApiException invalidToken() {
        return new ApiException(
            400,
            "TURNSTILE_INVALID",
            "Veuillez terminer la vérification Cloudflare puis réessayer."
        );
    }

    private record SiteverifyResponse(boolean success) {}
}
