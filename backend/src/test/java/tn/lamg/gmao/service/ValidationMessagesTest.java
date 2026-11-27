package tn.lamg.gmao.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.lamg.gmao.exception.ApiErrorHandler;
import tn.lamg.gmao.modules.maintenance.model.Checklist;
import tn.lamg.gmao.modules.shared.service.ResourceRules;

class ValidationMessagesTest {

    @Test
    void checklistValidationReturnsReadableFrenchThroughTheApi() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new ResourceService(
                null,
                null,
                null,
                null,
                new ObjectMapper(),
                factory.getValidator(),
                null,
                null,
                null,
                new StaticListableBeanFactory().getBeanProvider(ResourceRules.class)
            );
            var mvc = MockMvcBuilders.standaloneSetup(new ValidationController(service))
                .setControllerAdvice(new ApiErrorHandler())
                .build();
            mvc.perform(post("/validate-checklist"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHECKLIST_STEPS"))
                .andExpect(jsonPath("$.detail").value("Définissez entre 1 et 100 étapes."));
            mvc.perform(post("/invalid-input"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Vérifiez les champs et leurs formats."));
        }
    }

    @RestController
    static class ValidationController {

        private final ResourceService service;

        ValidationController(ResourceService service) {
            this.service = service;
        }

        @PostMapping("/validate-checklist")
        public void validateChecklist() {
            var checklist = new Checklist();
            checklist.name = "Contrôle préventif";
            checklist.revision = 1;
            checklist.steps = new ObjectMapper().createArrayNode();
            service.check(checklist);
        }

        @PostMapping("/invalid-input")
        public void invalidInput() {
            throw new IllegalArgumentException();
        }
    }
}
