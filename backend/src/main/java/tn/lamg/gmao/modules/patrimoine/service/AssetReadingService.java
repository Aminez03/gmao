package tn.lamg.gmao.modules.patrimoine.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.maintenance.model.WorkOrder;
import tn.lamg.gmao.modules.patrimoine.model.*;
import tn.lamg.gmao.security.*;

@Service
public class AssetReadingService {

    private final EntityStore store;
    private final CurrentUser current;
    private final AccessPolicy policy;
    private final DtoMapper mapper;

    public AssetReadingService(
        EntityStore store,
        CurrentUser current,
        AccessPolicy policy,
        DtoMapper mapper
    ) {
        this.store = store;
        this.current = current;
        this.policy = policy;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID assetId) {
        var asset = store.find(Asset.class, assetId);
        policy.read(asset);
        var values = new ArrayList<>(store.matching(AssetReading.class, "assetId", assetId));
        values.sort(Comparator.comparing((AssetReading r) -> r.recordedAt).reversed());
        return mapper.dtos(values);
    }

    @Transactional
    public Map<String, Object> create(UUID assetId, Map<String, Object> input) {
        current.require("ADMIN", "RESPONSABLE_TECHNIQUE", "TECHNICIEN", "SOUS_TRAITANT");
        var asset = store.find(Asset.class, assetId);
        policy.read(asset);
        String metric = text(input, "metric", 120);
        String unit = text(input, "unit", 40);
        BigDecimal value;
        try {
            value = new BigDecimal(String.valueOf(input.get("value")));
        } catch (RuntimeException ex) {
            throw new ApiException(
                422,
                "INVALID_READING",
                "La valeur du relevé doit être numérique."
            );
        }
        ApiException.require(
            value.precision() <= 24 && value.scale() <= 6,
            "INVALID_READING",
            "La valeur dépasse la précision autorisée."
        );
        Instant recordedAt;
        try {
            recordedAt =
                input.get("recordedAt") == null
                    ? Instant.now()
                    : Instant.parse(String.valueOf(input.get("recordedAt")));
        } catch (RuntimeException ex) {
            throw new ApiException(422, "INVALID_READING_DATE", "La date du relevé est invalide.");
        }
        String note = input.get("note") == null ? null : String.valueOf(input.get("note")).trim();
        if (note != null && note.length() > 1000) throw new ApiException(
            422,
            "INVALID_NOTE",
            "La note est limitée à 1 000 caractères."
        );
        UUID workOrderId = null;
        if (
            input.get("workOrderId") != null && !String.valueOf(input.get("workOrderId")).isBlank()
        ) {
            try {
                workOrderId = UUID.fromString(String.valueOf(input.get("workOrderId")));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(
                    422,
                    "INVALID_WORK_ORDER",
                    "L'ordre de travail est invalide."
                );
            }
            WorkOrder order = store.find(WorkOrder.class, workOrderId);
            ApiException.require(
                assetId.equals(order.assetId),
                "ASSET_MISMATCH",
                "L'ordre de travail concerne un autre équipement."
            );
            policy.execute(order);
        }
        var reading = new AssetReading();
        reading.assetId = assetId;
        reading.userId = current.get().id;
        reading.workOrderId = workOrderId;
        reading.metric = metric;
        reading.value = value;
        reading.unit = unit;
        reading.recordedAt = recordedAt;
        reading.note = note;
        reading.createdBy = current.get().id;
        return mapper.dto(store.save(reading));
    }

    private static String text(Map<String, Object> input, String field, int max) {
        String value = Objects.toString(input.get(field), "").trim();
        if (value.isBlank() || value.length() > max) throw new ApiException(
            422,
            "INVALID_READING",
            "Le champ « " + field + " » est obligatoire (" + max + " caractères maximum)."
        );
        return value;
    }
}
