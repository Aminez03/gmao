package tn.lamg.gmao.modules.patrimoine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "location")
public class Location extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "code")
    public String code;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "type")
    public String type;

    @Column(name = "parent_id")
    public UUID parentId;
}
