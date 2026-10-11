package com.jgomes.logistics.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jgomes.logistics.exception.ChatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Exercises the HTTP exchange against a mock server; the real provider is never called. */
class GroqClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ChatProperties properties = new ChatProperties();

    private MockRestServiceServer server;
    private GroqClient client;

    @BeforeEach
    void setUp() {
        properties.setApiKey("test-key");
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        client = new GroqClient(properties, mapper, new RestTemplateBuilder(customizer));
        server = customizer.getServer();
    }

    private ArrayNode oneQuestion() {
        ArrayNode messages = mapper.createArrayNode();
        messages.addObject().put("role", "user").put("content", "hi");
        return messages;
    }

    @Test
    void complete_sendsTheKeyModelAndToolsAndReturnsTheAssistantMessage() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.tool_choice").value("auto"))
                .andExpect(jsonPath("$.messages[0].content").value("hi"))
                .andRespond(withSuccess("""
                        {"choices": [{"message": {"role": "assistant", "content": "Hello"}}]}
                        """, MediaType.APPLICATION_JSON));

        JsonNode message = client.complete(oneQuestion(), mapper.createArrayNode());

        assertThat(message.path("content").asText()).isEqualTo("Hello");
        server.verify();
    }

    @Test
    void complete_withARejectedKey_explainsWhatToCheck() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": {\"message\": \"Invalid API Key\"}}"));

        assertThatThrownBy(() -> client.complete(oneQuestion(), mapper.createArrayNode()))
                .isInstanceOf(ChatUnavailableException.class)
                .hasMessageContaining("GROQ_API_KEY");
    }

    @Test
    void complete_withoutAKey_failsBeforeAnyRequest() {
        properties.setApiKey("");

        assertThatThrownBy(() -> client.complete(oneQuestion(), mapper.createArrayNode()))
                .isInstanceOf(ChatUnavailableException.class)
                .hasMessageContaining("not configured");
        server.verify();
    }
}
