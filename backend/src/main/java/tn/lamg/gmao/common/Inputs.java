package tn.lamg.gmao.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class Inputs {

    private Inputs() {}

    public static String text(Map<String, Object> b, String key) {
        Object v = b.get(key);
        return v == null ? "" : v.toString().trim();
    }

    public static String required(Map<String, Object> b, String key) {
        var value = text(b, key);
        ApiException.require(!value.isBlank(), "REQUIRED_FIELD", "Champ obligatoire : " + key);
        ApiException.require(value.length() <= 10000, "TOO_LONG", "Texte trop long.");
        return value;
    }

    public static UUID uuid(Map<String, Object> b, String key) {
        var s = text(b, key);
        return s.isBlank() ? null : UUID.fromString(s);
    }

    public static BigDecimal number(Map<String, Object> b, String key) {
        return new BigDecimal(required(b, key));
    }

    public static Instant instant(Map<String, Object> b, String key) {
        return Instant.parse(required(b, key));
    }
}
