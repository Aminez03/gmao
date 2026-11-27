package tn.lamg.gmao.modules.patrimoine.controller;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.CommandService;
import tn.lamg.gmao.modules.patrimoine.service.AssetReadingService;

@RestController
@RequestMapping("/api/v1/assets/{assetId}/readings")
public class AssetReadingController {

    private final AssetReadingService readings;
    private final CommandService commands;

    public AssetReadingController(AssetReadingService readings, CommandService commands) {
        this.readings = readings;
        this.commands = commands;
    }

    @GetMapping
    public Object list(@PathVariable UUID assetId) {
        return readings.list(assetId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(
        @PathVariable UUID assetId,
        @RequestHeader("Idempotency-Key") UUID key,
        @RequestBody Map<String, Object> body
    ) {
        return commands.run(key, "asset-readings/" + assetId + "/create", body, () ->
            readings.create(assetId, body)
        );
    }
}
