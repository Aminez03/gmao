package tn.lamg.gmao.modules.shared.service;

import java.util.Map;
import java.util.UUID;

/** CRUD boundary exposed by a business module while preserving shared API conventions. */
public interface ModuleResourceService {
    boolean supports(String route);

    Map<String, Object> list(String route, int page, int size, String search, String status);

    Map<String, Object> get(String route, UUID id);

    Object create(String route, Map<String, Object> fields);

    Object update(String route, UUID id, String version, Map<String, Object> fields);

    Object archive(String route, UUID id, String version);
}
