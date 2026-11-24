package tn.lamg.gmao;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import tn.lamg.gmao.security.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GMAO_TEST_URL", matches = ".+")
class GmaoIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder passwords;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("GMAO_TEST_URL"));
        r.add("spring.datasource.username", () ->
            System.getenv().getOrDefault("GMAO_TEST_USER", "gmao_test")
        );
        r.add("spring.datasource.password", () ->
            System.getenv().getOrDefault("GMAO_TEST_PASSWORD", "")
        );
        r.add(
            "gmao.jwt-secret",
            () -> "integration-test-signing-secret-longer-than-forty-eight-characters"
        );
        r.add("gmao.bootstrap-email", () -> "bootstrap@test.invalid");
        r.add("gmao.bootstrap-password", () -> "integration-password-1234");
        r.add("gmao.secure-cookie", () -> false);
        r.add("gmao.schedule-delay", () -> 3600000);
        r.add("logging.level.root", () -> "WARN");
        r.add("debug", () -> false);
        r.add("logging.level.org.springframework", () -> "WARN");
        r.add("logging.level.tn.lamg", () -> "WARN");
        r.add("logging.level.org.hibernate", () -> "WARN");
    }

    static final java.util.concurrent.atomic.AtomicInteger loginAddress =
        new java.util.concurrent.atomic.AtomicInteger();
    String admin, tech;
    String password = "Integration-test-1234";

    @BeforeEach
    void accounts() throws Exception {
        admin = account("ADMIN");
        tech = account("TECHNICIEN");
    }

    String account(String role) throws Exception {
        var u = new UserAccount();
        u.email = UUID.randomUUID() + "@test.invalid";
        u.name = "Test " + role;
        u.role = role;
        u.passwordHash = passwords.encode(password);
        users.saveAndFlush(u);
        var result = mvc
            .perform(
                post("/api/v1/auth/login")
                    .with(req -> {
                        req.setRemoteAddr("127.0.1." + loginAddress.incrementAndGet());
                        return req;
                    })
                    .header("Origin", "http://localhost:3000")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(Map.of("email", u.email, "password", password))
                    )
            )
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    JsonNode create(String token, String route, Map<String, Object> fields) throws Exception {
        return read(
            mvc
                .perform(
                    post("/api/v1/" + route)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fields", fields)))
                )
                .andExpect(status().isCreated())
                .andReturn()
        );
    }

    JsonNode act(
        String token,
        String route,
        JsonNode entity,
        String action,
        Map<String, Object> body,
        int expected
    ) throws Exception {
        return read(
            mvc
                .perform(
                    post("/api/v1/" + route + "/" + entity.get("id").asText() + "/" + action)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .header("If-Match", "\"" + entity.get("version").asLong() + "\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body))
                )
                .andExpect(status().is(expected))
                .andReturn()
        );
    }

    JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    JsonNode asset() throws Exception {
        var parent = create(
            admin,
            "locations",
            Map.of("code", UUID.randomUUID().toString(), "name", "Bâtiment", "type", "BATIMENT")
        );
        for (String type : List.of("ETAGE", "ZONE", "LOCAL"))
            parent = create(
                admin,
                "locations",
                Map.of(
                    "code",
                    UUID.randomUUID().toString(),
                    "name",
                    type,
                    "type",
                    type,
                    "parentId",
                    parent.get("id").asText()
                )
            );
        return create(
            admin,
            "assets",
            Map.of(
                "code",
                UUID.randomUUID().toString(),
                "name",
                "Pompe",
                "category",
                "Hydraulique",
                "locationId",
                parent.get("id").asText(),
                "criticality",
                "C1"
            )
        );
    }

    @Test
    void paginatedSearchAndAssignedQueueRespectScope() throws Exception {
        var a = asset();
        String marker = "Queue-" + UUID.randomUUID();
        var assigned = create(
            admin,
            "work-orders",
            Map.of(
                "title",
                marker,
                "description",
                "Test pagination",
                "assetId",
                a.get("id").asText(),
                "priority",
                "P2",
                "type",
                "CORRECTIVE"
            )
        );
        create(
            admin,
            "work-orders",
            Map.of(
                "title",
                marker,
                "description",
                "Autre technicien",
                "assetId",
                a.get("id").asText(),
                "priority",
                "P2",
                "type",
                "CORRECTIVE"
            )
        );
        act(admin, "work-orders", assigned, "assign", Map.of("technicianId", subject(tech)), 200);
        var page = read(
            mvc
                .perform(
                    get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + tech)
                        .param("search", marker)
                        .param("size", "1")
                        .param("status", "AFFECTEE")
                )
                .andExpect(status().isOk())
                .andReturn()
        );
        assertEquals(1, page.get("totalElements").asInt());
        assertEquals(assigned.get("id"), page.get("items").get(0).get("id"));
        var assignedRow = page.get("items").get(0);
        assertEquals(subject(tech), assignedRow.get("technicianId").asText());
        assertEquals(
            "Test TECHNICIEN",
            assignedRow.path("userNames").path("technicianId").asText()
        );
        assertEquals("Test ADMIN", assignedRow.path("userNames").path("createdBy").asText());
        assertFalse(assignedRow.toString().contains("passwordHash"));
        var detail = read(
            mvc
                .perform(
                    get("/api/v1/work-orders/" + assigned.get("id").asText()).header(
                        "Authorization",
                        "Bearer " + tech
                    )
                )
                .andExpect(status().isOk())
                .andReturn()
        );
        assertEquals(assignedRow.get("userNames"), detail.get("userNames"));
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + tech)).andExpect(
            status().isForbidden()
        );
        var second = read(
            mvc
                .perform(
                    get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + admin)
                        .param("search", marker)
                        .param("size", "1")
                        .param("page", "1")
                )
                .andExpect(status().isOk())
                .andReturn()
        );
        assertEquals(2, second.get("totalElements").asInt());
        assertEquals(1, second.get("items").size());
    }

    @Test
    void idempotentReplayRechecksChangedPrivileges() throws Exception {
        String token = account("ADMIN");
        UUID key = UUID.randomUUID();
        String body = json.writeValueAsString(
            Map.of("fields", Map.of("code", UUID.randomUUID().toString(), "name", "Restricted"))
        );
        mvc.perform(
            post("/api/v1/suppliers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated());
        var u = users.findById(UUID.fromString(subject(token))).orElseThrow();
        u.role = "TECHNICIEN";
        users.saveAndFlush(u);
        mvc.perform(
            post("/api/v1/suppliers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotFound());
    }

    @Test
    void repeatedLoginAttemptsAreLimited() throws Exception {
        for (int i = 0; i < 31; i++) mvc.perform(
            post("/api/v1/auth/login")
                .with(req -> {
                    req.setRemoteAddr("127.0.2.1");
                    return req;
                })
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@test.invalid\",\"password\":\"incorrect-password\"}")
        ).andExpect(status().is(i < 30 ? 401 : 429));
    }

    @Test
    void anonymousDenied() throws Exception {
        mvc.perform(get("/api/v1/assets")).andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotPromoteRole() throws Exception {
        mvc.perform(
            post("/api/v1/teams")
                .header("Authorization", "Bearer " + tech)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("fields", Map.of("code", "X", "name", "X", "actorRole", "ADMIN"))
                    )
                )
        ).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void technicianCannotCreateTeam() throws Exception {
        mvc.perform(
            post("/api/v1/teams")
                .header("Authorization", "Bearer " + tech)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fields\":{\"code\":\"Z\",\"name\":\"Z\"}}")
        ).andExpect(status().isForbidden());
    }

    @Test
    void sameOperationHasOneEffect() throws Exception {
        UUID key = UUID.randomUUID();
        String body = json.writeValueAsString(
            Map.of("fields", Map.of("code", UUID.randomUUID().toString(), "name", "Équipe"))
        );
        var first = read(
            mvc
                .perform(
                    post("/api/v1/teams")
                        .header("Authorization", "Bearer " + admin)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isCreated())
                .andReturn()
        );
        var second = read(
            mvc
                .perform(
                    post("/api/v1/teams")
                        .header("Authorization", "Bearer " + admin)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isCreated())
                .andReturn()
        );
        assertEquals(first.get("id"), second.get("id"));
        mvc.perform(
            post("/api/v1/teams")
                .header("Authorization", "Bearer " + admin)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Équipe", "Différente"))
        ).andExpect(status().isConflict());
    }

    @Test
    void optimisticVersionPreventsOverwrite() throws Exception {
        var team = create(
            admin,
            "teams",
            Map.of("code", UUID.randomUUID().toString(), "name", "Origine")
        );
        String url = "/api/v1/teams/" + team.get("id").asText();
        mvc.perform(
            put(url)
                .header("Authorization", "Bearer " + admin)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("If-Match", "\"99\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fields\":{\"name\":\"Perdu\"}}")
        ).andExpect(status().isPreconditionFailed());
    }

    @Test
    void invalidStateRejected() throws Exception {
        var a = asset();
        var w = create(
            admin,
            "work-orders",
            Map.of(
                "title",
                "Test",
                "description",
                "Diagnostic",
                "assetId",
                a.get("id").asText(),
                "priority",
                "P1",
                "type",
                "CORRECTIVE"
            )
        );
        act(admin, "work-orders", w, "validate", Map.of(), 422);
    }

    @Test
    void stockCannotBecomeNegative() throws Exception {
        var i = create(
            admin,
            "inventory/items",
            Map.of(
                "code",
                UUID.randomUUID().toString(),
                "name",
                "Filtre",
                "unit",
                "unité",
                "minimum",
                1,
                "unitCost",
                2
            )
        );
        act(
            admin,
            "inventory/items",
            i,
            "adjust",
            Map.of("type", "ADJUST_OUT", "quantity", 1, "reason", "Test"),
            422
        );
        var updated = act(
            admin,
            "inventory/items",
            i,
            "adjust",
            Map.of("type", "ADJUST_IN", "quantity", 4, "reason", "Inventaire initial"),
            200
        );
        assertEquals(
            0,
            updated.get("stock").decimalValue().compareTo(new java.math.BigDecimal("4"))
        );
    }

    @Test
    void publishedChecklistImmutable() throws Exception {
        var c = create(
            admin,
            "checklists",
            Map.of(
                "name",
                "Contrôle",
                "steps",
                List.of(
                    Map.of("key", "check", "label", "Serrage", "type", "CHECKBOX", "required", true)
                )
            )
        );
        var published = act(admin, "checklists", c, "publish", Map.of(), 200);
        mvc.perform(
            put("/api/v1/checklists/" + c.get("id").asText())
                .header("Authorization", "Bearer " + admin)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("If-Match", "\"" + published.get("version").asLong() + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fields\":{\"name\":\"Modification\"}}")
        ).andExpect(status().isUnprocessableEntity());
    }

    String subject(String token) throws Exception {
        return json
            .readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]))
            .get("sub")
            .asText();
    }

    @Test
    void correctiveRequiresChecklistAndSecondValidator() throws Exception {
        var a = asset();
        var c = create(
            admin,
            "checklists",
            Map.of(
                "name",
                "Sécurité",
                "steps",
                List.of(
                    Map.of(
                        "key",
                        "safe",
                        "label",
                        "Contrôle sécurité",
                        "type",
                        "CHECKBOX",
                        "required",
                        true
                    )
                )
            )
        );
        c = act(admin, "checklists", c, "publish", Map.of(), 200);
        var w = create(
            admin,
            "work-orders",
            Map.of(
                "title",
                "Réparer",
                "description",
                "Fuite",
                "assetId",
                a.get("id").asText(),
                "priority",
                "P1",
                "type",
                "CORRECTIVE",
                "checklistId",
                c.get("id").asText()
            )
        );
        w = act(admin, "work-orders", w, "assign", Map.of("technicianId", subject(tech)), 200);
        w = act(tech, "work-orders", w, "start", Map.of("safetyConfirmed", true), 200);
        act(
            tech,
            "work-orders",
            w,
            "complete",
            Map.of(
                "diagnostic",
                "Fuite",
                "cause",
                "Joint",
                "solution",
                "Remplacement",
                "report",
                "Réparé"
            ),
            422
        );
        w = act(tech, "work-orders", w, "answers", Map.of("answers", Map.of("safe", true)), 200);
        w = act(
            tech,
            "work-orders",
            w,
            "time",
            Map.of("startsAt", "2025-01-01T10:00:00Z", "endsAt", "2025-01-01T10:30:00Z"),
            200
        );
        w = act(
            tech,
            "work-orders",
            w,
            "complete",
            Map.of(
                "diagnostic",
                "Fuite",
                "cause",
                "Joint",
                "solution",
                "Remplacement",
                "report",
                "Réparé"
            ),
            200
        );
        act(tech, "work-orders", w, "validate", Map.of(), 403);
        w = act(admin, "work-orders", w, "validate", Map.of(), 200);
        assertEquals("VALIDEE", w.get("status").asText());
    }

    @Test
    void purchasePartialQualityCreditsOnlyAcceptedQuantity() throws Exception {
        String requester = account("SERVICE_ACHATS");
        var item = create(
            admin,
            "inventory/items",
            Map.of(
                "code",
                UUID.randomUUID().toString(),
                "name",
                "Filtre",
                "unit",
                "unité",
                "minimum",
                1,
                "unitCost",
                2
            )
        );
        var request = create(
            requester,
            "purchase-requests",
            Map.of(
                "itemId",
                item.get("id").asText(),
                "quantity",
                10,
                "justification",
                "Stock de sécurité"
            )
        );
        request = act(requester, "purchase-requests", request, "submit", Map.of(), 200);
        request = act(admin, "purchase-requests", request, "approve", Map.of(), 200);
        request = act(admin, "purchase-requests", request, "send-admin", Map.of(), 200);
        var supplier = create(
            admin,
            "suppliers",
            Map.of("code", UUID.randomUUID().toString(), "name", "Fournisseur")
        );
        var order = create(
            requester,
            "purchase-orders",
            Map.of(
                "requestId",
                request.get("id").asText(),
                "supplierId",
                supplier.get("id").asText(),
                "quantity",
                10,
                "unitPrice",
                2
            )
        );
        order = act(requester, "purchase-orders", order, "place", Map.of(), 200);
        var receipt = create(
            requester,
            "goods-receipts",
            Map.of(
                "orderId",
                order.get("id").asText(),
                "deliveryReference",
                "BL-" + UUID.randomUUID(),
                "quantity",
                4
            )
        );
        act(
            requester,
            "goods-receipts",
            receipt,
            "quality",
            Map.of("accepted", 3, "rejected", 1, "note", "Une pièce non conforme"),
            200
        );
        var result = read(
            mvc
                .perform(
                    get("/api/v1/inventory/items/" + item.get("id").asText()).header(
                        "Authorization",
                        "Bearer " + requester
                    )
                )
                .andExpect(status().isOk())
                .andReturn()
        );
        assertEquals(
            0,
            result.get("stock").decimalValue().compareTo(new java.math.BigDecimal("3"))
        );
    }

    @Test
    void concurrentRetriesCreateOneResource() throws Exception {
        var key = UUID.randomUUID();
        String body = json.writeValueAsString(
            Map.of("fields", Map.of("code", UUID.randomUUID().toString(), "name", "Concurrent"))
        );
        java.util.concurrent.Callable<JsonNode> task = () ->
            read(
                mvc
                    .perform(
                        post("/api/v1/teams")
                            .header("Authorization", "Bearer " + admin)
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                    )
                    .andExpect(status().isCreated())
                    .andReturn()
            );
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = pool.submit(task);
            var b = pool.submit(task);
            assertEquals(a.get().get("id"), b.get().get("id"));
        }
    }

    @Test
    void offlineConflictDoesNotModifyServer() throws Exception {
        var a = asset();
        var w = create(
            admin,
            "work-orders",
            Map.of(
                "title",
                "Test sync",
                "description",
                "Test",
                "assetId",
                a.get("id").asText(),
                "priority",
                "P2",
                "type",
                "CORRECTIVE"
            )
        );
        var operation = Map.of(
            "operationId",
            UUID.randomUUID(),
            "resourceId",
            w.get("id").asText(),
            "action",
            "answers",
            "expectedVersion",
            99,
            "payload",
            Map.of("answers", Map.of())
        );
        var result = read(
            mvc
                .perform(
                    post("/api/v1/sync/operations")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(List.of(operation)))
                )
                .andExpect(status().isOk())
                .andReturn()
        );
        assertEquals("CONFLICT", result.get(0).get("state").asText());
    }

    @Test
    void logoutRevokesAccess() throws Exception {
        mvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + tech)
                .header("Origin", "http://localhost:3000")
        ).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tech)).andExpect(
            status().isUnauthorized()
        );
    }
}
