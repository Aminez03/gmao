package tn.lamg.gmao.modules.reports;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tn.lamg.gmao.modules.maintenance.model.WorkOrder;

class MaintenanceActivityTest {

    private WorkOrder order(String date, String type) {
        var order = new WorkOrder();
        order.createdAt = Instant.parse(date);
        order.type = type;
        return order;
    }

    @Test
    void includesFirstDayOfSelectedPeriodAndExcludesEnd() {
        var activity = MaintenanceActivity.build(
            Instant.parse("2026-11-20T10:23:00Z"),
            Instant.parse("2026-11-27T10:25:00Z"),
            List.of(
                order("2026-11-20T19:21:45Z", "PREVENTIVE"),
                order("2026-11-20T19:22:36Z", "CORRECTIVE"),
                order("2026-11-20T10:22:00Z", "CORRECTIVE"),
                order("2026-11-27T10:25:00Z", "CORRECTIVE")
            )
        );
        assertEquals("day", activity.grouping());
        assertEquals(8, activity.series().size());
        assertEquals(1L, activity.series().getFirst().get("preventive"));
        assertEquals(1L, activity.series().getFirst().get("corrective"));
        assertEquals(0L, activity.series().getLast().get("corrective"));
    }

    @Test
    void groupsByWeekWithPartialLastBucket() {
        var activity = MaintenanceActivity.build(
            Instant.parse("2026-10-01T23:00:00Z"),
            Instant.parse("2026-11-03T23:00:00Z"),
            List.of(order("2026-11-03T22:59:00Z", "CORRECTIVE"))
        );
        assertEquals("week", activity.grouping());
        assertEquals(5, activity.series().size());
        assertEquals("2026-11-03", activity.series().getLast().get("lastDay"));
        assertEquals(1L, activity.series().getLast().get("corrective"));
    }

    @Test
    void groupsByCalendarMonthAndKeepsEmptyMonths() {
        var activity = MaintenanceActivity.build(
            Instant.parse("2026-07-14T23:00:00Z"),
            Instant.parse("2026-11-26T23:00:00Z"),
            List.of(order("2026-11-20T19:00:00Z", "PREVENTIVE"))
        );
        assertEquals("month", activity.grouping());
        assertEquals(5, activity.series().size());
        assertEquals("2026-07-15", activity.series().getFirst().get("day"));
        assertEquals("2026-11-26", activity.series().getLast().get("lastDay"));
        assertEquals(0L, activity.series().getFirst().get("preventive"));
        assertEquals(1L, activity.series().getLast().get("preventive"));
    }
}
