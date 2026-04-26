package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithChatClientObservationConvention}.
 */
class LangSmithChatClientObservationConventionTests {

    private final LangSmithChatClientObservationConvention convention =
            new LangSmithChatClientObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(LangSmithChatClientObservationConvention.DEFAULT_NAME);
    }

    @Test
    void shouldHaveSessionIdWhenConversationIdProvided() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "session-007")
                        .build())
                .build();

        assertThat(convention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(LangSmithAttributes.LANGSMITH_TRACE_SESSION_ID.getKey(), "session-007")
        );
    }

    @Test
    void shouldNotHaveSessionIdWhenNoConversationId() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();

        assertThat(convention.getHighCardinalityKeyValues(context))
                .noneSatisfy(kv -> assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.LANGSMITH_TRACE_SESSION_ID.getKey()));
    }

    @Test
    void shouldNotHaveSessionIdWhenConversationIdIsEmpty() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "")
                        .build())
                .build();

        assertThat(convention.getHighCardinalityKeyValues(context))
                .noneSatisfy(kv -> assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.LANGSMITH_TRACE_SESSION_ID.getKey()));
    }

}
