package tn.lamg.gmao.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.shared.service.ModuleResourceRouter;
import tn.lamg.gmao.service.WorkflowService;

@RestController
@RequestMapping("/api/v1")
public class ResourceController {

    final ModuleResourceRouter resources;
    final WorkflowService workflows;
    final CommandService commands;

    public ResourceController(ModuleResourceRouter r, WorkflowService w, CommandService c) {
        resources = r;
        workflows = w;
        commands = c;
    }

    public record ResourceWrite(@NotEmpty Map<String, Object> fields) {}

    private String route(String resource, HttpServletRequest req) {
        return req.getRequestURI().startsWith("/api/v1/inventory/")
            ? "inventory/" + resource
            : resource;
    }

    @GetMapping({ "/{resource}", "/inventory/{resource}" })
    public Object list(
        @PathVariable String resource,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        HttpServletRequest req
    ) {
        return resources.list(route(resource, req), page, size, search, status);
    }

    @GetMapping({
        "/{resource}/{id:[0-9a-fA-F\\-]{36}}",
        "/inventory/{resource}/{id:[0-9a-fA-F\\-]{36}}",
    })
    public Object get(
        @PathVariable String resource,
        @PathVariable UUID id,
        HttpServletRequest req
    ) {
        return resources.get(route(resource, req), id);
    }

    @PostMapping({ "/{resource}", "/inventory/{resource}" })
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object create(
        @PathVariable String resource,
        @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody ResourceWrite body,
        HttpServletRequest req
    ) {
        String name = route(resource, req);
        return commands.run(key, name + "/create", body, () ->
            resources.create(name, body.fields())
        );
    }

    @PutMapping({
        "/{resource}/{id:[0-9a-fA-F\\-]{36}}",
        "/inventory/{resource}/{id:[0-9a-fA-F\\-]{36}}",
    })
    public Object update(
        @PathVariable String resource,
        @PathVariable UUID id,
        @RequestHeader(value = "If-Match", required = false) String version,
        @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody ResourceWrite body,
        HttpServletRequest req
    ) {
        String name = route(resource, req);
        return commands.run(key, name + "/" + id + "/" + version, body, () ->
            resources.update(name, id, version, body.fields())
        );
    }

    @DeleteMapping({
        "/{resource}/{id:[0-9a-fA-F\\-]{36}}",
        "/inventory/{resource}/{id:[0-9a-fA-F\\-]{36}}",
    })
    public Object archive(
        @PathVariable String resource,
        @PathVariable UUID id,
        @RequestHeader(value = "If-Match", required = false) String version,
        @RequestHeader("Idempotency-Key") UUID key,
        HttpServletRequest req
    ) {
        String name = route(resource, req);
        return commands.run(key, name + "/" + id + "/archive/" + version, Map.of(), () ->
            resources.archive(name, id, version)
        );
    }

    @PostMapping({
        "/{resource}/{id:[0-9a-fA-F\\-]{36}}/{action}",
        "/inventory/{resource}/{id:[0-9a-fA-F\\-]{36}}/{action}",
    })
    public Object action(
        @PathVariable String resource,
        @PathVariable UUID id,
        @PathVariable String action,
        @RequestHeader(value = "If-Match", required = false) String version,
        @RequestHeader("Idempotency-Key") UUID key,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        String name = route(resource, req);
        return commands.run(key, name + "/" + id + "/" + action + "/" + version, body, () ->
            workflows.act(name, id, action, version, body)
        );
    }
}
