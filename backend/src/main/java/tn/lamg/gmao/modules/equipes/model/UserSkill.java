package tn.lamg.gmao.modules.equipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "user_skill")
public class UserSkill extends BaseEntity {

    @NotNull
    @Column(name = "user_id")
    public UUID userId;

    @NotNull
    @Column(name = "skill_id")
    public UUID skillId;

    @NotNull
    @Column(name = "valid_until")
    public LocalDate validUntil;
}
