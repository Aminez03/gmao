package tn.lamg.gmao.modules.equipes.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "handover")
public class Handover extends BaseEntity {

    @NotNull
    @Column(name = "from_shift_id")
    public UUID fromShiftId;

    @NotNull
    @Column(name = "to_shift_id")
    public UUID toShiftId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "priority")
    public String priority;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "comment")
    public String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "work_order_ids", columnDefinition = "jsonb")
    public JsonNode workOrderIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "asset_ids", columnDefinition = "jsonb")
    public JsonNode assetIds;

    @Column(name = "acknowledged_at")
    public Instant acknowledgedAt;

    @Column(name = "acknowledged_by")
    public UUID acknowledgedBy;
}
