package com.example.emailassistant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiClientTest {

    private static final String KEY = "test-secret-key-123";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private AiClient client(String baseUrl) {
        return new AiClient(builder, new ObjectMapper(), baseUrl, KEY, "test-model", 256);
    }

    @Test
    void sendsChatCompletionRequestAndReadsReply() {
        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + KEY))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.max_tokens").value(256))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("be brief"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("hello"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hi there\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client("https://api.example.com/v1").complete("be brief", "hello")).isEqualTo("hi there");
        server.verify();
    }

    @Test
    void handlesTrailingSlashInBaseUrl() {
        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client("https://api.example.com/v1/").complete("s", "u")).isEqualTo("ok");
        server.verify();
    }

    @Test
    void unauthorizedGivesFixedMessageWithoutProviderText() {
        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Incorrect API key provided: " + KEY + "\"}}"));

        assertThatThrownBy(() -> client("https://api.example.com/v1").complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(KEY);
    }

    @Test
    void providerErrorMessageIsRedacted() {
        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"bad request for key " + KEY + "\"}}"));

        assertThatThrownBy(() -> client("https://api.example.com/v1").complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("[redacted]")
                .hasMessageNotContaining(KEY);
    }

    @Test
    void readsErrorWrappedInArray() {
        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"error\":{\"message\":\"model not found\"}}]"));

        assertThatThrownBy(() -> client("https://api.example.com/v1").complete("s", "u"))
                .hasMessage("AI provider returned HTTP 404: model not found");
    }

    @Test
    void missingConfigurationGivesClearMessage() {
        AiClient unconfigured = new AiClient(builder, new ObjectMapper(), "", "", "", 256);

        assertThatThrownBy(() -> unconfigured.complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI_BASE_URL");
    }
}
