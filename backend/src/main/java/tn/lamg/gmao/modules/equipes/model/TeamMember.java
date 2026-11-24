package tn.lamg.gmao.modules.equipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "team_member")
public class TeamMember extends BaseEntity {

    @NotNull
    @Column(name = "team_id")
    public UUID teamId;

    @NotNull
    @Column(name = "user_id")
    public UUID userId;

    @NotNull
    @Column(name = "valid_from")
    public LocalDate validFrom;

    @Column(name = "valid_until")
    public LocalDate validUntil;
}
