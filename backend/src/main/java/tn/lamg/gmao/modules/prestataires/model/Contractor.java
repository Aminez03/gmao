package tn.lamg.gmao.modules.prestataires.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import tn.lamg.gmao.common.BaseEntity;

@Entity
@Table(name = "contractor")
public class Contractor extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "code")
    public String code;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    public String name;

    @Size(max = 255)
    @Column(name = "email")
    public String email;

    @Size(max = 255)
    @Column(name = "phone")
    public String phone;

    @Size(max = 10000)
    @Column(name = "skills")
    public String skills;
}
