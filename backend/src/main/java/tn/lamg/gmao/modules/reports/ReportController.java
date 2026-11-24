package tn.lamg.gmao.modules.reports;

import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.achats.model.*;
import tn.lamg.gmao.modules.maintenance.model.*;
import tn.lamg.gmao.modules.patrimoine.model.*;
import tn.lamg.gmao.security.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    final EntityStore store;
    final CurrentUser current;
    final AccessPolicy policy;

    public ReportController(EntityStore s, CurrentUser c, AccessPolicy p) {
        store = s;
        current = c;
        policy = p;
    }

    @GetMapping({ "/dashboard", "/maintenance" })
    public Object dashboard(
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(required = false) UUID teamId,
        @RequestParam(required = false) UUID assetId
    ) {
        current.get();
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofDays(30)) : from;
        ApiException.require(start.isBefore(end), "REPORT_DATES", "PÃ©riode invalide.");
        var orders = store
            .all(WorkOrder.class)
            .stream()
            .filter(policy::canRead)
            .filter(w -> teamId == null || teamId.equals(w.teamId))
            .filter(w -> assetId == null || assetId.equals(w.assetId))
            .toList();
        var period = orders
            .stream()
            .filter(w -> !w.createdAt.isBefore(start) && w.createdAt.isBefore(end))
            .toList();
        var active = orders
            .stream()
            .filter(w -> !Set.of("VALIDEE", "ANNULEE").contains(w.status))
            .toList();
        var result = new LinkedHashMap<String, Object>();
        result.put("active", active.size());
        result.put(
            "overdue",
            active
                .stream()
                .filter(w -> w.dueAt != null && w.dueAt.isBefore(end))
                .count()
        );
        result.put(
            "critical",
            active
                .stream()
                .filter(w -> w.priority.equals("P1"))
                .count()
        );
        result.put(
            "completed",
            period
                .stream()
                .filter(w -> w.status.equals("VALIDEE"))
                .count()
        );
        result.put(
            "lowStock",
            store
                .all(InventoryItem.class)
                .stream()
                .filter(i -> i.stock.subtract(i.reserved).compareTo(i.minimum) < 0)
                .count()
        );
        result.put(
            "unavailableAssets",
            store
                .all(Asset.class)
                .stream()
                .filter(policy::canRead)
                .filter(a -> a.status.equals("INDISPONIBLE"))
                .count()
        );
        var repairs = orders
            .stream()
            .filter(
                w ->
                    w.type.equals("CORRECTIVE") &&
                    w.completedAt != null &&
                    !w.completedAt.isBefore(start) &&
                    w.completedAt.isBefore(end)
            )
            .toList();
        result.put(
            "mttrMinutes",
            repairs.isEmpty()
                ? null
                : repairs
                      .stream()
                      .mapToInt(w -> w.actualMinutes == null ? 0 : w.actualMinutes)
                      .average()
                      .orElse(0)
        );
        var occurrences = store
            .all(PreventiveOccurrence.class)
            .stream()
            .filter(o -> !o.dueAt.isBefore(start) && o.dueAt.isBefore(end))
            .filter(o -> {
                var p = store.find(PreventivePlan.class, o.planId);
                return (
                    (assetId == null || assetId.equals(p.assetId)) &&
                    (teamId == null || teamId.equals(p.teamId))
                );
            })
            .toList();
        long onTime = occurrences
            .stream()
            .filter(o ->
                orders
                    .stream()
                    .anyMatch(
                        w ->
                            o.id.equals(w.occurrenceId) &&
                            w.validatedAt != null &&
                            !w.validatedAt.isAfter(o.dueAt)
                    )
            )
            .count();
        result.put(
            "preventiveRate",
            occurrences.isEmpty() ? null : (100.0 * onTime) / occurrences.size()
        );
        var assets = store
            .all(Asset.class)
            .stream()
            .filter(policy::canRead)
            .filter(a -> assetId == null || assetId.equals(a.id))
            .toList();
        double observed = Duration.between(start, end).toSeconds() * (double) assets.size();
        double down = assets
            .stream()
            .mapToDouble(a -> downtimeSeconds(a.id, start, end))
            .sum();
        result.put("availability", observed == 0 ? null : (100.0 * (observed - down)) / observed);
        long failures = period
            .stream()
            .filter(w -> w.type.equals("CORRECTIVE") && !w.status.equals("ANNULEE"))
            .count();
        result.put("mtbfHours", failures == 0 ? null : (observed - down) / 3600 / failures);
        if (current.has("ADMIN", "RESPONSABLE_TECHNIQUE", "DIRECTION", "SERVICE_ACHATS")) {
            var ids = orders
                .stream()
                .map(w -> w.id)
                .collect(java.util.stream.Collectors.toSet());
            BigDecimal cost = store
                .all(StockMovement.class)
                .stream()
                .filter(
                    m ->
                        "OUT".equals(m.type) &&
                        ids.contains(m.workOrderId) &&
                        !m.createdAt.isBefore(start) &&
                        m.createdAt.isBefore(end)
                )
                .map(m -> m.quantity.multiply(m.unitCost))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put("partsCost", cost);
            result.put("currency", "TND");
            result.put(
                "costScope",
                "Ce total inclut uniquement les pièces consommées. La main-d’œuvre et les prestations sont exclues."
            );
        }
        var series = new ArrayList<Map<String, Object>>();
        for (int n = 6; n >= 0; n--) {
            var day = end.atZone(ZoneId.of("Africa/Tunis")).toLocalDate().minusDays(n);
            var a = day.atStartOfDay(ZoneId.of("Africa/Tunis")).toInstant();
            var b = a.plus(Duration.ofDays(1));
            series.add(
                Map.of(
                    "day",
                    day.toString(),
                    "preventive",
                    period
                        .stream()
                        .filter(
                            w ->
                                w.type.equals("PREVENTIVE") &&
                                !w.createdAt.isBefore(a) &&
                                w.createdAt.isBefore(b)
                        )
                        .count(),
                    "corrective",
                    period
                        .stream()
                        .filter(
                            w ->
                                w.type.equals("CORRECTIVE") &&
                                !w.createdAt.isBefore(a) &&
                                w.createdAt.isBefore(b)
                        )
                        .count()
                )
            );
        }
        result.put("series", series);
        result.put("from", start);
        result.put("to", end);
        return result;
    }

    private double downtimeSeconds(UUID asset, Instant start, Instant end) {
        var intervals = store
            .all(Downtime.class)
            .stream()
            .filter(
                d ->
                    asset.equals(d.assetId) &&
                    d.startsAt.isBefore(end) &&
                    (d.endsAt == null || d.endsAt.isAfter(start))
            )
            .map(d -> new Instant[] {
                d.startsAt.isBefore(start) ? start : d.startsAt,
                d.endsAt == null || d.endsAt.isAfter(end) ? end : d.endsAt,
            })
            .sorted(Comparator.comparing(a -> a[0]))
            .toList();
        double total = 0;
        Instant a = null,
            b = null;
        for (var interval : intervals) {
            if (a == null) {
                a = interval[0];
                b = interval[1];
            } else if (!interval[0].isAfter(b)) {
                if (interval[1].isAfter(b)) b = interval[1];
            } else {
                total += Duration.between(a, b).toSeconds();
                a = interval[0];
                b = interval[1];
            }
        }
        return total + (a == null ? 0 : Duration.between(a, b).toSeconds());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam String format,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(required = false) UUID teamId,
        @RequestParam(required = false) UUID assetId
    ) throws Exception {
        current.require("ADMIN", "RESPONSABLE_TECHNIQUE", "DIRECTION", "SERVICE_ACHATS");
        var data = (Map<?, ?>) dashboard(from, to, teamId, assetId);
        var out = new ByteArrayOutputStream();
        String mime;
        if (format.equals("xlsx")) {
            try (var workbook = new XSSFWorkbook()) {
                var sheet = workbook.createSheet("Maintenance");
                int n = 0;
                var header = sheet.createRow(n++);
                header.createCell(0).setCellValue("Indicateur");
                header.createCell(1).setCellValue("Valeur");
                for (var entry : MaintenanceReportDocument.indicators(data)) {
                    var row = sheet.createRow(n++);
                    row.createCell(0).setCellValue(entry.label());
                    row.createCell(1).setCellValue(entry.value());
                }
                var startRow = sheet.createRow(n++);
                startRow.createCell(0).setCellValue("Début de la période (heure de Tunis)");
                startRow
                    .createCell(1)
                    .setCellValue(MaintenanceReportDocument.date(data.get("from")));
                var endRow = sheet.createRow(n);
                endRow.createCell(0).setCellValue("Fin de la période (heure de Tunis)");
                endRow.createCell(1).setCellValue(MaintenanceReportDocument.date(data.get("to")));
                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);
                workbook.write(out);
            }
            mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (format.equals("pdf")) {
            out.write(MaintenanceReportDocument.pdf(data));
            mime = "application/pdf";
        } else throw new ApiException(422, "EXPORT_FORMAT", "Format PDF ou XLSX attendu.");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=maintenance." + format)
            .contentType(MediaType.parseMediaType(mime))
            .body(out.toByteArray());
    }
}
