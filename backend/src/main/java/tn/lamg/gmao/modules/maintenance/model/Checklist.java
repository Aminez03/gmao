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
@Table(name = "checklist")
public class Checklist extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @Column(name = "asset_id")
    public UUID assetId;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", columnDefinition = "jsonb")
    public JsonNode steps;

    @NotNull
    @Column(name = "revision")
    public Integer revision;

    @Column(name = "published")
    public Boolean published;
}
