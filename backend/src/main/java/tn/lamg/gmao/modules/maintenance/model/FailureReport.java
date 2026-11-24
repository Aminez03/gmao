package tn.lamg.gmao.modules.maintenance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "failure_report")
public class FailureReport extends BaseEntity {

    @NotNull
    @Column(name = "asset_id")
    public UUID assetId;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "description")
    public String description;

    @NotBlank
    @Size(max = 255)
    @Column(name = "priority")
    public String priority;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @Column(name = "work_order_id")
    public UUID workOrderId;
}
