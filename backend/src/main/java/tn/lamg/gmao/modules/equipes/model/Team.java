package tn.lamg.gmao.modules.equipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "team")
public class Team extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "code")
    public String code;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;
}
