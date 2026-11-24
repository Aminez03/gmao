package tn.lamg.gmao.service;

import static tn.lamg.gmao.common.ApiException.require;
import static tn.lamg.gmao.common.Inputs.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.achats.model.*;
import tn.lamg.gmao.modules.equipes.model.*;
import tn.lamg.gmao.modules.maintenance.model.*;
import tn.lamg.gmao.modules.patrimoine.model.*;
import tn.lamg.gmao.modules.pilotage.model.*;
import tn.lamg.gmao.modules.prestataires.model.*;
import tn.lamg.gmao.modules.preventive.*;
import tn.lamg.gmao.security.*;

@Service
public class WorkflowService {

    final EntityStore store;
    final CurrentUser current;
    final AccessPolicy policy;
    final ResourceRegistry registry;
    final Events events;
    final ObjectMapper json;
    final DtoMapper mapper;
    final ResourceService resources;
    final PreventiveService preventive;

    public WorkflowService(
        EntityStore s,
        CurrentUser c,
        AccessPolicy p,
        ResourceRegistry r,
        Events e,
        ObjectMapper j,
        DtoMapper m,
        ResourceService rs,
        PreventiveService ps
    ) {
        store = s;
        current = c;
        policy = p;
        registry = r;
        events = e;
        json = j;
        mapper = m;
        resources = rs;
        preventive = ps;
    }

