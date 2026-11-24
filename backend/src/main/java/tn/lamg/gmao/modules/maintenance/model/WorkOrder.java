package tn.lamg.gmao.modules.maintenance.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "work_order")
public class WorkOrder extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "number")
    public String number;

    @NotBlank
    @Size(max = 255)
    @Column(name = "title")
    public String title;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "description")
    public String description;

    @NotNull
    @Column(name = "asset_id")
    public UUID assetId;

    @Column(name = "plan_id")
    public UUID planId;

    @Column(name = "occurrence_id")
    public UUID occurrenceId;

    @Column(name = "checklist_id")
    public UUID checklistId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_snapshot", columnDefinition = "jsonb")
    public JsonNode checklistSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb")
    public JsonNode answers;

    @NotBlank
    @Size(max = 255)
    @Column(name = "type")
    public String type;

    @NotBlank
    @Size(max = 255)
    @Column(name = "status")
    public String status;

    @NotBlank
    @Size(max = 255)
    @Column(name = "priority")
    public String priority;

    @Column(name = "team_id")
    public UUID teamId;

    @Column(name = "technician_id")
    public UUID technicianId;

    @Column(name = "contractor_id")
    public UUID contractorId;

    @Column(name = "required_skill_id")
    public UUID requiredSkillId;

    @Column(name = "due_at")
    public Instant dueAt;

    @Column(name = "started_at")
    public Instant startedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "validated_at")
    public Instant validatedAt;

    @Column(name = "validated_by")
    public UUID validatedBy;

    @Size(max = 255)
    @Column(name = "wait_reason")
    public String waitReason;

    @Size(max = 10000)
    @Column(name = "wait_note")
    public String waitNote;

    @Size(max = 10000)
    @Column(name = "diagnostic")
    public String diagnostic;

    @Size(max = 10000)
    @Column(name = "cause")
    public String cause;

    @Size(max = 10000)
    @Column(name = "solution")
    public String solution;

    @Size(max = 10000)
    @Column(name = "report")
    public String report;

    @Column(name = "safety_confirmed")
    public Boolean safetyConfirmed;

    @Column(name = "actual_minutes")
    public Integer actualMinutes;
}
