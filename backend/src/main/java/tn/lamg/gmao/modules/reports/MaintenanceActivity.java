package tn.lamg.gmao.modules.reports;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import tn.lamg.gmao.modules.maintenance.model.WorkOrder;

final class MaintenanceActivity {

    record Activity(String grouping, List<Map<String, Object>> series) {}

    static Activity build(Instant start, Instant end, List<WorkOrder> orders) {
        var zone = ZoneId.of("Africa/Tunis");
        var first = start.atZone(zone).toLocalDate();
        // The end of the selected period is exclusive, including at midnight.
        var last = end.minusNanos(1).atZone(zone).toLocalDate();
        long days = ChronoUnit.DAYS.between(first, last) + 1;
        String grouping = days <= 31 ? "day" : days <= 90 ? "week" : "month";
        var series = new ArrayList<Map<String, Object>>();
        for (var day = first; !day.isAfter(last); ) {
            var next = switch (grouping) {
                case "month" -> day.withDayOfMonth(1).plusMonths(1);
                case "week" -> day.plusWeeks(1);
                default -> day.plusDays(1);
            };
            var a = day.atStartOfDay(zone).toInstant();
            var b = next.atStartOfDay(zone).toInstant();
            long preventive = 0;
            long corrective = 0;
            for (var order : orders) {
                var created = order.createdAt;
                if (
                    created.isBefore(start) ||
                    !created.isBefore(end) ||
                    created.isBefore(a) ||
                    !created.isBefore(b)
                ) continue;
                if ("PREVENTIVE".equals(order.type)) preventive++;
                if ("CORRECTIVE".equals(order.type)) corrective++;
            }
            var bucketLast = next.minusDays(1).isAfter(last) ? last : next.minusDays(1);
            series.add(
                Map.of(
                    "day",
                    day.toString(),
                    "lastDay",
                    bucketLast.toString(),
                    "preventive",
                    preventive,
                    "corrective",
                    corrective
                )
            );
            day = next;
        }
        return new Activity(grouping, series);
    }
}
