package com.jgomes.logistics.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jgomes.logistics.exception.ChatUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin client for Groq's OpenAI-compatible chat completions endpoint.
 *
 * Kept to one method on purpose: everything about the conversation (prompt,
 * tools, the tool-calling loop) belongs to ChatService, so swapping the provider
 * for another OpenAI-compatible one means changing app.chat.base-url and model.
 */
@Slf4j
@Component
@EnableConfigurationProperties(ChatProperties.class)
public class GroqClient {

    private final ChatProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public GroqClient(ChatProperties properties, ObjectMapper mapper, RestTemplateBuilder builder) {
        this.properties = properties;
        this.mapper = mapper;
        this.restTemplate = builder
                .setConnectTimeout(properties.getTimeout())
                .setReadTimeout(properties.getTimeout())
                .build();
    }

    /**
     * Sends the conversation and the tool definitions, and returns the assistant
     * message of the first choice: either text content, or {@code tool_calls} for
     * the caller to execute and send back.
     *
     * @throws ChatUnavailableException when no key is configured or the provider fails
     */
    public JsonNode complete(ArrayNode messages, ArrayNode tools) {

        if (!properties.isConfigured()) {
            throw new ChatUnavailableException("The chat assistant is not configured. Set GROQ_API_KEY, or app.chat.api-key in application-local.yml, on the backend.");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("model", properties.getModel());
        body.set("messages", messages);
        body.set("tools", tools);
        body.put("tool_choice", "auto");
        // low temperature: answers are read off tool results, not invented
        body.put("temperature", 0.2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        try {
            JsonNode response = restTemplate.postForObject(
                    properties.getBaseUrl() + "/chat/completions", new HttpEntity<>(body, headers), JsonNode.class);

            JsonNode message = response == null ? null : response.path("choices").path(0).path("message");

            if (message == null || message.isMissingNode()) {
                throw new ChatUnavailableException("The language model returned an empty response. Please try again.");
            }

            return message;

        } catch (HttpStatusCodeException ex) {
            // the provider's body can be long; the first part is enough to diagnose
            String detail = ex.getResponseBodyAsString();
            log.warn("chat_provider_error status={} body={}",
                    ex.getStatusCode().value(), detail.substring(0, Math.min(detail.length(), 500)));
            throw new ChatUnavailableException(describe(ex));

        } catch (ResourceAccessException ex) {
            log.warn("chat_provider_unreachable message={}", ex.getMessage());
            throw new ChatUnavailableException("Could not reach the language model provider. Please try again.");
        }
    }

    private String describe(HttpStatusCodeException ex) {

        if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
            return "The language model provider rejected the API key. Check GROQ_API_KEY or app.chat.api-key in application-local.yml.";
        }

        if (ex.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return "The language model provider is rate limiting requests. Wait a moment and try again.";
        }

        return "The language model provider could not answer. Please try again.";
    }
}
