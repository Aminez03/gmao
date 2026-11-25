package tn.lamg.gmao.modules.maintenance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "preventive_plan")
public class PreventivePlan extends BaseEntity {

    @NotBlank
    @Size(max = 32)
    @Column(name = "number", nullable = false, unique = true, updatable = false)
    public String number;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @NotNull
    @Column(name = "asset_id")
    public UUID assetId;

    @NotNull
    @Column(name = "checklist_id")
    public UUID checklistId;

    @Column(name = "team_id")
    public UUID teamId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "frequency")
    public String frequency;

    @NotNull
    @Column(name = "interval_count")
    public Integer intervalCount;

    @NotBlank
    @Size(max = 255)
    @Column(name = "mode")
    public String mode;

    @NotBlank
    @Size(max = 255)
    @Column(name = "anchor_local")
    public String anchorLocal;

    @NotBlank
    @Size(max = 255)
    @Column(name = "zone_id")
    public String zoneId;

    @NotNull
    @Column(name = "next_due_at")
    public Instant nextDueAt;

    @Column(name = "active")
    public Boolean active;

    @Column(name = "required_skill_id")
    public UUID requiredSkillId;
}
