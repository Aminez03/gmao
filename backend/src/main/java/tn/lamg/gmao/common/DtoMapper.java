package tn.lamg.gmao.common;

import java.lang.reflect.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tn.lamg.gmao.security.CurrentUser;

@Component
public class DtoMapper {

    final CurrentUser current;
    final NamedParameterJdbcTemplate jdbc;
    private static final Set<String> USER_FIELDS = Set.of(
        "userId",
        "technicianId",
        "technicianIds",
        "createdBy",
        "approvedBy",
        "validatedBy",
        "acknowledgedBy",
        "resolvedBy",
        "recipientId",
        "actorId"
    );

    public DtoMapper(CurrentUser c, NamedParameterJdbcTemplate j) {
        current = c;
        jdbc = j;
    }

    public Map<String, Object> dto(BaseEntity entity) {
        return dtos(List.of(entity)).getFirst();
    }

    /** Resolve only names referenced by authorized records, in one query per collection. */
    public List<Map<String, Object>> dtos(Collection<? extends BaseEntity> entities) {
        var rows = entities.stream().map(this::values).toList();
        var ids = new HashSet<UUID>();
        for (var row : rows) {
            for (var field : USER_FIELDS) {
                if (row.get(field) instanceof UUID id) ids.add(id);
                if ("technicianIds".equals(field) && row.get(field) instanceof com.fasterxml.jackson.databind.JsonNode node && node.isArray())
                    for (var value : node) try {
                        ids.add(UUID.fromString(value.asText()));
                    } catch (IllegalArgumentException ignored) {}
            }
        }
        var names = new HashMap<UUID, String>();
        if (!ids.isEmpty()) {
            jdbc.query(
                "select id, name from app_user where id in (:ids)",
                Map.of("ids", ids),
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                    names.put(rs.getObject("id", UUID.class), rs.getString("name"))
            );
        }
        for (var row : rows) {
            var labels = new LinkedHashMap<String, String>();
            for (var field : USER_FIELDS) {
                if (row.get(field) instanceof UUID id && names.containsKey(id)) {
                    labels.put(field, names.get(id));
                }
                if ("technicianIds".equals(field) && row.get(field) instanceof com.fasterxml.jackson.databind.JsonNode node && node.isArray()) {
                    var technicians = new ArrayList<String>();
                    for (var value : node) try {
                        var name = names.get(UUID.fromString(value.asText()));
                        if (name != null) technicians.add(name);
                    } catch (IllegalArgumentException ignored) {}
                    if (!technicians.isEmpty()) labels.put(field, String.join(", ", technicians));
                }
            }
            row.put("userNames", labels);
        }
        return rows;
    }

    private Map<String, Object> values(BaseEntity entity) {
        var values = new LinkedHashMap<String, Object>();
        boolean costs = current.has("ADMIN", "RESPONSABLE_TECHNIQUE");
        try {
            for (Field f : entity.getClass().getFields()) {
                if (
                    !costs &&
                    Set.of("unitCost", "unitPrice", "amount", "cost").contains(f.getName())
                ) continue;
                values.put(f.getName(), f.get(entity));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return values;
    }
}
