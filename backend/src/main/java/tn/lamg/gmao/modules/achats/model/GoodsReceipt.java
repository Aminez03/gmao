package tn.lamg.gmao.modules.achats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "goods_receipt")
public class GoodsReceipt extends BaseEntity {

    @NotNull
    @Column(name = "order_id")
    public UUID orderId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "delivery_reference")
    public String deliveryReference;

    @NotNull
    @Column(name = "quantity")
    public BigDecimal quantity;

    @Column(name = "accepted")
    public BigDecimal accepted;

    @Column(name = "rejected")
    public BigDecimal rejected;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @Size(max = 10000)
    @Column(name = "quality_note")
    public String qualityNote;
}
