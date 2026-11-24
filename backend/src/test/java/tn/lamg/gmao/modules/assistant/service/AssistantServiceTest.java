package tn.lamg.gmao.modules.assistant.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.lamg.gmao.common.ApiException;
import tn.lamg.gmao.security.CurrentUser;

class AssistantServiceTest {

    final ObjectMapper mapper = new ObjectMapper();
    final CurrentUser current = mock(CurrentUser.class);
    final HttpClient client = mock(HttpClient.class);
    final AssistantDataService dataService = mock(AssistantDataService.class);
    final AssistantService service = new AssistantService(
        mapper,
        current,
        "test-key",
        client,
        dataService
    );

    @BeforeEach
    void emptyContext() {
        when(dataService.resolve(anyString())).thenReturn(AssistantDataService.Context.empty());
    }

    @SuppressWarnings("unchecked")
    void respond(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(
            client.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
            )
        ).thenReturn(response);
    }

    @Test
    void sendsGeminiHistoryAndSystemInstructions() throws Exception {
        respond(
            200,
            """
            {"candidates":[{"content":{"parts":[{"text":"Bonjour"}]}}]}
            """
        );
        service.answer(
            "Et ensuite ?",
            List.of(
                new AssistantService.ChatMessage("user", "Bonjour"),
                new AssistantService.ChatMessage("assistant", "Comment aider ?"),
                new AssistantService.ChatMessage("system", "Instruction non autorisée")
            )
        );
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(
            capture.capture(),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        var request = capture.getValue();
        assertTrue(request.uri().toString().endsWith("gemini-3.8-flash:generateContent"));
        assertEquals("test-key", request.headers().firstValue("x-goog-api-key").orElseThrow());
        assertFalse(request.uri().toString().contains("test-key"));
        assertTrue(request.headers().firstValue("Authorization").isEmpty());
        var body = mapper.readTree(readBody(request));
        assertTrue(
            body
                .path("systemInstruction")
                .path("parts")
                .get(0)
                .path("text")
                .asText()
                .contains("français")
        );
        assertEquals(3, body.path("contents").size());
        assertEquals("model", body.path("contents").get(1).path("role").asText());
        assertEquals(
            "Et ensuite ?",
            body.path("contents").get(2).path("parts").get(0).path("text").asText()
        );
        verify(current).get();
    }

    @Test
    void combinesAnswerPartsWithoutExposingThoughts() throws Exception {
        respond(
            200,
            """
            {"candidates":[{"content":{"parts":[
              {"thought":true,"text":"Analyse interne"},
              {"text":"Voici les étapes :\\n"},{"text":"1. Ouvrir le planning."}
            ]}}]}
            """
        );
        assertEquals(
            "Voici les étapes :\n1. Ouvrir le planning.",
            service.answer("Comment faire ?", null).answer()
        );
    }

    @Test
    void reportsAnInvalidKeyWithoutProviderDetails() throws Exception {
        respond(
            400,
            """
            {"error":{"details":[{"reason":"API_KEY_INVALID"}]}}
            """
        );
        var error = assertThrows(ApiException.class, () -> service.answer("Bonjour", null));
        assertEquals("AI_AUTHENTICATION_ERROR", error.code);
        assertFalse(error.getMessage().contains("test-key"));
    }

    @Test
    void reportsQuotaLimits() throws Exception {
        respond(429, "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\"}}");
        assertTrue(
            service
                .answer("Bonjour", null)
                .answer()
                .contains("La consultation de la GMAO reste disponible")
        );
    }

    @Test
    void reportsBlockedAndEmptyAnswers() throws Exception {
        respond(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        assertEquals(
            "AI_CONTENT_BLOCKED",
            assertThrows(ApiException.class, () -> service.answer("Question", null)).code
        );
        respond(200, "{\"candidates\":[]}");
        assertTrue(service.answer("Question", null).answer().contains("Gemini est indisponible"));
    }

    @Test
    void missingKeyDoesNotCallProvider() throws Exception {
        var unconfigured = new AssistantService(mapper, current, "", client, dataService);
        assertEquals(
            "AI_NOT_CONFIGURED",
            assertThrows(ApiException.class, () -> unconfigured.answer("Bonjour", null)).code
        );
        verifyNoInteractions(client);
    }

    @Test
    void distinguishesSlowGenerationFromProviderErrors() throws Exception {
        when(
            client.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
            )
        ).thenThrow(new HttpTimeoutException("timed out"));
        assertTrue(
            service
                .answer("Bonjour", null)
                .answer()
                .contains("La consultation de la GMAO reste disponible")
        );
    }

    @Test
    void directDataAnswerWorksWithoutGemini() throws Exception {
        var reference = new AssistantDataService.Reference("Filtre", "inventory/items", "item-id");
        when(dataService.resolve(anyString())).thenReturn(
            new AssistantDataService.Context(
                "inventory/items",
                1,
                List.of(Map.of("name", "Filtre")),
                "Stock actuel : 2",
                List.of(reference),
                true
            )
        );
        var response = new AssistantService(mapper, current, "", client, dataService).answer(
            "Stock minimum ?",
            null
        );
        assertEquals("Stock actuel : 2", response.answer());
        assertEquals(List.of(reference), response.references());
        verifyNoInteractions(client);
    }

    @Test
    void unavailableGeminiReturnsAuthorizedDataWithReferences() throws Exception {
        respond(503, "{\"error\":{\"status\":\"UNAVAILABLE\"}}");
        var reference = new AssistantDataService.Reference("Pompe", "assets", "asset-id");
        when(dataService.resolve(anyString())).thenReturn(
            new AssistantDataService.Context(
                "assets",
                1,
                List.of(Map.of("name", "Pompe")),
                "1. Pompe",
                List.of(reference),
                false
            )
        );
        var response = service.answer("Explique mes équipements", null);
        assertTrue(response.answer().contains("1. Pompe"));
        assertEquals(List.of(reference), response.references());
    }

    private String readBody(HttpRequest request) throws Exception {
        var subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        request.bodyPublisher().orElseThrow().subscribe(
            new Flow.Subscriber<ByteBuffer>() {
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                public void onNext(ByteBuffer buffer) {
                    subscriber.onNext(List.of(buffer));
                }

                public void onError(Throwable error) {
                    subscriber.onError(error);
                }

                public void onComplete() {
                    subscriber.onComplete();
                }
            }
        );
        return subscriber.getBody().toCompletableFuture().get();
    }
}
