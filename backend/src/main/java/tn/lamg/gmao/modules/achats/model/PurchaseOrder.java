package tn.lamg.gmao.modules.achats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseEntity {

    @NotNull
    @Column(name = "request_id")
    public UUID requestId;

    @NotNull
    @Column(name = "supplier_id")
    public UUID supplierId;

    @NotNull
    @Column(name = "quantity")
    public BigDecimal quantity;

    @NotNull
    @Column(name = "unit_price")
    public BigDecimal unitPrice;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @Column(name = "expected_on")
    public LocalDate expectedOn;
}
