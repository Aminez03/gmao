package tn.lamg.gmao.modules.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.patrimoine.service.AssetReadingService;
import tn.lamg.gmao.security.*;
import tn.lamg.gmao.service.*;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    final CommandService commands;
    final WorkflowService workflows;
    final ResourceService resources;
    final CurrentUser current;
    final AssetReadingService readings;

    public SyncController(
        CommandService c,
        WorkflowService w,
        ResourceService r,
        CurrentUser u,
        AssetReadingService readings
    ) {
        commands = c;
        workflows = w;
        resources = r;
        current = u;
        this.readings = readings;
    }

    public record Operation(
        @NotNull UUID operationId,
        @NotNull UUID resourceId,
        @NotBlank String action,
        long expectedVersion,
        @NotNull Map<String, Object> payload
    ) {}

    @PostMapping("/operations")
    public Object apply(@RequestBody @Size(min = 1, max = 100) List<@Valid Operation> operations) {
        current.require("ADMIN", "RESPONSABLE_TECHNIQUE", "TECHNICIEN", "SOUS_TRAITANT");
        var results = new ArrayList<Map<String, Object>>();
        var blocked = new HashSet<UUID>();
        for (var op : operations) {
            if (blocked.contains(op.resourceId())) {
                results.add(Map.of("operationId", op.operationId(), "state", "BLOCKED"));
                continue;
            }
            try {
                ApiException.require(
                    Set.of("answers", "time", "asset-reading").contains(op.action()),
                    "OFFLINE_ACTION_DENIED",
                    "Cette action nécessite une connexion directe."
                );
                if (op.action().equals("asset-reading")) {
                    Object result = commands.run(
                        op.operationId(),
                        "asset-readings/" + op.resourceId() + "/create",
                        op.payload(),
                        () -> readings.create(op.resourceId(), op.payload())
                    );
                    results.add(
                        Map.of(
                            "operationId",
                            op.operationId(),
                            "state",
                            "APPLIED",
                            "resource",
                            result
                        )
                    );
                    continue;
                }
                String version = "\"" + op.expectedVersion() + "\"";
                Object result = commands.run(
                    op.operationId(),
                    "work-orders/" + op.resourceId() + "/" + op.action() + "/" + version,
                    op.payload(),
                    () ->
                        workflows.act(
                            "work-orders",
                            op.resourceId(),
                            op.action(),
                            version,
                            op.payload()
                        )
                );
                results.add(
                    Map.of("operationId", op.operationId(), "state", "APPLIED", "resource", result)
                );
            } catch (ApiException ex) {
                blocked.add(op.resourceId());
                results.add(
                    Map.of(
                        "operationId",
                        op.operationId(),
                        "state",
                        ex.status == 412 ? "CONFLICT" : "REJECTED",
                        "code",
                        ex.code,
                        "detail",
                        ex.getMessage()
                    )
                );
            }
        }
        return results;
    }

    @GetMapping("/bootstrap")
    public Object bootstrap() {
        current.get();
        return Map.of(
            "workOrders",
            resources.list("work-orders", 0, 100, null, null),
            "assets",
            resources.list("assets", 0, 100, null, null),
            "userId",
            current.get().id,
            "downloadedAt",
            java.time.Instant.now()
        );
    }
}
