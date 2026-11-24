package tn.lamg.gmao.modules.assistant.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.security.*;

/** Read-only data retrieval using the same row and financial permissions as the application. */
@Service
public class AssistantDataService {

    private static final int LIMIT = 12;
    private static final Set<String> CLOSED = Set.of(
        "VALIDEE",
        "ANNULEE",
        "TERMINEE",
        "FERMEE",
        "CLOTUREE",
        "RESOLUE",
        "QUALIFIEE"
    );
    private static final Set<String> FIELDS = Set.of(
        "code",
        "name",
        "number",
        "title",
        "status",
        "priority",
        "type",
        "category",
        "criticality",
        "stock",
        "reserved",
        "minimum",
        "unit",
        "quantity",
        "allocated",
        "consumed",
        "accepted",
        "rejected",
        "startsAt",
        "endsAt",
        "dueAt",
        "plannedAt",
        "visitedAt",
        "startsOn",
        "endsOn",
        "validUntil",
        "validFrom",
        "frequency",
        "nextDueAt",
        "active",
        "published",
        "revision",
        "minutes",
        "actualMinutes",
        "severity",
        "acknowledgedAt",
        "resolvedAt",
        "userNames",
        "reference",
        "designation",
        "brand",
        "model",
        "description",
        "comment",
        "report",
        "diagnostic",
        "cause",
        "solution",
        "qualityNote",
        "waitNote",
        "scope"
    );
    private static final Set<String> COSTS = Set.of("unitCost", "unitPrice", "amount", "cost");
    private static final List<Module> MODULES = List.of(
        new Module("attachments", "Documents", "piece jointe"),
        new Module("inventory/movements", "Mouvements de stock", "mouvement"),
        new Module("inventory/reservations", "Réservations de pièces", "reservation"),
        new Module("inventory/items", "Articles en stock", "stock|piece|article|inventaire"),
        new Module("shift-assignments", "Affectations de poste", "affectation|remplacement"),
        new Module("team-members", "Membres des équipes", "membre"),
        new Module("user-skills", "Habilitations des utilisateurs", "habilitation"),
        new Module("skills", "Compétences", "competence"),
        new Module("handovers", "Passations de consignes", "passation|consigne|transmission"),
        new Module("absences", "Absences", "absence|conge"),
        new Module("shifts", "Postes planifiés", "poste|planning|rotation"),
        new Module("teams", "Équipes", "equipe"),
        new Module(
            "preventive-occurrences",
            "Échéances préventives",
            "occurrence|echeance preventive"
        ),
        new Module("preventive-plans", "Plans préventifs", "plan preventif|plans preventifs"),
        new Module("checklists", "Gammes et checklists", "gamme|checklist"),
        new Module("time-entries", "Temps enregistrés", "temps|duree|chronometre"),
        new Module("downtimes", "Arrêts des équipements", "arret|indisponibilite"),
        new Module("anomalies", "Anomalies", "anomalie"),
        new Module("failure-reports", "Pannes signalées", "panne|signalement|depannage"),
        new Module("purchase-requests", "Demandes d’achat", "demande d'achat|demandes d'achat"),
        new Module("purchase-orders", "Commandes", "commande|achat"),
        new Module("goods-receipts", "Réceptions", "reception|livraison"),
        new Module("suppliers", "Fournisseurs", "fournisseur"),
        new Module(
            "external-interventions",
            "Interventions externes",
            "intervention externe|interventions externes|mission"
        ),
        new Module("material-requests", "Demandes de matériel", "materiel"),
        new Module("subcontractors", "Prestataires", "prestataire|sous.traitant"),
        new Module("contracts", "Contrats", "contrat"),
        new Module("locations", "Localisations", "localisation|emplacement|local"),
        new Module("assets", "Équipements", "equipement|patrimoine|machine"),
        new Module("notifications", "Notifications", "notification"),
        new Module("audit-events", "Journal d’audit", "audit|journal"),
        new Module("attachments", "Documents", "document|photo|piece jointe"),
        new Module(
            "work-orders",
            "Ordres de travail",
            "\\bot\\b|\\bots\\b|ordre de travail|ordres de travail|intervention|urgent|retard"
        )
    );
    private final ResourceRegistry registry;
    private final EntityStore store;
    private final AccessPolicy policy;
    private final DtoMapper mapper;
    private final CurrentUser current;

