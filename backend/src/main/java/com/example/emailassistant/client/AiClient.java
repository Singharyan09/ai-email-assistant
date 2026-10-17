package com.example.emailassistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper around the Anthropic Messages API.
 */
@Component
public class AiClient {

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public AiClient(RestClient.Builder builder,
                    @Value("${ai.api-url}") String apiUrl,
                    @Value("${ai.api-key}") String apiKey,
                    @Value("${ai.model}") String model,
                    @Value("${ai.max-tokens}") int maxTokens) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public String complete(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage)));

        JsonNode response = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return response == null ? "" : response.path("content").path(0).path("text").asText("");
    }
}
