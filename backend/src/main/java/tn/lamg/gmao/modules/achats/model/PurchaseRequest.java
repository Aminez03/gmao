package tn.lamg.gmao.modules.achats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "purchase_request")
public class PurchaseRequest extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "number")
    public String number;

    @Column(name = "work_order_id")
    public UUID workOrderId;

    @NotNull
    @Column(name = "item_id")
    public UUID itemId;

    @NotNull
    @Column(name = "quantity")
    public BigDecimal quantity;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "justification")
    public String justification;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @Column(name = "approved_by")
    public UUID approvedBy;

    @Column(name = "approved_at")
    public Instant approvedAt;

    @Size(max = 10000)
    @Column(name = "decision_reason")
    public String decisionReason;
}
