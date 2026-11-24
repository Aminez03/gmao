package tn.lamg.gmao.common;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ResourceRegistry {

    private final Map<String, Class<? extends BaseEntity>> types = new LinkedHashMap<>();

    public ResourceRegistry() {
        add("locations", tn.lamg.gmao.modules.patrimoine.model.Location.class);
        add("assets", tn.lamg.gmao.modules.patrimoine.model.Asset.class);
        add("teams", tn.lamg.gmao.modules.equipes.model.Team.class);
        add("skills", tn.lamg.gmao.modules.equipes.model.Skill.class);
        add("user-skills", tn.lamg.gmao.modules.equipes.model.UserSkill.class);
        add("team-members", tn.lamg.gmao.modules.equipes.model.TeamMember.class);
        add("shifts", tn.lamg.gmao.modules.equipes.model.Shift.class);
        add("shift-assignments", tn.lamg.gmao.modules.equipes.model.ShiftAssignment.class);
        add("absences", tn.lamg.gmao.modules.equipes.model.Absence.class);
        add("handovers", tn.lamg.gmao.modules.equipes.model.Handover.class);
        add("checklists", tn.lamg.gmao.modules.maintenance.model.Checklist.class);
        add("preventive-plans", tn.lamg.gmao.modules.maintenance.model.PreventivePlan.class);
        add("preventive-occurrences", tn.lamg.gmao.modules.maintenance.model.PreventiveOccurrence.class);
        add("work-orders", tn.lamg.gmao.modules.maintenance.model.WorkOrder.class);
        add("failure-reports", tn.lamg.gmao.modules.maintenance.model.FailureReport.class);
        add("time-entries", tn.lamg.gmao.modules.maintenance.model.TimeEntry.class);
        add("anomalies", tn.lamg.gmao.modules.maintenance.model.Anomaly.class);
        add("downtimes", tn.lamg.gmao.modules.maintenance.model.Downtime.class);
        add("suppliers", tn.lamg.gmao.modules.achats.model.Supplier.class);
        add("inventory/items", tn.lamg.gmao.modules.achats.model.InventoryItem.class);
        add("inventory/reservations", tn.lamg.gmao.modules.achats.model.Reservation.class);
        add("inventory/movements", tn.lamg.gmao.modules.achats.model.StockMovement.class);
        add("purchase-requests", tn.lamg.gmao.modules.achats.model.PurchaseRequest.class);
        add("purchase-orders", tn.lamg.gmao.modules.achats.model.PurchaseOrder.class);
        add("goods-receipts", tn.lamg.gmao.modules.achats.model.GoodsReceipt.class);
        add("subcontractors", tn.lamg.gmao.modules.prestataires.model.Contractor.class);
        add("contracts", tn.lamg.gmao.modules.prestataires.model.Contract.class);
        add("external-interventions", tn.lamg.gmao.modules.prestataires.model.ExternalIntervention.class);
        add("material-requests", tn.lamg.gmao.modules.prestataires.model.MaterialRequest.class);
        add("notifications", tn.lamg.gmao.modules.pilotage.model.Notification.class);
        add("audit-events", tn.lamg.gmao.modules.pilotage.model.AuditEvent.class);
        add("attachments", tn.lamg.gmao.modules.patrimoine.model.Attachment.class);
    }

    private void add(String name, Class<? extends BaseEntity> type) {
        types.put(name, type);
    }

    public Class<? extends BaseEntity> type(String name) {
        var type = types.get(name);
        if (type == null) throw new ApiException(404, "RESOURCE_NOT_FOUND", "Ressource inconnue");
        return type;
    }

    public Set<String> routes() {
        return types.keySet();
    }
}
