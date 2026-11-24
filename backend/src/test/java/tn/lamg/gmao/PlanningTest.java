package tn.lamg.gmao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;
import tn.lamg.gmao.modules.preventive.Planning;

class PlanningTest {

    @Test
    void monthlyRetainsOriginalDay() {
        var anchor = LocalDateTime.parse("2027-01-31T06:00");
        assertEquals(
            LocalDate.of(2027, 2, 28),
            Planning.occurrence(anchor, "Africa/Tunis", "MONTHLY", 1, 1).toLocalDate()
        );
        assertEquals(
            LocalDate.of(2027, 3, 31),
            Planning.occurrence(anchor, "Africa/Tunis", "MONTHLY", 1, 2).toLocalDate()
        );
    }

    @Test
    void leapYear() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            Planning.occurrence(
                LocalDateTime.parse("2024-02-29T06:00"),
                "Africa/Tunis",
                "ANNUAL",
                1,
                1
            ).toLocalDate()
        );
    }

    @Test
    void rollingUsesValidationDate() {
        assertEquals(
            Instant.parse("2027-04-05T09:00:00Z"),
            Planning.next(
                "GLISSANT",
                LocalDateTime.parse("2027-01-31T06:00"),
                "Africa/Tunis",
                "MONTHLY",
                1,
                Instant.parse("2027-03-05T09:00:00Z")
            )
        );
    }

    @Test
    void fixedSkipsMissedDatesWithoutDrift() {
        assertEquals(
            Instant.parse("2027-03-31T05:00:00Z"),
            Planning.next(
                "FIXE",
                LocalDateTime.parse("2027-01-31T06:00"),
                "Africa/Tunis",
                "MONTHLY",
                1,
                Instant.parse("2027-03-05T09:00:00Z")
            )
        );
    }

    @Test
    void dailyAndWeekly() {
        var anchor = LocalDateTime.parse("2027-01-01T22:00");
        assertEquals(3, Planning.occurrence(anchor, "Africa/Tunis", "DAILY", 2, 1).getDayOfMonth());
        assertEquals(
            15,
            Planning.occurrence(anchor, "Africa/Tunis", "WEEKLY", 2, 1).getDayOfMonth()
        );
    }

    @Test
    void quarterlyAndSemestral() {
        var anchor = LocalDateTime.parse("2027-01-01T22:00");
        assertEquals(
            4,
            Planning.occurrence(anchor, "Africa/Tunis", "QUARTERLY", 1, 1).getMonthValue()
        );
        assertEquals(
            7,
            Planning.occurrence(anchor, "Africa/Tunis", "SEMESTRAL", 1, 1).getMonthValue()
        );
    }

    @Test
    void invalidPeriodRefused() {
        assertThrows(RuntimeException.class, () ->
            Planning.occurrence(LocalDateTime.now(), "Africa/Tunis", "DAILY", 0, 1)
        );
    }
}
