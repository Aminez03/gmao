package tn.lamg.gmao.modules.assistant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.modules.assistant.service.AssistantService;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public AssistantService.Response chat(@Valid @RequestBody Request request) {
        return assistant.answer(request.message(), request.history());
    }

    public record Request(
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 12) List<AssistantService.ChatMessage> history
    ) {}
}
