package tn.lamg.gmao.modules.maintenance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "anomaly")
public class Anomaly extends BaseEntity {

    @NotNull
    @Column(name = "work_order_id")
    public UUID workOrderId;

    @Size(max = 255)
    @Column(name = "step_key")
    public String stepKey;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "description")
    public String description;

    @NotBlank
    @Size(max = 255)
    @Column(name = "severity")
    public String severity;

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    @Column(name = "resolved_by")
    public UUID resolvedBy;

    @Size(max = 10000)
    @Column(name = "disposition")
    public String disposition;

    @Column(name = "corrective_report_id")
    public UUID correctiveReportId;
}
