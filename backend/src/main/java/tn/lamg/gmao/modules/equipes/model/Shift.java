package tn.lamg.gmao.modules.equipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "shift")
public class Shift extends BaseEntity {

    @NotNull
    @Column(name = "team_id")
    public UUID teamId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @NotNull
    @Column(name = "starts_at")
    public Instant startsAt;

    @NotNull
    @Column(name = "ends_at")
    public Instant endsAt;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;
}
