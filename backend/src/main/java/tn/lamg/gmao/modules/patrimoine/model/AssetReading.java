package tn.lamg.gmao.modules.patrimoine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "asset_reading")
public class AssetReading extends BaseEntity {

    @NotNull
    @Column(name = "asset_id", nullable = false)
    public UUID assetId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "work_order_id")
    public UUID workOrderId;

    @NotBlank
    @Size(max = 120)
    @Column(name = "metric", nullable = false, length = 120)
    public String metric;

    @NotNull
    @Digits(integer = 18, fraction = 6)
    @Column(name = "reading_value", nullable = false, precision = 24, scale = 6)
    public BigDecimal value;

    @NotBlank
    @Size(max = 40)
    @Column(name = "unit", nullable = false, length = 40)
    public String unit;

    @NotNull
    @Column(name = "recorded_at", nullable = false)
    public Instant recordedAt = Instant.now();

    @Size(max = 1000)
    @Column(name = "note", length = 1000)
    public String note;
}
