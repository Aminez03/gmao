package tn.lamg.gmao.modules.prestataires.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "material_request")
public class MaterialRequest extends BaseEntity {

    @NotNull
    @Column(name = "intervention_id")
    public UUID interventionId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "designation")
    public String designation;

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
}
