package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryAdvisorObservationConvention}.
 */
class OpenLLMetryAdvisorObservationConventionTests {

    private final OpenLLMetryAdvisorObservationConvention convention
            = new OpenLLMetryAdvisorObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLLMetryAdvisorObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "call")
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
