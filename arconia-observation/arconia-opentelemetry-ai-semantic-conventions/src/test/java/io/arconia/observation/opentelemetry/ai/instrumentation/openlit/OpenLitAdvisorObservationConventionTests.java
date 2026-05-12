package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLitAdvisorObservationConvention}.
 */
class OpenLitAdvisorObservationConventionTests {

    private final OpenLitAdvisorObservationConvention convention = new OpenLitAdvisorObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLitAdvisorObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of("spring.ai.kind", "advisor"),
                KeyValue.of("spring.ai.advisor.name", "call")
        );
    }

    @Test
    void shouldNotHaveTraceloopKeyValues() {
        var context = createContext("call");

        assertThat(convention.getLowCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("traceloop."));
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
