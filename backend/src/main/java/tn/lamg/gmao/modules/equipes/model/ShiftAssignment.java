package tn.lamg.gmao.modules.equipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "shift_assignment")
public class ShiftAssignment extends BaseEntity {

    @NotNull
    @Column(name = "shift_id")
    public UUID shiftId;

    @NotNull
    @Column(name = "user_id")
    public UUID userId;

    @Column(name = "replacement_for_id")
    public UUID replacementForId;
}
