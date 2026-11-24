package tn.lamg.gmao.modules.pilotage.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    @NotNull
    @Column(name = "recipient_id")
    public UUID recipientId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "title")
    public String title;

    @Size(max = 255)
    @Column(name = "resource_type")
    public String resourceType;

    @Column(name = "resource_id")
    public UUID resourceId;

    @Column(name = "read_at")
    public Instant readAt;
}
