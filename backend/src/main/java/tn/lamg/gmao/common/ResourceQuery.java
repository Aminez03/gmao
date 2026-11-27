package tn.lamg.gmao.common;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.equipes.model.TeamMember;
import tn.lamg.gmao.security.*;

/** Database pagination for broad role scopes and assigned work queues. */
@Service
public class ResourceQuery {

    final EntityStore store;
    final CurrentUser current;
    final DtoMapper mapper;

    public ResourceQuery(EntityStore s, CurrentUser c, DtoMapper m) {
        store = s;
        current = c;
        mapper = m;
    }

    public Map<String, Object> page(
        Class<? extends BaseEntity> type,
        int page,
        int size,
        String search,
        String status
    ) {
        var u = current.get();
        String name = type.getSimpleName();
        var params = new HashMap<String, Object>();
        String scope = null;
        if (name.equals("Notification")) {
            scope = "e.recipientId=:uid";
            params.put("uid", u.id);
        } else if (Set.of("ADMIN", "RESPONSABLE_TECHNIQUE").contains(u.role)) scope = "true";
        else if (Set.of("Absence", "UserSkill", "TimeEntry").contains(name)) {
            scope = "e.userId=:uid";
            params.put("uid", u.id);
        } else if (name.equals("AuditEvent")) scope = "false";
        else if (u.role.equals("TECHNICIEN")) {
            if (
                Set.of(
                    "Asset",
                    "Location",
                    "Skill",
                    "Team",
                    "TeamMember",
                    "Shift",
                    "Checklist",
                    "InventoryItem",
                    "PreventivePlan",
                    "PreventiveOccurrence",
                    "Reservation",
                    "Downtime"
                ).contains(name)
            ) scope = "true";
            else if (Set.of("PurchaseRequest", "FailureReport").contains(name)) {
                scope = "e.createdBy=:uid";
                params.put("uid", u.id);
            } else if (name.equals("ShiftAssignment")) {
                scope = "e.userId=:uid";
                params.put("uid", u.id);
            } else if (name.equals("WorkOrder") || name.equals("Anomaly")) {
                var day = LocalDate.now(java.time.ZoneId.of("Africa/Tunis"));
                var teams = store
                    .matching(TeamMember.class, "userId", u.id)
                    .stream()
                    .filter(
                        m ->
                            !m.validFrom.isAfter(day) &&
                            (m.validUntil == null || !m.validUntil.isBefore(day))
                    )
                    .map(m -> m.teamId)
                    .distinct()
                    .toList();
                String variable = name.equals("WorkOrder") ? "e" : "w";
                String assigned = variable + ".technicianId=:uid";
                params.put("uid", u.id);
                if (!teams.isEmpty()) {
                    assigned += " or " + variable + ".teamId in :teams";
                    params.put("teams", teams);
                }
                scope = name.equals("WorkOrder")
                    ? "(" + assigned + ")"
                    : "e.workOrderId in (select w.id from WorkOrder w where " + assigned + ")";
            }
        }
        if (scope == null) return null; // Object-linked and external scopes use the policy-filtered fallback.
        String where = " where e.archived=false and (" + scope + ")";
        if (status != null && !status.isBlank()) {
            try {
                type.getField("status");
            } catch (NoSuchFieldException ex) {
                throw new ApiException(
                    422,
                    "INVALID_FILTER",
                    "Cette ressource ne possède pas d'état."
                );
            }
            where += " and e.status=:status";
            params.put("status", status);
        }
        if (search != null && !search.isBlank()) {
            var parts = new ArrayList<String>();
            for (Field f : type.getFields())
                if (
                    !f.getType().getName().equals("com.fasterxml.jackson.databind.JsonNode")
                ) parts.add(
                    "lower(cast(e." + f.getName() + " as string)) like :search escape '!' "
                );
            where += " and (" + String.join(" or ", parts) + ")";
            params.put(
                "search",
                "%" +
                    search
                        .toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_") +
                    "%"
            );
        }
        var data = store
            .manager()
            .createQuery(
                "select e from " + name + " e" + where + " order by e.createdAt desc,e.id",
                type
            );
        var count = store
            .manager()
            .createQuery("select count(e) from " + name + " e" + where, Long.class);
        params.forEach((key, value) -> {
            data.setParameter(key, value);
            count.setParameter(key, value);
        });
        long total = count.getSingleResult();
        long offset = (long) page * size;
        ApiException.require(offset <= Integer.MAX_VALUE, "INVALID_PAGE", "Page trop éloignée.");
        var rows = mapper.dtos(
            data
                .setFirstResult((int) offset)
                .setMaxResults(size)
                .getResultList()
        );
        return Map.of(
            "items",
            rows,
            "page",
            page,
            "size",
            size,
            "totalElements",
            total,
            "totalPages",
            (total + size - 1) / size
        );
    }
}
