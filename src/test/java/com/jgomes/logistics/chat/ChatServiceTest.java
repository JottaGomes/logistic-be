package com.jgomes.logistics.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jgomes.logistics.dto.ChatMessageDTO;
import com.jgomes.logistics.dto.ChatResponseDTO;
import com.jgomes.logistics.exception.ChatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GroqClient client = mock(GroqClient.class);
    private final ShipmentChatTools tools = mock(ShipmentChatTools.class);
    private final ChatProperties properties = new ChatProperties();

    private ChatService service;

    /** Snapshot of every request, since the service keeps appending to the same array. */
    private final List<ArrayNode> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(tools.definitions()).thenReturn(mapper.createArrayNode());
        Clock clock = Clock.fixed(Instant.parse("2026-10-11T09:00:00Z"), ZoneOffset.UTC);
        service = new ChatService(client, tools, properties, mapper, clock);
    }

    private JsonNode json(String text) throws Exception {
        return mapper.readTree(text);
    }

    private void answerWith(JsonNode... answers) {
        var stubbing = when(client.complete(any(), any()));
        for (JsonNode answer : answers) {
            stubbing = stubbing.thenAnswer(call -> {
                sent.add(((ArrayNode) call.getArgument(0)).deepCopy());
                return answer;
            });
        }
    }

    @Test
    void reply_runsTheRequestedToolAndReturnsTheAnswerBuiltOnIt() throws Exception {
        answerWith(
                json("""
                        {"role": "assistant", "content": null, "reasoning": "need the shipment",
                         "tool_calls": [{"id": "call_1", "type": "function",
                           "function": {"name": "get_shipment", "arguments": "{\\"reference\\": \\"SHP-2026-0001\\"}"}}]}
                        """),
                json("""
                        {"role": "assistant", "content": "SHP-2026-0001 made a profit of €2,100.00."}
                        """));
        when(tools.execute("get_shipment", "{\"reference\": \"SHP-2026-0001\"}")).thenReturn("{\"totalIncome\": 5300}");

        ChatResponseDTO response = service.reply(List.of(new ChatMessageDTO("user", "How did SHP-2026-0001 do?")));

        assertThat(response.reply()).isEqualTo("SHP-2026-0001 made a profit of €2,100.00.");
        assertThat(response.toolsUsed()).containsExactly("get_shipment");

        // the second request carries the tool call and its result, in that order
        ArrayNode second = sent.get(1);
        JsonNode assistantTurn = second.get(2);
        JsonNode toolTurn = second.get(3);
        assertThat(assistantTurn.path("tool_calls").get(0).path("id").asText()).isEqualTo("call_1");
        assertThat(assistantTurn.has("reasoning")).as("provider-specific fields are not sent back").isFalse();
        assertThat(toolTurn.path("role").asText()).isEqualTo("tool");
        assertThat(toolTurn.path("tool_call_id").asText()).isEqualTo("call_1");
        assertThat(toolTurn.path("content").asText()).isEqualTo("{\"totalIncome\": 5300}");
    }

    @Test
    void reply_startsWithTheSystemPromptAndKeepsOnlyTheRecentHistory() throws Exception {
        properties.setMaxHistory(2);
        answerWith(json("{\"role\": \"assistant\", \"content\": \"Hello\"}"));

        service.reply(List.of(
                new ChatMessageDTO("user", "first"),
                new ChatMessageDTO("assistant", "second"),
                new ChatMessageDTO("user", "third")));

        ArrayNode messages = sent.get(0);
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(0).path("content").asText()).contains("Today is 2026-10-11");
        assertThat(messages.get(1).path("content").asText()).isEqualTo("second");
        assertThat(messages.get(2).path("content").asText()).isEqualTo("third");
    }

    /** A model that keeps asking for tools must not loop on the API indefinitely. */
    @Test
    void reply_givesUpAfterTheConfiguredNumberOfRounds() throws Exception {
        properties.setMaxToolRounds(2);
        JsonNode moreTools = json("""
                {"role": "assistant", "content": null,
                 "tool_calls": [{"id": "c", "type": "function", "function": {"name": "get_profit_summary", "arguments": "{}"}}]}
                """);
        when(client.complete(any(), any())).thenReturn(moreTools);
        when(tools.execute(any(), any())).thenReturn("{}");

        assertThatThrownBy(() -> service.reply(List.of(new ChatMessageDTO("user", "loop"))))
                .isInstanceOf(ChatUnavailableException.class);

        ArgumentCaptor<ArrayNode> captor = ArgumentCaptor.forClass(ArrayNode.class);
        verify(client, times(2)).complete(captor.capture(), any());
    }
}
