package tn.lamg.gmao.modules.maintenance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "downtime")
public class Downtime extends BaseEntity {

    @NotNull
    @Column(name = "asset_id")
    public UUID assetId;

    @Column(name = "work_order_id")
    public UUID workOrderId;

    @NotNull
    @Column(name = "starts_at")
    public Instant startsAt;

    @Column(name = "ends_at")
    public Instant endsAt;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "reason")
    public String reason;
}
