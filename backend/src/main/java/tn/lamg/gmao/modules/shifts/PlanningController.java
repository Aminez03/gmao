package tn.lamg.gmao.modules.shifts;

import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.equipes.model.*;
import tn.lamg.gmao.security.*;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    final EntityStore store;
    final CurrentUser current;
    final AccessPolicy policy;
    final CommandService commands;
    final UserRepository users;

    public PlanningController(
        EntityStore s,
        CurrentUser c,
        AccessPolicy p,
        CommandService cs,
        UserRepository u
    ) {
        store = s;
        current = c;
        policy = p;
        commands = cs;
        users = u;
    }

    @GetMapping
    public Object calendar(@RequestParam LocalDate from) {
        current.get();
        var zone = ZoneId.of("Africa/Tunis");
        var start = from.atStartOfDay(zone).toInstant();
        var end = from.plusDays(7).atStartOfDay(zone).toInstant();
        return store
            .all(Shift.class)
            .stream()
            .filter(policy::canRead)
            .filter(s -> !s.startsAt.isBefore(start) && s.startsAt.isBefore(end))
            .sorted(Comparator.comparing(s -> s.startsAt))
            .map(s -> {
                var names = store
                    .all(ShiftAssignment.class)
                    .stream()
                    .filter(a -> a.shiftId.equals(s.id))
                    .map(a ->
                        users
                            .findById(a.userId)
                            .map(u -> u.name)
                            .orElse("Compte archivÃ©")
                    )
                    .toList();
                return Map.of(
                    "id",
                    s.id,
                    "name",
                    s.name,
                    "teamName",
                    store.find(Team.class, s.teamId).name,
                    "status",
                    s.status,
                    "localDate",
                    s.startsAt.atZone(zone).toLocalDate(),
                    "hours",
                    s.startsAt.atZone(zone).toLocalTime() +
                        " â€“ " +
                        s.endsAt.atZone(zone).toLocalTime(),
                    "technicians",
                    names
                );
            })
            .toList();
    }

    public record Generate(LocalDate startDate, int days, int cycleDays) {}

    @PostMapping("/generate")
    public Object generate(@RequestHeader("Idempotency-Key") UUID key, @RequestBody Generate b) {
        current.require("ADMIN", "RESPONSABLE_TECHNIQUE");
        ApiException.require(
            b.startDate() != null &&
                b.days() > 0 &&
                b.days() <= 31 &&
                b.cycleDays() > 0 &&
                b.cycleDays() <= 31,
            "ROTATION_INPUT",
            "DÃ©finissez une pÃ©riode et un cycle de 1 Ã  31 jours."
        );
        return commands.run(key, "planning/generate", b, () -> {
            var teams = store
                .all(Team.class)
                .stream()
                .filter(t -> Set.of("A", "B", "C").contains(t.code))
                .sorted(Comparator.comparing(t -> t.code))
                .toList();
            ApiException.require(
                teams.size() == 3,
                "TEAMS_REQUIRED",
                "CrÃ©ez les Ã©quipes A, B et C avant gÃ©nÃ©ration."
            );
            store.lock(Team.class, teams.getFirst().id);
            int count = 0;
            for (int day = 0; day < b.days(); day++) for (int n = 0; n < 3; n++) {
                var date = b.startDate().plusDays(day);
                var start = date
                    .atTime(6 + 8 * n, 0)
                    .atZone(ZoneId.of("Africa/Tunis"))
                    .toInstant();
                if (
                    store
                        .all(Shift.class)
                        .stream()
                        .anyMatch(s -> s.startsAt.equals(start))
                ) continue;
                var shift = new Shift();
                shift.name = List.of("Matin", "AprÃ¨s-midi", "Nuit").get(n);
                shift.teamId = teams.get((n + day / b.cycleDays()) % 3).id;
                shift.startsAt = start;
                shift.endsAt = start.plus(Duration.ofHours(8));
                shift.status = "BROUILLON";
                shift.createdBy = current.get().id;
                store.save(shift);
                count++;
            }
            return Map.of("created", count);
        });
    }
}
