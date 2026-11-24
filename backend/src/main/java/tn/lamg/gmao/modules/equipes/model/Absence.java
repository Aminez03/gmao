package tn.lamg.gmao.modules.equipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "absence")
public class Absence extends BaseEntity {

    @NotNull
    @Column(name = "user_id")
    public UUID userId;

    @NotNull
    @Column(name = "starts_at")
    public Instant startsAt;

    @NotNull
    @Column(name = "ends_at")
    public Instant endsAt;

    @NotBlank
    @Size(max = 255)
    @Column(name = "reason")
    public String reason;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;
}