    public Object act(
        String route,
        UUID id,
        String action,
        String version,
        Map<String, Object> body
    ) {
        var entity = store.find(registry.type(route), id);
        policy.read(entity);
        if (entity instanceof WorkOrder w && w.planId != null) store.lock(
            PreventivePlan.class,
            w.planId
        );
        store.manager().lock(entity, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        store.expect(entity, version);
        if (entity instanceof WorkOrder w) work(w, action, body);
        else if (entity instanceof PurchaseRequest p) purchase(p, action, body);
        else if (entity instanceof PurchaseOrder p) {
            current.require("ADMIN", "SERVICE_ACHATS");
            if (action.equals("place")) {
                require(p.status.equals("BROUILLON"), "ORDER_STATE", "Commande dÃ©jÃ  envoyÃ©e.");
                p.status = "COMMANDEE";
                store.find(PurchaseRequest.class, p.requestId).status = "COMMANDEE";
            } else if (action.equals("cancel")) {
                require(
                    p.status.equals("BROUILLON"),
                    "ORDER_STATE",
                    "Annulation rÃ©servÃ©e au brouillon."
                );
                required(body, "reason");
                p.status = "ANNULEE";
            } else unknown();
        } else if (entity instanceof GoodsReceipt r) {
            current.require("ADMIN", "SERVICE_ACHATS", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("quality")) unknown();
            quality(r, body);
        } else if (entity instanceof InventoryItem item) {
            current.require("ADMIN", "SERVICE_ACHATS");
            if (!action.equals("adjust")) unknown();
            var qty = number(body, "quantity");
            ResourceService.positive(qty);
            String type = required(body, "type");
            require(
                Set.of("ADJUST_IN", "ADJUST_OUT").contains(type),
                "MOVEMENT_TYPE",
                "Type ADJUST_IN ou ADJUST_OUT attendu."
            );
            if (type.equals("ADJUST_OUT")) require(
                qty.compareTo(item.stock.subtract(item.reserved)) <= 0,
                "INSUFFICIENT_STOCK",
                "Stock disponible insuffisant."
            );
            item.stock = type.equals("ADJUST_IN") ? item.stock.add(qty) : item.stock.subtract(qty);
            movement(item, null, null, type, qty, item.unitCost, required(body, "reason"));
            allocate(item);
        } else if (entity instanceof Reservation r) {
            current.require("ADMIN", "SERVICE_ACHATS", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("release")) unknown();
            required(body, "reason");
            var item = store.lock(InventoryItem.class, r.itemId);
            item.reserved = item.reserved.subtract(r.allocated.subtract(r.consumed));
            r.allocated = r.consumed;
            r.quantity = r.consumed.signum() > 0 ? r.consumed : r.quantity;
            r.status = "LIBEREE";
            r.archived = true;
        } else if (entity instanceof ShiftAssignment a) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("withdraw")) unknown();
            required(body, "reason");
            a.archived = true;
            store.find(Shift.class, a.shiftId).status = "BROUILLON";
        } else if (entity instanceof Checklist c) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("publish")) unknown();
            resources.check(c);
            c.published = true;
        } else if (entity instanceof PreventivePlan p) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (action.equals("generate")) preventive.generate(p.id);
            else if (action.equals("disable")) {
                required(body, "reason");
                p.active = false;
            } else unknown();
        } else if (entity instanceof FailureReport f) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("qualify")) unknown();
            require(f.workOrderId == null, "ALREADY_QUALIFIED", "DÃ©claration dÃ©jÃ  qualifiÃ©e.");
            var data = new LinkedHashMap<String, Object>();
            data.put("title", required(body, "title"));
            data.put("description", f.description);
            data.put("assetId", f.assetId);
            data.put("priority", f.priority);
            data.put("type", "CORRECTIVE");
            var result = resources.create("work-orders", data);
            f.workOrderId = UUID.fromString(((Map<?, ?>) result).get("id").toString());
            f.status = "QUALIFIEE";
        } else if (entity instanceof Shift s) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("publish")) unknown();
            long n = store
                .all(ShiftAssignment.class)
                .stream()
                .filter(a -> a.shiftId.equals(s.id))
                .count();
            require(n >= 1 && n <= 2, "SHIFT_COVERAGE", "Un Ã  deux techniciens requis.");
            for (var a : store.all(ShiftAssignment.class))
                if (a.shiftId.equals(s.id)) resources.check(a);
            s.status = "PUBLIE";
        } else if (entity instanceof Absence a) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            require(a.status.equals("SOUMISE"), "ABSENCE_STATE", "Absence dÃ©jÃ  traitÃ©e.");
            if (action.equals("approve")) {
                for (var assignment : store.all(ShiftAssignment.class))
                    if (assignment.userId.equals(a.userId)) {
                        var shift = store.find(Shift.class, assignment.shiftId);
                        require(
                            !shift.startsAt.isBefore(a.endsAt) ||
                                !a.startsAt.isBefore(shift.endsAt),
                            "REPLACEMENT_REQUIRED",
                            "Remplacez les affectations concernÃ©es avant approbation."
                        );
                    }
                a.status = "APPROUVEE";
            } else if (action.equals("reject")) {
                required(body, "reason");
                a.status = "REFUSEE";
            } else unknown();
        } else if (entity instanceof Handover h) {
            if (!action.equals("acknowledge")) unknown();
            require(
                policy.ownShift(h.toShiftId, current.get().id) || policy.supervisor(),
                "HANDOVER_RECIPIENT",
                "Vous n'Ãªtes pas membre du poste entrant."
            );
            require(h.acknowledgedAt == null, "ALREADY_ACKNOWLEDGED", "Consigne dÃ©jÃ  reÃ§ue.");
            h.acknowledgedAt = Instant.now();
            h.acknowledgedBy = current.get().id;
        } else if (entity instanceof Anomaly a) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (action.equals("resolve")) {
                a.disposition = required(body, "reason");
                a.resolvedBy = current.get().id;
                a.resolvedAt = Instant.now();
            } else if (action.equals("corrective-request")) {
                require(a.correctiveReportId == null, "ALREADY_LINKED", "Demande dÃ©jÃ  crÃ©Ã©e.");
                var w = store.find(WorkOrder.class, a.workOrderId);
                var data = Map.<String, Object>of(
                    "assetId",
                    w.assetId,
                    "description",
                    a.description,
                    "priority",
                    w.priority
                );
                var result = (Map<?, ?>) resources.create("failure-reports", data);
                a.correctiveReportId = UUID.fromString(result.get("id").toString());
            } else unknown();
        } else if (entity instanceof Notification n) {
            if (!action.equals("read")) unknown();
            n.readAt = Instant.now();
        } else if (entity instanceof ExternalIntervention m) external(m, action, body);
        else if (entity instanceof MaterialRequest m) {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            if (!action.equals("approve") && !action.equals("reject")) unknown();
            m.status = action.equals("approve") ? "APPROUVEE" : "REFUSEE";
        } else unknown();
        store.save(entity);
        events.audit(
            route,
            entity,
            action,
            "Motif : " + text(body, "reason") + " ; note : " + text(body, "note")
        );
        return mapper.dto(entity);
    }

    private void work(WorkOrder w, String action, Map<String, Object> b) {
        switch (action) {
            case "assign" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(
                    Set.of("NOUVELLE", "AFFECTEE", "EN_ATTENTE").contains(w.status),
                    "INVALID_TRANSITION",
                    "Intervention dÃ©jÃ  commencÃ©e."
                );
                w.technicianId = uuid(b, "technicianId");
                w.teamId = uuid(b, "teamId");
                w.contractorId = uuid(b, "contractorId");
                require(
                    w.technicianId != null || w.contractorId != null,
                    "ASSIGNEE_REQUIRED",
                    "Choisissez un technicien ou prestataire."
                );
                if (w.technicianId != null) {
                    var user = store.manager().find(UserAccount.class, w.technicianId);
                    require(
                        user != null &&
                            user.active &&
                            Set.of("TECHNICIEN", "ADMIN", "RESPONSABLE_TECHNIQUE").contains(
                                user.role
                            ),
                        "ASSIGNEE_INVALID",
                        "Technicien indisponible."
                    );
                    checkSkill(w);
                }
                w.status = "AFFECTEE";
                events.notify(
                    w.technicianId,
                    "Intervention affectÃ©e : " + w.number,
                    "work-orders",
                    w.id
                );
            }
            case "start" -> {
                policy.execute(w);
                require(
                    w.status.equals("AFFECTEE"),
                    "INVALID_TRANSITION",
                    "Affectez l'intervention avant dÃ©marrage."
                );
                require(
                    Boolean.TRUE.equals(b.get("safetyConfirmed")),
                    "SAFETY_REQUIRED",
                    "Confirmez les autorisations et contrÃ´les de sÃ©curitÃ©."
                );
                checkSkill(w);
                w.safetyConfirmed = true;
                w.startedAt = Instant.now();
                w.status = "EN_COURS";
            }
            case "wait" -> {
                policy.execute(w);
                require(
                    w.status.equals("EN_COURS"),
                    "INVALID_TRANSITION",
                    "Intervention non commencÃ©e."
                );
                w.waitReason = required(b, "reason");
                require(
                    Set.of("PIECE", "PRESTATAIRE", "AUTORISATION", "AUTRE").contains(w.waitReason),
                    "WAIT_REASON",
                    "Motif d'attente invalide."
                );
                w.waitNote = required(b, "note");
                w.status = "EN_ATTENTE";
            }
            case "resume" -> {
                policy.execute(w);
                require(
                    w.status.equals("EN_ATTENTE"),
                    "INVALID_TRANSITION",
                    "Intervention non en attente."
                );
                if ("PIECE".equals(w.waitReason)) require(
                    ready(w.id),
                    "MISSING_PARTS",
                    "PiÃ¨ces encore indisponibles."
                );
                w.status = "EN_COURS";
                checkSkill(w);
            }
            case "answers" -> {
                policy.execute(w);
                require(
                    w.status.equals("EN_COURS"),
                    "INVALID_TRANSITION",
                    "DÃ©marrez l'intervention avant saisie."
                );
                var answers = json.valueToTree(b.get("answers"));
                require(
                    answers != null && answers.isObject(),
                    "ANSWER_FORMAT",
                    "RÃ©ponses attendues sous forme d'objet."
                );
                w.answers = answers;
            }
            case "time" -> {
                policy.execute(w);
                require(
                    Set.of("EN_COURS", "EN_ATTENTE").contains(w.status),
                    "TIME_STATE",
                    "Intervention non ouverte."
                );
                var t = new TimeEntry();
                t.workOrderId = w.id;
                t.userId = current.get().id;
                t.startsAt = instant(b, "startsAt");
                t.endsAt = instant(b, "endsAt");
                long minutes = Duration.between(t.startsAt, t.endsAt).toMinutes();
                require(
                    minutes > 0 && minutes <= 1440,
                    "TIME_DURATION",
                    "DurÃ©e attendue entre une minute et 24 heures."
                );
                for (var other : store.all(TimeEntry.class))
                    if (other.userId.equals(t.userId)) require(
                        !t.startsAt.isBefore(other.endsAt) || !other.startsAt.isBefore(t.endsAt),
                        "TIME_OVERLAP",
                        "Temps dÃ©jÃ  comptabilisÃ© sur cette pÃ©riode."
                    );
                t.minutes = (int) minutes;
                t.createdBy = current.get().id;
                store.save(t);
                w.actualMinutes = store
                    .all(TimeEntry.class)
                    .stream()
                    .filter(x -> x.workOrderId.equals(w.id))
                    .mapToInt(x -> x.minutes)
                    .sum();
            }
            case "complete" -> {
                policy.execute(w);
                require(
                    w.status.equals("EN_COURS"),
                    "INVALID_TRANSITION",
                    "Intervention non en cours."
                );
                checkAnswers(w);
                require(
                    w.actualMinutes != null && w.actualMinutes > 0,
                    "TIME_REQUIRED",
                    "Enregistrez le temps rÃ©ellement passÃ©."
                );
                w.diagnostic = required(b, "diagnostic");
                w.cause = required(b, "cause");
                w.solution = required(b, "solution");
                w.report = required(b, "report");
                w.completedAt = Instant.now();
                w.status = "TERMINEE";
                events.supervisors("Intervention Ã  valider : " + w.number, "work-orders", w.id);
            }
            case "validate" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(
                    w.status.equals("TERMINEE"),
                    "INVALID_TRANSITION",
                    "Intervention non soumise."
                );
                require(
                    !current.get().id.equals(w.technicianId) &&
                        store
                            .all(TimeEntry.class)
                            .stream()
                            .noneMatch(
                                t -> t.workOrderId.equals(w.id) && t.userId.equals(current.get().id)
                            ),
                    "SELF_VALIDATION_FORBIDDEN",
                    "Un second validateur est requis."
                );
                checkAnswers(w);
                w.status = "VALIDEE";
                w.validatedAt = Instant.now();
                w.validatedBy = current.get().id;
                releaseRemaining(w);
                store.save(w);
                preventive.validated(w, w.validatedAt);
            }
            case "reject-validation" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(
                    w.status.equals("TERMINEE"),
                    "INVALID_TRANSITION",
                    "Intervention non soumise."
                );
                w.waitNote = required(b, "reason");
                w.status = "EN_COURS";
            }
            case "cancel" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(
                    !Set.of("VALIDEE", "ANNULEE").contains(w.status),
                    "INVALID_TRANSITION",
                    "Intervention terminale."
                );
                w.waitNote = required(b, "reason");
                w.status = "ANNULEE";
                releaseRemaining(w);
                if (w.occurrenceId != null) store
                    .find(PreventiveOccurrence.class, w.occurrenceId)
                    .status = "ANNULEE";
            }
            case "reserve" -> {
                policy.execute(w);
                reserve(w, b);
            }
            case "consume" -> {
                policy.execute(w);
                consume(w, b);
            }
            default -> unknown();
        }
    }

    private void releaseRemaining(WorkOrder w) {
        var reservations = store
            .all(Reservation.class)
            .stream()
            .filter(r -> r.workOrderId.equals(w.id))
            .sorted(Comparator.comparing(r -> r.itemId))
            .toList();
        for (var r : reservations) {
            var item = store.lock(InventoryItem.class, r.itemId);
            item.reserved = item.reserved.subtract(r.allocated.subtract(r.consumed));
            r.allocated = r.consumed;
            r.status = "LIBEREE";
            r.archived = true;
        }
    }

    private void checkSkill(WorkOrder w) {
        if (w.technicianId == null) return;
        for (var a : store.all(Absence.class))
            if (a.userId.equals(w.technicianId) && "APPROUVEE".equals(a.status)) require(
                Instant.now().isBefore(a.startsAt) || !Instant.now().isBefore(a.endsAt),
                "USER_ABSENT",
                "Technicien absent."
            );
        if (w.requiredSkillId != null) require(
            store
                .all(UserSkill.class)
                .stream()
                .anyMatch(
                    s ->
                        s.userId.equals(w.technicianId) &&
                        s.skillId.equals(w.requiredSkillId) &&
                        !s.validUntil.isBefore(LocalDate.now(ZoneId.of("Africa/Tunis")))
                ),
            "SKILL_REQUIRED",
            "Habilitation absente ou expirÃ©e."
        );
    }

    private void checkAnswers(WorkOrder w) {
        for (var step : w.checklistSnapshot) {
            String key = step.path("key").asText();
            var answer = w.answers.get(key);
            if (!step.path("required").asBoolean() && (answer == null || answer.isNull())) continue;
            require(
                answer != null && !answer.isNull(),
                "CHECKLIST_REQUIRED",
                "Ã‰tape obligatoire : " + step.path("label").asText()
            );
            String type = step.path("type").asText();
            if (Set.of("CHECKBOX", "ATTESTATION").contains(type)) require(
                answer.isBoolean() && answer.asBoolean(),
                "CHECKLIST_REQUIRED",
                "Validation obligatoire : " + key
            );
            if (type.equals("TEXT")) require(
                answer.isTextual() && !answer.asText().isBlank(),
                "CHECKLIST_TYPE",
                "Texte obligatoire : " + key
            );
            boolean anomaly = false;
            if (type.equals("CONFORMITY")) {
                require(
                    answer.isTextual() &&
                        Set.of("CONFORME", "NON_CONFORME").contains(answer.asText()),
                    "CHECKLIST_TYPE",
                    "ConformitÃ© invalide."
                );
                anomaly = answer.asText().equals("NON_CONFORME");
            }
            if (Set.of("NUMBER", "MEASURE").contains(type)) {
                require(
                    answer.isNumber(),
                    "CHECKLIST_TYPE",
                    "Mesure numÃ©rique obligatoire : " + key
                );
                var v = answer.decimalValue();
                anomaly =
                    (step.hasNonNull("min") && v.compareTo(step.get("min").decimalValue()) < 0) ||
                    (step.hasNonNull("max") && v.compareTo(step.get("max").decimalValue()) > 0);
            }
            if (type.equals("PHOTO")) {
                require(answer.isTextual(), "PHOTO_REQUIRED", "Photo attendue.");
                var file = store.find(Attachment.class, UUID.fromString(answer.asText()));
                require(
                    file.targetType.equals("work-orders") &&
                        file.targetId.equals(w.id) &&
                        file.contentType.startsWith("image/"),
                    "PHOTO_TARGET",
                    "Photo non rattachÃ©e Ã  cette intervention."
                );
            }
            if (anomaly) require(
                store
                    .all(Anomaly.class)
                    .stream()
                    .anyMatch(
                        a ->
                            a.workOrderId.equals(w.id) &&
                            key.equals(a.stepKey) &&
                            a.resolvedAt != null
                    ),
                "UNRESOLVED_ANOMALY",
                "Anomalie Ã  traiter pour l'Ã©tape : " + key
            );
        }
        require(
            store
                .all(Anomaly.class)
                .stream()
                .noneMatch(a -> a.workOrderId.equals(w.id) && a.resolvedAt == null),
            "UNRESOLVED_ANOMALY",
            "Des anomalies restent ouvertes."
        );
    }

    private void purchase(PurchaseRequest p, String action, Map<String, Object> b) {
        switch (action) {
            case "submit" -> {
                require(
                    p.createdBy.equals(current.get().id) || policy.supervisor(),
                    "NOT_REQUESTER",
                    "RÃ©servÃ© au demandeur."
                );
                require(p.status.equals("BROUILLON"), "PURCHASE_STATE", "Demande dÃ©jÃ  soumise.");
                p.status = "SOUMISE";
                events.supervisors(
                    "Demande d'achat Ã  approuver : " + p.number,
                    "purchase-requests",
                    p.id
                );
            }
            case "approve" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(p.status.equals("SOUMISE"), "PURCHASE_STATE", "Demande non soumise.");
                require(
                    !p.createdBy.equals(current.get().id),
                    "SELF_APPROVAL_FORBIDDEN",
                    "Un autre approbateur est requis."
                );
                p.status = "APPROUVEE_TECHNIQUE";
                p.approvedBy = current.get().id;
                p.approvedAt = Instant.now();
            }
            case "reject" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(p.status.equals("SOUMISE"), "PURCHASE_STATE", "Demande non soumise.");
                p.decisionReason = required(b, "reason");
                p.status = "BROUILLON";
            }
            case "send-admin" -> {
                current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
                require(
                    p.status.equals("APPROUVEE_TECHNIQUE"),
                    "PURCHASE_STATE",
                    "Approbation requise."
                );
                p.status = "TRANSMISE_ADMIN";
            }
            default -> unknown();
        }
    }

    private void quality(GoodsReceipt r, Map<String, Object> b) {
        require(
            r.status.equals("QUARANTAINE"),
            "ALREADY_INSPECTED",
            "RÃ©ception dÃ©jÃ  contrÃ´lÃ©e."
        );
        var accepted = number(b, "accepted");
        var rejected = number(b, "rejected");
        require(
            accepted.signum() >= 0 &&
                rejected.signum() >= 0 &&
                accepted.add(rejected).compareTo(r.quantity) == 0,
            "QUALITY_QUANTITIES",
            "AcceptÃ© + refusÃ© doit Ã©galer la quantitÃ© reÃ§ue."
        );
        r.accepted = accepted;
        r.rejected = rejected;
        r.qualityNote = required(b, "note");
        r.status = rejected.signum() > 0 ? "CONTROLEE_AVEC_REFUS" : "CONTROLEE";
        var order = store.find(PurchaseOrder.class, r.orderId);
        var request = store.find(PurchaseRequest.class, order.requestId);
        var item = store.lock(InventoryItem.class, request.itemId);
        if (accepted.signum() > 0) {
            item.stock = item.stock.add(accepted);
            movement(
                item,
                null,
                r.id,
                "IN",
                accepted,
                order.unitPrice,
                "RÃ©ception conforme " + r.deliveryReference
            );
            allocate(item);
        }
        var orders = store
            .matching(PurchaseOrder.class, "requestId", request.id)
            .stream()
            .filter(o -> !"ANNULEE".equals(o.status))
            .toList();
        var receipts = orders
            .stream()
            .flatMap(o -> store.matching(GoodsReceipt.class, "orderId", o.id).stream())
            .toList();
        var conforming = receipts
            .stream()
            .map(x -> x.accepted)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean inspected =
            orders
                .stream()
                .map(o -> o.quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(request.quantity) >= 0 &&
            receipts
                .stream()
                .map(x -> x.quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(request.quantity) >= 0 &&
            receipts.stream().noneMatch(x -> "QUARANTAINE".equals(x.status));
        request.status =
            conforming.compareTo(request.quantity) >= 0
                ? "DISPONIBLE"
                : inspected
                  ? "CONTROLEE_AVEC_REFUS"
                  : "PARTIELLEMENT_RECUE";
        events.notify(
            request.createdBy,
            "Reception controlee : " + r.deliveryReference,
            "purchase-requests",
            request.id
        );
    }

    private void reserve(WorkOrder w, Map<String, Object> b) {
        require(
            !Set.of("VALIDEE", "ANNULEE").contains(w.status),
            "WORK_ORDER_CLOSED",
            "Intervention terminale."
        );
        var item = store.lock(InventoryItem.class, uuid(b, "itemId"));
        var qty = number(b, "quantity");
        ResourceService.positive(qty);
        var r = new Reservation();
        r.workOrderId = w.id;
        r.itemId = item.id;
        r.quantity = qty;
        r.allocated = BigDecimal.ZERO;
        r.consumed = BigDecimal.ZERO;
        r.status = "EN_ATTENTE";
        r.createdBy = current.get().id;
        store.save(r);
        allocate(item);
    }

    private void allocate(InventoryItem item) {
        var needs = store
            .all(Reservation.class)
            .stream()
            .filter(r -> r.itemId.equals(item.id) && r.allocated.compareTo(r.quantity) < 0)
            .sorted(Comparator.comparing(r -> r.createdAt))
            .toList();
        for (var r : needs) {
            var available = item.stock.subtract(item.reserved);
            var amount = r.quantity.subtract(r.allocated).min(available);
            if (amount.signum() > 0) {
                r.allocated = r.allocated.add(amount);
                item.reserved = item.reserved.add(amount);
                r.status = r.allocated.compareTo(r.quantity) == 0 ? "RESERVEE" : "PARTIELLE";
                if (ready(r.workOrderId)) events.supervisors(
                    "PiÃ¨ces disponibles : intervention Ã  replanifier",
                    "work-orders",
                    r.workOrderId
                );
            }
        }
    }

    private boolean ready(UUID work) {
        return store
            .all(Reservation.class)
            .stream()
            .filter(r -> r.workOrderId.equals(work))
            .allMatch(r -> r.allocated.compareTo(r.quantity) >= 0);
    }

    private void consume(WorkOrder w, Map<String, Object> b) {
        require(
            w.status.equals("EN_COURS"),
            "WORK_ORDER_STATE",
            "Consommation rÃ©servÃ©e Ã  une intervention en cours."
        );
        var r = store.find(Reservation.class, uuid(b, "reservationId"));
        require(
            r.workOrderId.equals(w.id),
            "RESERVATION_TARGET",
            "RÃ©servation d'une autre intervention."
        );
        var item = store.lock(InventoryItem.class, r.itemId);
        store.manager().lock(r, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var qty = number(b, "quantity");
        ResourceService.positive(qty);
        require(
            qty.compareTo(r.allocated.subtract(r.consumed)) <= 0,
            "INSUFFICIENT_STOCK",
            "QuantitÃ© non rÃ©servÃ©e."
        );
        item.stock = item.stock.subtract(qty);
        item.reserved = item.reserved.subtract(qty);
        r.consumed = r.consumed.add(qty);
        if (r.consumed.compareTo(r.quantity) == 0) r.status = "CONSOMMEE";
        movement(item, w.id, null, "OUT", qty, item.unitCost, "Consommation intervention");
    }

    private void movement(
        InventoryItem item,
        UUID work,
        UUID inspection,
        String type,
        BigDecimal qty,
        BigDecimal cost,
        String reason
    ) {
        var m = new StockMovement();
        m.itemId = item.id;
        m.workOrderId = work;
        m.inspectionId = inspection;
        m.type = type;
        m.quantity = qty;
        m.unitCost = cost;
        m.reason = reason;
        m.createdBy = current.get().id;
        store.save(m);
    }

    private void external(ExternalIntervention m, String action, Map<String, Object> b) {
        if (action.equals("submit-report")) {
            require(
                current.has("SOUS_TRAITANT") || policy.supervisor(),
                "FORBIDDEN",
                "RÃ´le non autorisÃ©."
            );
            require(m.status.equals("PLANIFIEE"), "MISSION_STATE", "Mission dÃ©jÃ  soumise.");
            m.report = required(b, "report");
            m.visitedAt = Instant.now();
            m.status = "SOUMISE";
            events.supervisors("Rapport prestataire Ã  valider", "external-interventions", m.id);
        } else {
            current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
            require(m.status.equals("SOUMISE"), "MISSION_STATE", "Rapport non soumis.");
            if (action.equals("approve")) {
                m.status = "VALIDEE";
                m.approvedBy = current.get().id;
            } else if (action.equals("reject")) {
                required(b, "reason");
                m.status = "PLANIFIEE";
            } else unknown();
        }
    }

    private static void unknown() {
        throw new ApiException(404, "UNKNOWN_ACTION", "Action inconnue.");
    }
}
