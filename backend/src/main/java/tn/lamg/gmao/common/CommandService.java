package tn.lamg.gmao.common;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.lamg.gmao.security.*;

@Service
public class CommandService {

    final ResourceRegistry registry;
    final EntityStore store;
    final AccessPolicy policy;
    final CurrentUser current;
    final JdbcTemplate jdbc;
    final ObjectMapper json;

    public CommandService(
        CurrentUser c,
        JdbcTemplate j,
        ObjectMapper o,
        ResourceRegistry r,
        EntityStore s,
        AccessPolicy p
    ) {
        registry = r;
        store = s;
        policy = p;
        current = c;
        jdbc = j;
        json = o.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private Object replay(
        String route,
        String response
    ) throws com.fasterxml.jackson.core.JsonProcessingException {
        var result = json.readTree(response);
        var resource = registry
            .routes()
            .stream()
            .filter(r -> route.startsWith(r + "/"))
            .max(Comparator.comparingInt(String::length));
        if (resource.isPresent() && result.hasNonNull("id")) {
            var entity = store
                .manager()
                .find(registry.type(resource.get()), UUID.fromString(result.get("id").asText()));
            if (entity == null) throw new ApiException(404, "NOT_FOUND", "Ressource introuvable.");
            policy.read(entity);
            if (result instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
                if (
                    !current.has("ADMIN", "RESPONSABLE_TECHNIQUE", "SERVICE_ACHATS", "DIRECTION")
                ) object.remove(java.util.List.of("unitCost", "unitPrice", "amount", "cost"));
            }
        }
        return json.convertValue(result, Object.class);
    }

    @Transactional
    public Object run(UUID key, String route, Object body, Supplier<Object> work) {
        var user = current.get();
        if (key == null) throw new ApiException(
            400,
            "IDEMPOTENCY_REQUIRED",
            "ClÃ© d'opÃ©ration obligatoire."
        );
        jdbc.queryForObject("select id from app_user where id=? for update", UUID.class, user.id);
        try {
            String hash = AuthController.hash(route + json.writeValueAsString(body));
            var previous = jdbc.queryForList(
                "select request_hash,response::text as response from command_receipt where user_id=? and operation_key=?",
                user.id,
                key
            );
            if (!previous.isEmpty()) {
                var p = previous.getFirst();
                if (!hash.equals(p.get("request_hash"))) throw new ApiException(
                    409,
                    "IDEMPOTENCY_CONFLICT",
                    "Cette clÃ© dÃ©signe une autre opÃ©ration."
                );
                return replay(route, (String) p.get("response"));
            }
            Object result = work.get();
            jdbc.update(
                "insert into command_receipt(id,user_id,operation_key,request_hash,response) values(?,?,?,?,?::jsonb)",
                UUID.randomUUID(),
                user.id,
                key,
                hash,
                json.writeValueAsString(result)
            );
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
