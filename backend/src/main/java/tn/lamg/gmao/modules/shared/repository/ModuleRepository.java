package tn.lamg.gmao.modules.shared.repository;

import java.util.Set;

/** Declares the API resources owned by one business module. */
public interface ModuleRepository {
    Set<String> routes();

    default boolean owns(String route) {
        return routes().contains(route);
    }
}
