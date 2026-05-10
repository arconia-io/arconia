package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithAiAdvisorConvention}.
 */
class LangSmithAdvisorObservationConventionTests {

    private final LangSmithAiAdvisorConvention convention = new LangSmithAiAdvisorConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(LangSmithAiAdvisorConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameAdvisorNameHasNoSuffix() {
        var context = createContext("call");
        assertThat(convention.getContextualName(context)).isEqualTo("call");
    }

    @Test
    void contextualNameAdvisorNameHasSuffix() {
        var context = createContext("call_advisor");
        assertThat(convention.getContextualName(context)).isEqualTo("call");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = createContext("call");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "framework"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "spring_ai"),
                KeyValue.of("spring.ai.kind", "advisor"),
                KeyValue.of("spring.ai.advisor.name", "call"),
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "chain")
        );
    }

    // Helpers

    private AdvisorObservationContext createContext(String advisorName) {
        return AdvisorObservationContext.builder()
                .advisorName(advisorName)
                .chatClientRequest(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
    }

}
