package tn.lamg.gmao.modules.maintenance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "time_entry")
public class TimeEntry extends BaseEntity {

    @NotNull
    @Column(name = "work_order_id")
    public UUID workOrderId;

    @NotNull
    @Column(name = "user_id")
    public UUID userId;

    @NotNull
    @Column(name = "starts_at")
    public Instant startsAt;

    @NotNull
    @Column(name = "ends_at")
    public Instant endsAt;

    @NotNull
    @Column(name = "minutes")
    public Integer minutes;
}
