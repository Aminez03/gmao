package tn.lamg.gmao.modules.patrimoine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {

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
    @Column(name = "category")
    public String category;

    @NotNull
    @Column(name = "location_id")
    public UUID locationId;

    @Column(name = "parent_id")
    public UUID parentId;

    @Size(max = 255)
    @Column(name = "brand")
    public String brand;

    @Size(max = 255)
    @Column(name = "model")
    public String model;

    @Size(max = 255)
    @Column(name = "serial_number")
    public String serialNumber;

    @Column(name = "commissioned_on")
    public LocalDate commissionedOn;

    @Column(name = "supplier_id")
    public UUID supplierId;

    @Column(name = "warranty_until")
    public LocalDate warrantyUntil;

    @NotBlank
    @Size(max = 255)
    @Column(name = "criticality")
    public String criticality;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @Column(name = "qr_token")
    public UUID qrToken;
}
