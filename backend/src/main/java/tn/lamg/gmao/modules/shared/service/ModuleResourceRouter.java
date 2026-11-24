package tn.lamg.gmao.modules.shared.service;

import java.util.*;
import org.springframework.stereotype.Service;
import tn.lamg.gmao.common.ApiException;

/** Routes a generic REST resource to the service that owns its business module. */
@Service
public class ModuleResourceRouter {
    private final List<ModuleResourceService> modules;

    public ModuleResourceRouter(List<ModuleResourceService> modules) {
        this.modules = List.copyOf(modules);
    }

    private ModuleResourceService module(String route) {
        return modules
            .stream()
            .filter(module -> module.supports(route))
            .findFirst()
            .orElseThrow(() -> new ApiException(404, "RESOURCE_NOT_FOUND", "Ressource inconnue"));
    }

    public Map<String, Object> list(String route, int page, int size, String search, String status) {
        return module(route).list(route, page, size, search, status);
    }

    public Map<String, Object> get(String route, UUID id) {
        return module(route).get(route, id);
    }

    public Object create(String route, Map<String, Object> fields) {
        return module(route).create(route, fields);
    }

    public Object update(String route, UUID id, String version, Map<String, Object> fields) {
        return module(route).update(route, id, version, fields);
    }

    public Object archive(String route, UUID id, String version) {
        return module(route).archive(route, id, version);
    }
}
