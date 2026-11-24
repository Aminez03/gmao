package tn.lamg.gmao.modules.achats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "reservation")
public class Reservation extends BaseEntity {

    @NotNull
    @Column(name = "work_order_id")
    public UUID workOrderId;

    @NotNull
    @Column(name = "item_id")
    public UUID itemId;

    @NotNull
    @Column(name = "quantity")
    public BigDecimal quantity;

    @NotNull
    @Column(name = "allocated")
    public BigDecimal allocated;

    @NotNull
    @Column(name = "consumed")
    public BigDecimal consumed;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;
}
