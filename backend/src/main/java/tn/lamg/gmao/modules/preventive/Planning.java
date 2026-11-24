package tn.lamg.gmao.modules.preventive;

import java.time.*;
import tn.lamg.gmao.common.ApiException;

public final class Planning {

    private Planning() {}

    public static ZonedDateTime occurrence(
        LocalDateTime anchor,
        String zone,
        String frequency,
        int interval,
        long sequence
    ) {
        ApiException.require(interval > 0 && sequence >= 0, "PLAN_RULE", "Intervalle invalide.");
        var start = anchor.atZone(ZoneId.of(zone));
        long n = Math.multiplyExact(interval, sequence);
        return switch (frequency) {
            case "DAILY" -> start.plusDays(n);
            case "WEEKLY" -> start.plusWeeks(n);
            case "MONTHLY" -> start.plusMonths(n);
            case "QUARTERLY" -> start.plusMonths(3 * n);
            case "SEMESTRAL" -> start.plusMonths(6 * n);
            case "ANNUAL" -> start.plusYears(n);
            default -> throw new ApiException(422, "FREQUENCY", "FrÃ©quence inconnue.");
        };
    }

    public static Instant next(
        String mode,
        LocalDateTime anchor,
        String zone,
        String frequency,
        int interval,
        Instant validated
    ) {
        if ("GLISSANT".equals(mode)) return occurrence(
            validated.atZone(ZoneId.of(zone)).toLocalDateTime(),
            zone,
            frequency,
            interval,
            1
        ).toInstant();
        for (long n = 1; n < 100000; n++) {
            var value = occurrence(anchor, zone, frequency, interval, n).toInstant();
            if (value.isAfter(validated)) return value;
        }
        throw new ApiException(422, "PLAN_HORIZON", "Ancre de planification trop ancienne.");
    }
}
