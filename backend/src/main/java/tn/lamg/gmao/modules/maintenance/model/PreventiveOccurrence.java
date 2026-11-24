package tn.lamg.gmao.modules.maintenance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "preventive_occurrence")
public class PreventiveOccurrence extends BaseEntity {

    @NotNull
    @Column(name = "plan_id")
    public UUID planId;

    @NotNull
    @Column(name = "due_at")
    public Instant dueAt;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;
}
