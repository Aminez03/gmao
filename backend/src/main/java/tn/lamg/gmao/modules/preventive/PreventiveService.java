package tn.lamg.gmao.modules.preventive;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.maintenance.model.*;

@Service
public class PreventiveService {

    final EntityStore store;
    final ObjectMapper json;

    public PreventiveService(EntityStore s, ObjectMapper j) {
        store = s;
        json = j;
    }

    @Scheduled(fixedDelayString = "${gmao.schedule-delay:60000}")
    @Transactional
    public void tick() {
        for (var p : store.all(PreventivePlan.class))
            if (Boolean.TRUE.equals(p.active)) generate(p.id);
    }

    public WorkOrder generate(UUID id) {
        var p = store.lock(PreventivePlan.class, id);
        if (!Boolean.TRUE.equals(p.active)) return null;
        var existing = store
            .all(WorkOrder.class)
            .stream()
            .filter(w -> p.id.equals(w.planId) && !Set.of("VALIDEE", "ANNULEE").contains(w.status))
            .findFirst();
        if (existing.isPresent()) return existing.get();
        if (p.nextDueAt.isAfter(Instant.now().plus(Duration.ofDays(7)))) return null;
        var occurrence = store
            .all(PreventiveOccurrence.class)
            .stream()
            .filter(o -> o.planId.equals(p.id) && o.dueAt.equals(p.nextDueAt))
            .findFirst()
            .orElseGet(() -> {
                var o = new PreventiveOccurrence();
                o.planId = p.id;
                o.dueAt = p.nextDueAt;
                o.status = "EMISE";
                return store.save(o);
            });
        // An already cancelled occurrence requires an explicit supervisor decision, not regeneration.
        if (!"EMISE".equals(occurrence.status)) return null;
        var prior = store
            .all(WorkOrder.class)
            .stream()
            .filter(w -> occurrence.id.equals(w.occurrenceId))
            .findFirst();
        if (prior.isPresent()) return prior.get();
        var c = store.find(Checklist.class, p.checklistId);
        var w = new WorkOrder();
        w.number = "OT-" + w.id.toString().substring(0, 8).toUpperCase();
        w.title = p.name;
        w.description = "Maintenance prÃ©ventive planifiÃ©e";
        w.assetId = p.assetId;
        w.planId = p.id;
        w.occurrenceId = occurrence.id;
        w.checklistId = c.id;
        w.checklistSnapshot = c.steps.deepCopy();
        w.answers = json.createObjectNode();
        w.type = "PREVENTIVE";
        w.status = "NOUVELLE";
        w.priority = "P2";
        w.teamId = p.teamId;
        w.requiredSkillId = p.requiredSkillId;
        w.dueAt = p.nextDueAt;
        w.actualMinutes = 0;
        return store.save(w);
    }

    public void validated(WorkOrder w, Instant now) {
        if (w.planId == null) return;
        var p = store.lock(PreventivePlan.class, w.planId);
        store.find(PreventiveOccurrence.class, w.occurrenceId).status = "REALISEE";
        if (!Boolean.TRUE.equals(p.active)) return;
        var next = Planning.next(
            p.mode,
            LocalDateTime.parse(p.anchorLocal),
            p.zoneId,
            p.frequency,
            p.intervalCount,
            now
        );
        if ("FIXE".equals(p.mode)) {
            for (long n = 1; n < 100000; n++) {
                var due = Planning.occurrence(
                    LocalDateTime.parse(p.anchorLocal),
                    p.zoneId,
                    p.frequency,
                    p.intervalCount,
                    n
                ).toInstant();
                if (!due.isBefore(next)) break;
                if (due.isAfter(w.dueAt)) {
                    var missed = new PreventiveOccurrence();
                    missed.planId = p.id;
                    missed.dueAt = due;
                    missed.status = "MANQUEE";
                    store.save(missed);
                }
            }
        }
        p.nextDueAt = next;
        store.save(p);
        store.manager().flush();
        generate(p.id);
    }
}
