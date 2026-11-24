package tn.lamg.gmao.modules.achats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "inventory_item")
public class InventoryItem extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "code")
    public String code;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "unit")
    public String unit;

    @NotNull
    @Column(name = "stock")
    public BigDecimal stock;

    @NotNull
    @Column(name = "reserved")
    public BigDecimal reserved;

    @NotNull
    @Column(name = "minimum")
    public BigDecimal minimum;

    @NotNull
    @Column(name = "unit_cost")
    public BigDecimal unitCost;
}
