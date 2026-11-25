package tn.lamg.gmao.modules.prestataires.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "contract")
public class Contract extends BaseEntity {

    @NotBlank
    @Size(max = 32)
    @Column(name = "number", nullable = false, unique = true, updatable = false)
    public String number;

    @NotNull
    @Column(name = "contractor_id")
    public UUID contractorId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "reference")
    public String reference;

    @NotNull
    @Column(name = "starts_on")
    public LocalDate startsOn;

    @NotNull
    @Column(name = "ends_on")
    public LocalDate endsOn;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "scope")
    public String scope;

    @Column(name = "amount")
    public BigDecimal amount;
}
