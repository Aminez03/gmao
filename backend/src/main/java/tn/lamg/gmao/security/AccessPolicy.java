package tn.lamg.gmao.security;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.achats.model.*;
import tn.lamg.gmao.modules.equipes.model.*;
import tn.lamg.gmao.modules.maintenance.model.*;
import tn.lamg.gmao.modules.patrimoine.model.*;
import tn.lamg.gmao.modules.pilotage.model.*;
import tn.lamg.gmao.modules.prestataires.model.*;

@Component
public class AccessPolicy {

    final CurrentUser current;
    final EntityStore store;

    public AccessPolicy(CurrentUser c, EntityStore s) {
        current = c;
        store = s;
    }

    public boolean supervisor() {
        return current.has("ADMIN", "RESPONSABLE_TECHNIQUE");
    }

    public boolean buying() {
        return current.has("ADMIN", "SERVICE_ACHATS");
    }

    public boolean canRead(BaseEntity e) {
        var u = current.get();
        if (e instanceof Notification n) return u.id.equals(n.recipientId);
        if (e instanceof AuditEvent) return supervisor();
        if ("ADMIN".equals(u.role) || "RESPONSABLE_TECHNIQUE".equals(u.role)) return true;
        if (e instanceof Absence a) return u.id.equals(a.userId);
        if (e instanceof UserSkill s) return u.id.equals(s.userId);
        if (e instanceof TimeEntry t) return u.id.equals(t.userId);
        if (e instanceof Attachment a) {
            try {
                return canRead(store.find(typeForTarget(a.targetType), a.targetId));
            } catch (ApiException ex) {
                return false;
            }
        }
        if ("SOUS_TRAITANT".equals(u.role)) {
            if (u.contractorId == null) return false;
            if (e instanceof Contractor c) return u.contractorId.equals(c.id);
            if (e instanceof Contract c) return validContract(c, u);
            if (e instanceof ExternalIntervention m) return (
                u.contractorId.equals(m.contractorId) &&
                validContract(store.find(Contract.class, m.contractId), u)
            );
            if (e instanceof WorkOrder w) return (
                u.contractorId.equals(w.contractorId) &&
                store
                    .all(ExternalIntervention.class)
                    .stream()
                    .anyMatch(m -> m.workOrderId.equals(w.id) && canRead(m))
            );
            if (e instanceof Asset a) return store
                .all(WorkOrder.class)
                .stream()
                .anyMatch(w -> w.assetId.equals(a.id) && canRead(w));
            if (e instanceof Anomaly a) return canRead(store.find(WorkOrder.class, a.workOrderId));
            if (e instanceof MaterialRequest m) return canRead(
                store.find(ExternalIntervention.class, m.interventionId)
            );
            return false;
        }
        if ("TECHNICIEN".equals(u.role)) {
            if (e instanceof WorkOrder w) return (
                u.id.equals(w.technicianId) || teamMember(u.id, w.teamId)
            );
            if (e instanceof Anomaly a) return canRead(store.find(WorkOrder.class, a.workOrderId));
            if (e instanceof ShiftAssignment a) return u.id.equals(a.userId);
            if (e instanceof Handover h) return (
                ownShift(h.fromShiftId, u.id) || ownShift(h.toShiftId, u.id)
            );
            if (e instanceof PurchaseRequest p) return u.id.equals(p.createdBy);
            if (e instanceof FailureReport f) return u.id.equals(f.createdBy);
            return (
                e instanceof Asset ||
                e instanceof Location ||
                e instanceof Skill ||
                e instanceof Team ||
                e instanceof TeamMember ||
                e instanceof Shift ||
                e instanceof Checklist ||
                e instanceof InventoryItem ||
                e instanceof PreventivePlan ||
                e instanceof PreventiveOccurrence ||
                e instanceof Reservation ||
                e instanceof Downtime
            );
        }
        if ("DIRECTION".equals(u.role)) return !(
            e instanceof UserSkill ||
            e instanceof TeamMember ||
            e instanceof ShiftAssignment ||
            e instanceof Handover
        );
        if ("SERVICE_ACHATS".equals(u.role)) return (
            e instanceof InventoryItem ||
            e instanceof Supplier ||
            e instanceof Reservation ||
            e instanceof StockMovement ||
            e instanceof PurchaseRequest ||
            e instanceof PurchaseOrder ||
            e instanceof GoodsReceipt ||
            e instanceof Asset ||
            e instanceof WorkOrder ||
            e instanceof MaterialRequest
        );
        return false;
    }

