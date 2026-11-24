package tn.lamg.gmao.modules.shared.service;

import tn.lamg.gmao.common.BaseEntity;

/** Business lifecycle hooks owned by one module. */
public interface ResourceRules {
    boolean supports(BaseEntity entity);

    default void defaults(BaseEntity entity) {}

    default void validate(BaseEntity entity) {}

    default void editable(BaseEntity entity) {}
}
