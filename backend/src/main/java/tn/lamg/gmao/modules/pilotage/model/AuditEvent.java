package tn.lamg.gmao.modules.pilotage.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "audit_event")
public class AuditEvent extends BaseEntity {

    @Column(name = "actor_id")
    public UUID actorId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "action")
    public String action;

    @NotBlank
    @Size(max = 255)
    @Column(name = "resource_type")
    public String resourceType;

    @Column(name = "resource_id")
    public UUID resourceId;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "details")
    public String details;
}