    public void read(BaseEntity e) {
        if (!canRead(e)) throw new ApiException(404, "NOT_FOUND", "Ressource introuvable.");
    }

    public void manage(BaseEntity e) {
        if (
            e instanceof Notification ||
            e instanceof AuditEvent ||
            e instanceof Attachment ||
            e instanceof PreventiveOccurrence ||
            e instanceof StockMovement ||
            e instanceof Reservation ||
            e instanceof TimeEntry
        ) throw new ApiException(
            403,
            "COMMAND_REQUIRED",
            "Utilisez une commande mÃ©tier pour cette ressource."
        );
        if (supervisor()) return;
        if (
            buying() &&
            (e instanceof Supplier ||
                e instanceof InventoryItem ||
                e instanceof PurchaseOrder ||
                e instanceof GoodsReceipt ||
                e instanceof PurchaseRequest)
        ) return;
        var u = current.get();
        if (
            (e instanceof FailureReport || e instanceof PurchaseRequest) &&
            ("TECHNICIEN".equals(u.role) || "SERVICE_ACHATS".equals(u.role))
        ) return;
        if (e instanceof Absence a && u.id.equals(a.userId)) return;
        if (e instanceof Handover h && ownShift(h.fromShiftId, u.id)) return;
        if (
            e instanceof MaterialRequest m &&
            "SOUS_TRAITANT".equals(u.role) &&
            canRead(store.find(ExternalIntervention.class, m.interventionId))
        ) return;
        if (e instanceof Anomaly a) {
            execute(store.find(WorkOrder.class, a.workOrderId));
            return;
        }
        throw new ApiException(403, "FORBIDDEN", "Modification non autorisÃ©e.");
    }

    public void execute(WorkOrder w) {
        read(w);
        if (
            !(
                supervisor() ||
                current.get().id.equals(w.technicianId) ||
                ("SOUS_TRAITANT".equals(current.get().role) && canRead(w))
            )
        ) throw new ApiException(
            403,
            "NOT_ASSIGNED",
            "Intervention non affectÃ©e Ã  votre compte."
        );
    }

    public boolean ownShift(UUID shift, UUID user) {
        return store
            .all(ShiftAssignment.class)
            .stream()
            .anyMatch(a -> a.shiftId.equals(shift) && a.userId.equals(user));
    }

    public boolean teamMember(UUID user, UUID team) {
        return (
            team != null &&
            store
                .all(TeamMember.class)
                .stream()
                .anyMatch(
                    m ->
                        m.teamId.equals(team) &&
                        m.userId.equals(user) &&
                        !m.validFrom.isAfter(LocalDate.now()) &&
                        (m.validUntil == null || !m.validUntil.isBefore(LocalDate.now()))
                )
        );
    }

    private boolean validContract(Contract c, UserAccount u) {
        var day = LocalDate.now(ZoneId.of("Africa/Tunis"));
        return (
            u.contractorId.equals(c.contractorId) &&
            !day.isBefore(c.startsOn) &&
            !day.isAfter(c.endsOn)
        );
    }

    public Class<? extends BaseEntity> typeForTarget(String type) {
        return switch (type) {
            case "assets" -> Asset.class;
            case "work-orders" -> WorkOrder.class;
            case "failure-reports" -> FailureReport.class;
            case "external-interventions" -> ExternalIntervention.class;
            case "contracts" -> Contract.class;
            default -> throw new ApiException(422, "INVALID_TARGET", "Type de document invalide.");
        };
    }
}
