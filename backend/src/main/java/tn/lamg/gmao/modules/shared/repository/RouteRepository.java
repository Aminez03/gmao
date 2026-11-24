package tn.lamg.gmao.modules.shared.repository;

import java.util.Set;

/** Shared implementation for repositories that group resources of one module. */
public abstract class RouteRepository implements ModuleRepository {
    private final Set<String> routes;

    protected RouteRepository(String... routes) {
        this.routes = Set.of(routes);
    }

    @Override
    public Set<String> routes() {
        return routes;
    }
}
