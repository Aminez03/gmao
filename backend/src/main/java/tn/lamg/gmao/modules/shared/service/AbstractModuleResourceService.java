package tn.lamg.gmao.modules.shared.service;

import java.util.Map;
import java.util.UUID;
import tn.lamg.gmao.modules.shared.repository.ModuleRepository;
import tn.lamg.gmao.service.ResourceService;

/** Keeps shared CRUD mechanics in one place and gives each module an explicit boundary. */
public abstract class AbstractModuleResourceService implements ModuleResourceService {

    private final ModuleRepository repository;
    private final ResourceService resources;

    protected AbstractModuleResourceService(
        ModuleRepository repository,
        ResourceService resources
    ) {
        this.repository = repository;
        this.resources = resources;
    }

    @Override
    public boolean supports(String route) {
        return repository.owns(route);
    }

    @Override
    public Map<String, Object> list(
        String route,
        int page,
        int size,
        String search,
        String status
    ) {
        return resources.list(route, page, size, search, status);
    }

    @Override
    public Map<String, Object> get(String route, UUID id) {
        return resources.get(route, id);
    }

    @Override
    public Object create(String route, Map<String, Object> fields) {
        return resources.create(route, fields);
    }

    @Override
    public Object update(String route, UUID id, String version, Map<String, Object> fields) {
        return resources.update(route, id, version, fields);
    }

    @Override
    public Object archive(String route, UUID id, String version) {
        return resources.archive(route, id, version);
    }
}
