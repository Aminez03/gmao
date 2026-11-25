package tn.lamg.gmao.service;

import com.fasterxml.jackson.databind.*;
import jakarta.validation.Validator;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.achats.model.*;
import tn.lamg.gmao.modules.equipes.model.*;
import tn.lamg.gmao.modules.maintenance.model.*;
import tn.lamg.gmao.modules.patrimoine.model.*;
import tn.lamg.gmao.modules.prestataires.model.*;
import tn.lamg.gmao.modules.shared.service.ResourceRules;
import tn.lamg.gmao.security.*;

@Service
public class ResourceService {

    final ResourceRegistry registry;
    final EntityStore store;
    final AccessPolicy policy;
    final CurrentUser current;
    final ObjectMapper json;
    final Validator validator;
    final Events events;
    final DtoMapper mapper;
    final ResourceQuery queries;
    final ObjectProvider<ResourceRules> rules;

    public ResourceService(
        ResourceRegistry r,
        EntityStore s,
        AccessPolicy p,
        CurrentUser c,
        ObjectMapper j,
        Validator v,
        Events e,
        DtoMapper m,
        ResourceQuery q,
        ObjectProvider<ResourceRules> rules
    ) {
        registry = r;
        store = s;
        policy = p;
        current = c;
        json = j;
        validator = v;
        events = e;
        mapper = m;
        queries = q;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(
        String route,
        int page,
        int size,
        String search,
        String status
    ) {
        ApiException.require(
            page >= 0 && size > 0 && size <= 100,
            "INVALID_PAGE",
            "Pagination invalide."
        );
        var result = queries.page(registry.type(route), page, size, search, status);
        if (result != null) return result;
        var values = mapper
            .dtos(store.all(registry.type(route)).stream().filter(policy::canRead).toList())
            .stream()
            .filter(
                m ->
                    search == null ||
                    m
                        .values()
                        .stream()
                        .anyMatch(
                            v ->
                                v != null &&
                                v
                                    .toString()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(search.toLowerCase(Locale.ROOT))
                        )
            )
            .filter(m -> status == null || status.isBlank() || status.equals(m.get("status")))
            .toList();
        long start = (long) page * size;
        return Map.of(
            "items",
            values.subList(
                (int) Math.min(start, values.size()),
                (int) Math.min(start + size, values.size())
            ),
            "page",
            page,
            "size",
            size,
            "totalElements",
            values.size(),
            "totalPages",
            (values.size() + size - 1) / size
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String route, UUID id) {
        var e = store.find(registry.type(route), id);
        policy.read(e);
        return mapper.dto(e);
    }

    public Object create(String route, Map<String, Object> body) {
        try {
            var e = registry.type(route).getDeclaredConstructor().newInstance();
            fill(e, body, true);
            defaults(e);
            policy.manage(e);
            check(e);
            e.createdBy = current.get().id;
            store.save(e);
            events.audit(route, e, "CREATE");
            if (
                e instanceof FailureReport || e instanceof PurchaseRequest || e instanceof Handover
            ) events.supervisors("Nouvelle entrÃ©e : " + route, route, e.id);
            return mapper.dto(e);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Object update(String route, UUID id, String version, Map<String, Object> body) {
        var e = store.lock(registry.type(route), id);
        policy.read(e);
        policy.manage(e);
        store.expect(e, version);
        editable(e);
        fill(e, body, false);
        policy.manage(e);
        check(e);
        store.save(e);
        events.audit(route, e, "UPDATE");
        return mapper.dto(e);
    }

    public Object archive(String route, UUID id, String version) {
        var e = store.lock(registry.type(route), id);
        policy.read(e);
        policy.manage(e);
        current.require("ADMIN", "RESPONSABLE_TECHNIQUE", "SERVICE_ACHATS");
        store.expect(e, version);
        editable(e);
        ApiException.require(
            !(
                e instanceof WorkOrder ||
                e instanceof PurchaseRequest ||
                e instanceof PurchaseOrder ||
                e instanceof GoodsReceipt ||
                e instanceof Shift ||
                e instanceof ShiftAssignment
            ),
            "USE_WORKFLOW",
            "Utilisez le workflow pour cette ressource."
        );
        e.archived = true;
        store.save(e);
        events.audit(route, e, "ARCHIVE");
        return mapper.dto(e);
    }

    private static final Set<String> SERVER = Set.of(
        "id",
        "version",
        "createdAt",
        "updatedAt",
        "createdBy",
        "archived",
        "number",
        "qrToken",
        "stock",
        "reserved",
        "allocated",
        "consumed",
        "accepted",
        "rejected",
        "approvedAt",
        "approvedBy",
        "acknowledgedAt",
        "acknowledgedBy",
        "validatedAt",
        "validatedBy",
        "startedAt",
        "completedAt",
        "published",
        "revision",
        "actualMinutes",
        "answers",
        "checklistSnapshot",
        "occurrenceId",
        "planId",
        "workOrderId",
        "resolvedAt",
        "resolvedBy",
        "correctiveReportId",
        "status"
    );

    void fill(BaseEntity e, Map<String, Object> body, boolean creating) {
        ApiException.require(
            !body.isEmpty() && body.size() <= 40,
            "EMPTY_INPUT",
            "Champs manquants."
        );
        for (var entry : body.entrySet()) {
            String key = entry.getKey();
            boolean linkedWork =
                key.equals("workOrderId") &&
                (e instanceof PurchaseRequest ||
                    e instanceof ExternalIntervention ||
                    e instanceof Downtime ||
                    e instanceof Anomaly);
            boolean assetStatus = e instanceof Asset && key.equals("status");
            if (SERVER.contains(key) && !linkedWork && !assetStatus) throw new ApiException(
                422,
                "SERVER_FIELD",
                "Champ gÃ©rÃ© par le serveur : " + key
            );
            if (
                e instanceof WorkOrder &&
                !Set.of(
                    "title",
                    "description",
                    "assetId",
                    "priority",
                    "type",
                    "dueAt",
                    "checklistId",
                    "requiredSkillId"
                ).contains(key)
            ) throw new ApiException(
                422,
                "COMMAND_REQUIRED",
                "Utilisez les actions d'intervention."
            );
            if (
                e instanceof PreventivePlan &&
                !creating &&
                Set.of(
                    "anchorLocal",
                    "frequency",
                    "mode",
                    "intervalCount",
                    "zoneId",
                    "assetId",
                    "checklistId"
                ).contains(key)
            ) throw new ApiException(
                422,
                "NEW_PLAN_REQUIRED",
                "DÃ©sactivez ce plan et crÃ©ez un nouveau pour changer sa rÃ¨gle."
            );
            try {
                var f = e.getClass().getDeclaredField(key);
                f.set(e, json.convertValue(entry.getValue(), f.getType()));
            } catch (NoSuchFieldException ex) {
                throw new ApiException(422, "UNKNOWN_FIELD", "Champ inconnu : " + key);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    void defaults(BaseEntity e) {
        rules
            .orderedStream()
            .filter(rule -> rule.supports(e))
            .forEach(rule -> rule.defaults(e));
        if (e instanceof Asset a) {
            if (a.status == null) a.status = "EN_SERVICE";
            a.qrToken = UUID.randomUUID();
        }
        if (e instanceof Shift s) s.status = "BROUILLON";
        if (e instanceof Absence a) {
            a.status = "SOUMISE";
            if (!policy.supervisor()) a.userId = current.get().id;
        }
        if (e instanceof Handover h) {
            if (h.workOrderIds == null) h.workOrderIds = json.createArrayNode();
            if (h.assetIds == null) h.assetIds = json.createArrayNode();
        }
        if (e instanceof Checklist c) {
            c.revision = 1;
            c.published = false;
        }
        if (e instanceof PreventivePlan p) {
            long planNumber = ((Number) store.manager()
                .createNativeQuery("select nextval('preventive_plan_number_seq')")
                .getSingleResult()).longValue();
            p.number = "PM-" + String.format(Locale.ROOT, "%03d", planNumber);
            ApiException.require(
                p.anchorLocal != null && p.zoneId != null,
                "PLAN_ANCHOR",
                "Date de rÃ©fÃ©rence et fuseau obligatoires."
            );
            if (p.active == null) p.active = true;
            p.nextDueAt = LocalDateTime.parse(p.anchorLocal)
                .atZone(ZoneId.of(p.zoneId))
                .toInstant();
        }
        if (e instanceof Contract c) {
            long contractNumber = ((Number) store.manager()
                .createNativeQuery("select nextval('contract_number_seq')")
                .getSingleResult()).longValue();
            c.number = "CT-" + String.format(Locale.ROOT, "%03d", contractNumber);
        }
        if (e instanceof WorkOrder w) {
            w.number = "OT-" + w.id.toString().substring(0, 8).toUpperCase();
            w.status = "NOUVELLE";
            w.actualMinutes = 0;
            w.answers = json.createObjectNode();
            if (w.type == null) w.type = "CORRECTIVE";
            if (w.checklistId != null) {
                var c = store.find(Checklist.class, w.checklistId);
                ApiException.require(
                    Boolean.TRUE.equals(c.published),
                    "CHECKLIST_UNPUBLISHED",
                    "Publiez la gamme avant utilisation."
                );
                w.checklistSnapshot = c.steps.deepCopy();
            } else w.checklistSnapshot = json.createArrayNode();
        }
        if (e instanceof FailureReport f) f.status = "NOUVELLE";
        if (e instanceof ExternalIntervention m) {
            m.status = "PLANIFIEE";
            m.contractorId = store.find(Contract.class, m.contractId).contractorId;
        }
        if (e instanceof MaterialRequest m) m.status = "SOUMISE";
    }

    void editable(BaseEntity e) {
        rules
            .orderedStream()
            .filter(rule -> rule.supports(e))
            .forEach(rule -> rule.editable(e));
        if (e instanceof Checklist c) ApiException.require(
            !Boolean.TRUE.equals(c.published),
            "IMMUTABLE_VERSION",
            "Une gamme publiÃ©e est immuable. CrÃ©ez une nouvelle version."
        );
        if (e instanceof WorkOrder w) ApiException.require(
            Set.of("NOUVELLE", "AFFECTEE").contains(w.status),
            "IMMUTABLE_WORK_ORDER",
            "Seule une intervention non commencÃ©e peut Ãªtre modifiÃ©e."
        );
        if (e instanceof Handover h) ApiException.require(
            h.acknowledgedAt == null,
            "IMMUTABLE_HANDOVER",
            "Consigne dÃ©jÃ  reÃ§ue ; crÃ©ez un complÃ©ment."
        );
        if (e instanceof Shift s) ApiException.require(
            "BROUILLON".equals(s.status),
            "PUBLISHED_SHIFT",
            "Poste dÃ©jÃ  publiÃ©."
        );
        if (e instanceof ExternalIntervention m) ApiException.require(
            "PLANIFIEE".equals(m.status),
            "IMMUTABLE_MISSION",
            "Mission dÃ©jÃ  soumise."
        );
    }

    public void check(BaseEntity e) {
        rules
            .orderedStream()
            .filter(rule -> rule.supports(e))
            .forEach(rule -> rule.validate(e));
        var violations = validator.validate(e);
        if (!violations.isEmpty()) throw new ApiException(
            422,
            "INVALID_INPUT",
            violations.iterator().next().getPropertyPath() +
                " : " +
                violations.iterator().next().getMessage()
        );
        if (e instanceof Location l) {
            ApiException.require(
                Set.of("BATIMENT", "ETAGE", "ZONE", "LOCAL").contains(l.type),
                "LOCATION_TYPE",
                "Type de localisation invalide."
            );
            UUID parent = l.parentId;
            var seen = new HashSet<UUID>();
            seen.add(l.id);
            String expected = switch (l.type) {
                case "ETAGE" -> "BATIMENT";
                case "ZONE" -> "ETAGE";
                case "LOCAL" -> "ZONE";
                default -> null;
            };
            if (expected != null) ApiException.require(
                parent != null && expected.equals(store.find(Location.class, parent).type),
                "LOCATION_PARENT",
                "Respectez bÃ¢timent â†’ Ã©tage â†’ zone â†’ local."
            );
            else ApiException.require(
                parent == null,
                "LOCATION_ROOT",
                "Un bÃ¢timent est une racine."
            );
            while (parent != null) {
                ApiException.require(
                    seen.add(parent),
                    "LOCATION_CYCLE",
                    "Cycle de localisation interdit."
                );
                parent = store.find(Location.class, parent).parentId;
            }
        }
        if (e instanceof Asset a) {
            ApiException.require(
                "LOCAL".equals(store.find(Location.class, a.locationId).type),
                "ASSET_LOCATION",
                "Choisissez un local."
            );
            ApiException.require(
                Set.of("C1", "C2", "C3", "C4").contains(a.criticality),
                "CRITICALITY",
                "CriticitÃ© invalide."
            );
            ApiException.require(
                Set.of("EN_SERVICE", "INDISPONIBLE", "SURVEILLANCE", "HORS_SERVICE").contains(
                    a.status
                ),
                "ASSET_STATUS",
                "Ã‰tat invalide."
            );
            var seen = new HashSet<UUID>();
            seen.add(a.id);
            var parent = a.parentId;
            while (parent != null) {
                ApiException.require(
                    seen.add(parent),
                    "ASSET_CYCLE",
                    "Cycle Ã©quipement interdit."
                );
                parent = store.find(Asset.class, parent).parentId;
            }
        }
        if (e instanceof Checklist c) {
            ApiException.require(
                c.steps.isArray() && c.steps.size() > 0 && c.steps.size() <= 100,
                "CHECKLIST_STEPS",
                "DÃ©finissez entre 1 et 100 Ã©tapes."
            );
            var keys = new HashSet<String>();
            for (var step : c.steps) {
                ApiException.require(
                    keys.add(step.path("key").asText()) &&
                        !step.path("key").asText().isBlank() &&
                        !step.path("label").asText().isBlank(),
                    "CHECKLIST_KEYS",
                    "ClÃ©s d'Ã©tape uniques et libellÃ©s obligatoires."
                );
                ApiException.require(
                    Set.of(
                        "CHECKBOX",
                        "CONFORMITY",
                        "NUMBER",
                        "TEXT",
                        "MEASURE",
                        "PHOTO",
                        "ATTESTATION"
                    ).contains(step.path("type").asText()),
                    "CHECKLIST_TYPE",
                    "Type d'Ã©tape invalide."
                );
                if (step.hasNonNull("min") && step.hasNonNull("max")) ApiException.require(
                    step.get("min").decimalValue().compareTo(step.get("max").decimalValue()) <= 0,
                    "THRESHOLDS",
                    "Bornes inversÃ©es."
                );
            }
        }
        if (e instanceof PreventivePlan p) {
            ApiException.require(
                p.intervalCount > 0 &&
                    p.intervalCount <= 365 &&
                    Set.of(
                        "DAILY",
                        "WEEKLY",
                        "MONTHLY",
                        "QUARTERLY",
                        "SEMESTRAL",
                        "ANNUAL"
                    ).contains(p.frequency) &&
                    Set.of("FIXE", "GLISSANT").contains(p.mode),
                "PLAN_RULE",
                "RÃ¨gle prÃ©ventive invalide."
            );
            ApiException.require(
                Boolean.TRUE.equals(store.find(Checklist.class, p.checklistId).published),
                "CHECKLIST_UNPUBLISHED",
                "Une gamme publiÃ©e est requise."
            );
        }
        if (e instanceof WorkOrder w) {
            store.find(Asset.class, w.assetId);
            ApiException.require(
                Set.of("P1", "P2", "P3", "P4").contains(w.priority) &&
                    Set.of("CORRECTIVE", "PREVENTIVE").contains(w.type),
                "WORK_ORDER_INPUT",
                "PrioritÃ©/type invalide."
            );
        }
        if (e instanceof Shift s) ApiException.require(
            s.startsAt.isBefore(s.endsAt) &&
                Duration.between(s.startsAt, s.endsAt).equals(Duration.ofHours(8)),
            "SHIFT_HOURS",
            "Un poste doit durer huit heures."
        );
        if (e instanceof ShiftAssignment a) checkAssignment(a);
        if (e instanceof Absence a) ApiException.require(
            a.startsAt.isBefore(a.endsAt),
            "ABSENCE_DATES",
            "Dates d'absence invalides."
        );
        if (e instanceof Handover h) {
            var from = store.find(Shift.class, h.fromShiftId);
            var to = store.find(Shift.class, h.toShiftId);
            ApiException.require(
                !from.id.equals(to.id) && from.endsAt.equals(to.startsAt),
                "HANDOVER_POSTS",
                "Choisissez deux postes successifs."
            );
        }
        if (e instanceof Contract c) ApiException.require(
            !c.endsOn.isBefore(c.startsOn),
            "CONTRACT_DATES",
            "Dates du contrat invalides."
        );
        if (e instanceof ExternalIntervention m) {
            var contract = store.find(Contract.class, m.contractId);
            ApiException.require(
                contract.contractorId.equals(m.contractorId),
                "CONTRACTOR_MISMATCH",
                "Entreprise diffÃ©rente du contrat."
            );
            if (m.plannedAt != null) {
                var date = m.plannedAt.atZone(ZoneId.of("Africa/Tunis")).toLocalDate();
                ApiException.require(
                    !date.isBefore(contract.startsOn) && !date.isAfter(contract.endsOn),
                    "CONTRACT_EXPIRED",
                    "Visite hors pÃ©riode du contrat."
                );
            }
        }
        if (e instanceof MaterialRequest m) positive(m.quantity);
        if (e instanceof TeamMember m) ApiException.require(
            m.validUntil == null || !m.validUntil.isBefore(m.validFrom),
            "MEMBERSHIP_DATES",
            "Dates d'appartenance invalides."
        );
        if (e instanceof Downtime d) ApiException.require(
            d.endsAt == null || !d.endsAt.isBefore(d.startsAt),
            "DOWNTIME_DATES",
            "Dates d'indisponibilitÃ© invalides."
        );
    }

    void checkAssignment(ShiftAssignment a) {
        var shift = store.lock(Shift.class, a.shiftId);
        store
            .manager()
            .createNativeQuery("select id from app_user where id=:id for update")
            .setParameter("id", a.userId)
            .getSingleResult();
        long count = store
            .all(ShiftAssignment.class)
            .stream()
            .filter(x -> x.shiftId.equals(a.shiftId) && !x.id.equals(a.id))
            .count();
        ApiException.require(count < 2, "SHIFT_FULL", "Deux techniciens maximum par poste.");
        for (var other : store.all(ShiftAssignment.class)) {
            if (other.id.equals(a.id) || !other.userId.equals(a.userId)) continue;
            var s = store.find(Shift.class, other.shiftId);
            ApiException.require(
                !shift.startsAt.isBefore(s.endsAt) || !s.startsAt.isBefore(shift.endsAt),
                "SHIFT_OVERLAP",
                "Affectation chevauchante."
            );
        }
        for (var absence : store.all(Absence.class))
            if (
                absence.userId.equals(a.userId) && "APPROUVEE".equals(absence.status)
            ) ApiException.require(
                !shift.startsAt.isBefore(absence.endsAt) ||
                    !absence.startsAt.isBefore(shift.endsAt),
                "USER_ABSENT",
                "Technicien absent sur ce poste."
            );
    }

    public static void positive(BigDecimal value) {
        ApiException.require(
            value != null && value.signum() > 0,
            "POSITIVE_QUANTITY",
            "QuantitÃ© strictement positive requise."
        );
    }
}
