package tn.lamg.gmao.modules.achats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "stock_movement")
public class StockMovement extends BaseEntity {

    @NotNull
    @Column(name = "item_id")
    public UUID itemId;

    @Column(name = "work_order_id")
    public UUID workOrderId;

    @Column(name = "inspection_id")
    public UUID inspectionId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "type")
    public String type;

    @NotNull
    @Column(name = "quantity")
    public BigDecimal quantity;

    @NotNull
    @Column(name = "unit_cost")
    public BigDecimal unitCost;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "reason")
    public String reason;
}
