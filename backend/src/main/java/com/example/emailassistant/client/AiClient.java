package com.example.emailassistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin wrapper around any OpenAI-compatible chat completions API.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String chatUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AiClient(RestClient.Builder builder,
                    ObjectMapper objectMapper,
                    @Value("${ai.base-url}") String baseUrl,
                    @Value("${ai.api-key}") String apiKey,
                    @Value("${ai.model}") String model,
                    @Value("${ai.max-tokens}") int maxTokens) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.chatUrl = baseUrl.strip().replaceAll("/+$", "") + "/chat/completions";
        this.apiKey = apiKey.strip();
        this.model = model.strip();
        this.maxTokens = maxTokens;
    }

    public String complete(String systemPrompt, String userMessage) {
        if (chatUrl.equals("/chat/completions") || apiKey.isEmpty() || model.isEmpty()) {
            throw new AiProviderException("AI provider is not configured. Set AI_BASE_URL, AI_API_KEY and AI_MODEL.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)));

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(chatUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String message = (status == 401 || status == 403)
                    ? "AI provider rejected the API key (HTTP " + status + "). Check AI_API_KEY."
                    : "AI provider returned HTTP " + status + ": " + providerMessage(e.getResponseBodyAsString());
            log.warn(message);
            throw new AiProviderException(message);
        } catch (ResourceAccessException e) {
            String message = "Could not reach the AI provider. Check AI_BASE_URL and your network connection.";
            log.warn(message);
            throw new AiProviderException(message);
        } catch (RestClientException e) {
            String message = "Unexpected response from the AI provider.";
            log.warn(message);
            throw new AiProviderException(message);
        }

        JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
        if (content == null || !content.isTextual()) {
            throw new AiProviderException("AI provider returned no reply text.");
        }
        return content.asText();
    }

    /** Pulls error.message out of an OpenAI-style error body, with the API key scrubbed. */
    private String providerMessage(String responseBody) {
        String message;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // Some providers (e.g. Gemini) wrap the error object in an array.
            JsonNode error = (root.isArray() ? root.path(0) : root).path("error");
            message = error.isTextual() ? error.asText() : error.path("message").asText("");
        } catch (Exception e) {
            message = "";
        }
        if (message.isBlank()) {
            message = "no error details";
        }
        if (message.length() > 300) {
            message = message.substring(0, 300) + "...";
        }
        return apiKey.isEmpty() ? message : message.replace(apiKey, "[redacted]");
    }
}
