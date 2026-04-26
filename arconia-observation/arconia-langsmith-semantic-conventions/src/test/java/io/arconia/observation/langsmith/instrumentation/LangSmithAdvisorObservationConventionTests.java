package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.observation.conventions.AiOperationType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithAdvisorObservationConvention}.
 */
class LangSmithAdvisorObservationConventionTests {

    private final LangSmithAdvisorObservationConvention convention =
            new LangSmithAdvisorObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(LangSmithAdvisorObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenAdvisorNameHasNoSuffix() {
        var context = createContext("call");
        assertThat(convention.getContextualName(context)).isEqualTo("call");
    }

    @Test
    void contextualNameWhenAdvisorNameHasSuffix() {
        var context = createContext("call_advisor");
        assertThat(convention.getContextualName(context)).isEqualTo("call");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = createContext("call");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), AiOperationType.FRAMEWORK.value()),
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "chain")
        );
    }

    @Test
    void shouldHaveEmptyHighCardinalityKeyValues() {
        var context = createContext("call");

        assertThat(convention.getHighCardinalityKeyValues(context)).isEmpty();
    }

    private AdvisorObservationContext createContext(String advisorName) {
        return AdvisorObservationContext.builder()
                .advisorName(advisorName)
                .chatClientRequest(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
    }

}
