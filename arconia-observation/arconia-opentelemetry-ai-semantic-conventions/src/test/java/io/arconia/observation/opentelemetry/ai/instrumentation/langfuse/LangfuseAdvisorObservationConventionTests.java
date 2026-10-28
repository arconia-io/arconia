package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import io.micrometer.common.KeyValue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangfuseAdvisorObservationConvention}.
 */
class LangfuseAdvisorObservationConventionTests {

    private final LangfuseAdvisorObservationConvention convention = new LangfuseAdvisorObservationConvention();

    @Test
    void shouldHaveChainObservationTypeOnSpansOnly() {
        AdvisorObservationContext context = AdvisorObservationContext.builder()
                .advisorName("MessageChatMemoryAdvisor")
                .chatClientRequest(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();

        assertThat(convention.getHighCardinalityKeyValues(context))
                .contains(KeyValue.of(LangfuseAttributes.LANGFUSE_OBSERVATION_TYPE, "chain"));
        assertThat(convention.getLowCardinalityKeyValues(context)).noneSatisfy(keyValue ->
                assertThat(keyValue.getKey()).isEqualTo(LangfuseAttributes.LANGFUSE_OBSERVATION_TYPE));
    }

}
