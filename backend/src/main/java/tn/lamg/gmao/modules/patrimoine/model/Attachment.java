package tn.lamg.gmao.modules.patrimoine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "attachment")
public class Attachment extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "target_type")
    public String targetType;

    @NotNull
    @Column(name = "target_id")
    public UUID targetId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "content_type")
    public String contentType;

    @NotNull
    @Column(name = "size")
    public Long size;
}
