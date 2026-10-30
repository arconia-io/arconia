package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GenAiConversationIdObservationHandler}.
 */
class GenAiConversationIdObservationHandlerTests {

    private final GenAiConversationIdObservationHandler handler =
            new GenAiConversationIdObservationHandler(List.of("gen_ai.conversation.id", "vendor.session_id"));

    // A registry without handlers hands out no-op observations, which don't expose their context.
    private final ObservationRegistry registry = ObservationRegistry.create();

    {
        registry.observationConfig().observationHandler(context -> true);
    }

    @Test
    void shouldRequireAttributeKeys() {
        assertThatThrownBy(() -> new GenAiConversationIdObservationHandler(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attributeKeys cannot be empty");
    }

    @Test
    void shouldSupportObservationsNestedInChatClientCalls() {
        assertThat(handler.supportsContext(chatModelContext())).isTrue();
        assertThat(handler.supportsContext(toolCallingContext())).isTrue();
        assertThat(handler.supportsContext(advisorContext())).isTrue();
        assertThat(handler.supportsContext(chatClientContext("session-42"))).isFalse();
        assertThat(handler.supportsContext(new Observation.Context())).isFalse();
    }

    @Test
    void shouldRecordConversationIdUnderEveryKey() {
        ChatModelObservationContext context = chatModelContext();
        context.setParentObservation(observation(chatClientContext("session-42")));

        handler.onStart(context);

        assertThat(context.getHighCardinalityKeyValues()).containsExactlyInAnyOrder(
                KeyValue.of("gen_ai.conversation.id", "session-42"),
                KeyValue.of("vendor.session_id", "session-42"));
    }

    @Test
    void shouldPropagateConversationIdThroughIntermediateObservations() {
        AdvisorObservationContext advisorContext = advisorContext();
        advisorContext.setParentObservation(observation(chatClientContext("session-42")));
        ToolCallingObservationContext context = toolCallingContext();
        context.setParentObservation(observation(advisorContext));

        handler.onStart(context);

        assertThat(context.getHighCardinalityKeyValues()).contains(KeyValue.of("gen_ai.conversation.id", "session-42"));
    }

    @Test
    void shouldNotAddConversationIdWhenChatClientCallHasNoConversation() {
        ChatModelObservationContext context = chatModelContext();
        context.setParentObservation(observation(chatClientContext(null)));

        handler.onStart(context);

        assertThat(context.getHighCardinalityKeyValues()).isEmpty();
    }

    @Test
    void shouldNotAddConversationIdWithoutChatClientCall() {
        ChatModelObservationContext context = chatModelContext();

        handler.onStart(context);

        assertThat(context.getHighCardinalityKeyValues()).isEmpty();
    }

    // Helpers

    private Observation observation(Observation.Context context) {
        return Observation.createNotStarted("parent", () -> context, registry);
    }

    private static ChatClientObservationContext chatClientContext(String conversationId) {
        ChatClientRequest.Builder request = ChatClientRequest.builder()
                .prompt(Prompt.builder().content("Hello").build());
        if (conversationId != null) {
            request.context(ChatMemory.CONVERSATION_ID, conversationId);
        }
        return ChatClientObservationContext.builder().request(request.build()).build();
    }

    private static ChatModelObservationContext chatModelContext() {
        return ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hello"))))
                .provider(AiProvider.SPRING_AI.value())
                .build();
    }

    private static ToolCallingObservationContext toolCallingContext() {
        return ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("weather").description("Weather").inputSchema("{}").build())
                .toolCallArguments("{}")
                .build();
    }

    private static AdvisorObservationContext advisorContext() {
        return AdvisorObservationContext.builder()
                .advisorName("memory")
                .chatClientRequest(ChatClientRequest.builder().prompt(Prompt.builder().content("Hello").build()).build())
                .order(0)
                .build();
    }

}
