package tn.lamg.gmao.modules.prestataires.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "external_intervention")
public class ExternalIntervention extends BaseEntity {

    @NotNull
    @Column(name = "contract_id")
    public UUID contractId;

    @NotNull
    @Column(name = "work_order_id")
    public UUID workOrderId;

    @NotNull
    @Column(name = "contractor_id")
    public UUID contractorId;

    @Column(name = "planned_at")
    public Instant plannedAt;

    @Column(name = "visited_at")
    public Instant visitedAt;

    @Size(max = 10000)
    @Column(name = "report")
    public String report;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @Column(name = "cost")
    public BigDecimal cost;

    @Column(name = "approved_by")
    public UUID approvedBy;
}