    public AssistantDataService(
        ResourceRegistry registry,
        EntityStore store,
        AccessPolicy policy,
        DtoMapper mapper,
        CurrentUser current
    ) {
        this.registry = registry;
        this.store = store;
        this.policy = policy;
        this.mapper = mapper;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public Context resolve(String message) {
        current.get();
        String question = normalize(message);
        Module module = MODULES.stream()
            .filter(item ->
                java.util.regex.Pattern.compile(item.keywords()).matcher(question).find()
            )
            .findFirst()
            .orElse(null);
        if (module == null) return Context.empty();
        // Never accept a class, table, field or SQL query supplied by the model or the user.
        List<? extends BaseEntity> authorized = store
            .all(registry.type(module.route()))
            .stream()
            .filter(entity -> !entity.archived && policy.canRead(entity))
            .toList();
        List<Map<String, Object>> rows = mapper.dtos(authorized);
        boolean lowStock =
            module.route().equals("inventory/items") &&
            question.matches(".*(minimum|seuil|rupture|insuffisant|alerte).*");
        boolean urgent =
            module.route().equals("work-orders") &&
            question.matches(".*(urgent|critique|\\bp1\\b).*");
        boolean overdue =
            module.route().equals("work-orders") && question.matches(".*(retard|depassee).*");
        boolean open = question.matches(".*(ouvert|en cours|actif|a traiter).*");
        Set<String> requestedStatuses = question.contains("en cours")
            ? Set.of("EN_COURS")
            : question.contains("en attente")
              ? Set.of("EN_ATTENTE")
              : question.contains("valide")
                ? Set.of("VALIDEE", "VALIDE")
                : question.contains("termine")
                  ? Set.of("TERMINEE")
                  : question.contains("annule")
                    ? Set.of("ANNULEE")
                    : question.contains("publie")
                      ? Set.of("PUBLIE", "PUBLIEE")
                      : Set.of();
        List<Map<String, Object>> matches = rows
            .stream()
            .filter(row -> {
                if (
                    lowStock &&
                    decimal(row.get("stock")).compareTo(decimal(row.get("minimum"))) >= 0
                ) return false;
                String status = Objects.toString(row.get("status"), "");
                if (requestedStatuses.contains("PUBLIE") && row.containsKey("published")) {
                    if (!Boolean.TRUE.equals(row.get("published"))) return false;
                } else if (
                    !requestedStatuses.isEmpty() && !requestedStatuses.contains(status)
                ) return false;
                if ((urgent || overdue || open) && CLOSED.contains(status)) return false;
                if (urgent && !"P1".equals(row.get("priority"))) return false;
                if (
                    overdue &&
                    (!(row.get("dueAt") instanceof Instant due) || !due.isBefore(Instant.now()))
                ) return false;
                return true;
            })
            .toList();
        boolean financial = current.has(
            "ADMIN",
            "RESPONSABLE_TECHNIQUE",
            "DIRECTION",
            "SERVICE_ACHATS"
        );
        List<Map<String, Object>> projected = matches
            .stream()
            .limit(LIMIT)
            .map(row -> {
                Map<String, Object> safe = new LinkedHashMap<>();
                for (var entry : row.entrySet()) {
                    if (
                        FIELDS.contains(entry.getKey()) ||
                        (financial && COSTS.contains(entry.getKey()))
                    ) {
                        Object value = entry.getValue();
                        if (value instanceof String text) value = text.substring(
                            0,
                            Math.min(text.length(), 300)
                        );
                        if (value != null) safe.put(entry.getKey(), value);
                    }
                }
                return safe;
            })
            .toList();
        List<Reference> references = matches
            .stream()
            .limit(LIMIT)
            .filter(row -> row.get("id") != null)
            .map(row -> new Reference(label(row), module.route(), row.get("id").toString()))
            .toList();
        String heading = lowStock
            ? "Articles dont le stock physique est inférieur au minimum"
            : urgent
              ? "Ordres de travail ouverts de priorité P1"
              : overdue
                ? "Ordres de travail en retard"
                : module.label() + (open ? " · éléments ouverts" : "");
        if (!requestedStatuses.isEmpty()) heading += " · " + String.join(", ", requestedStatuses);
        StringBuilder answer = new StringBuilder(heading)
            .append(" : ")
            .append(matches.size())
            .append(" résultat(s) dans votre périmètre.\n");
        for (int i = 0; i < projected.size(); i++) {
            var row = projected.get(i);
            answer
                .append("\n")
                .append(i + 1)
                .append(". ")
                .append(label(row));
            if (module.route().equals("inventory/items")) {
                answer
                    .append(" — stock : ")
                    .append(row.get("stock"))
                    .append(" ")
                    .append(row.getOrDefault("unit", ""))
                    .append(" ; minimum : ")
                    .append(row.get("minimum"));
            } else {
                if (row.get("status") != null) answer
                    .append(" — ")
                    .append(row.get("status").toString().replace('_', ' '));
                if (row.get("priority") != null) answer.append(" · ").append(row.get("priority"));
                if (row.get("dueAt") != null) answer
                    .append(" · échéance : ")
                    .append(row.get("dueAt"));
                if (row.get("startsAt") != null) answer
                    .append(" · début : ")
                    .append(row.get("startsAt"));
                if (row.get("endsAt") != null) answer.append(" · fin : ").append(row.get("endsAt"));
                for (String field : List.of("comment", "description", "report")) {
                    if (row.get(field) != null) answer.append("\n   ").append(row.get(field));
                }
            }
        }
        if (matches.size() > LIMIT) answer
            .append("\n\nLes ")
            .append(LIMIT)
            .append(
                " premiers résultats sont affichés. Consultez le module pour la liste complète."
            );
        if (matches.isEmpty()) answer.append(
            "\nAucun élément correspondant n’est accessible à votre compte."
        );
        boolean direct =
            lowStock ||
            urgent ||
            overdue ||
            question.matches(".*(quels|quelles|montre|affiche|liste|combien|donne|recherche).*");
        return new Context(
            module.route(),
            matches.size(),
            projected,
            answer.toString(),
            references,
            direct
        );
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private static String label(Map<String, Object> row) {
        String userName =
            row.get("userNames") instanceof Map<?, ?> users
                ? Objects.toString(users.get("userId"), "Fiche")
                : "Fiche";
        String code = Objects.toString(
            row.getOrDefault("number", row.getOrDefault("code", "")),
            ""
        );
        String name = Objects.toString(
            row.getOrDefault(
                "name",
                row.getOrDefault(
                    "title",
                    row.getOrDefault("designation", row.getOrDefault("reference", userName))
                )
            ),
            "Fiche"
        );
        return code.isBlank() ? name : code + " · " + name;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(
            text.toLowerCase(Locale.ROOT).replace('’', '\''),
            Normalizer.Form.NFD
        )
            .replaceAll("\\p{M}", "")
            .replaceAll("\\s+", " ")
            .strip();
    }

    private record Module(String route, String label, String keywords) {}

    public record Reference(String label, String route, String id) {}

    public record Context(
        String route,
        int total,
        List<Map<String, Object>> items,
        String answer,
        List<Reference> references,
        boolean direct
    ) {
        public static Context empty() {
            return new Context("", 0, List.of(), "", List.of(), false);
        }

        public boolean available() {
            return !route.isEmpty();
        }
    }
}
