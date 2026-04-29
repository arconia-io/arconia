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

    private final OpenLLMetryAdvisorObservationConvention observationConvention
            = new OpenLLMetryAdvisorObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
                .isEqualTo(OpenLLMetryAdvisorObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameAdvisorNameHasNoSuffix() {
        var context = AdvisorObservationContext.builder()
                .advisorName("call")
                .chatClientRequest(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
        assertThat(observationConvention.getContextualName(context)).isEqualTo("call");
    }

    @Test
    void contextualNameAdvisorNameHasSuffix() {
        var context = AdvisorObservationContext.builder()
                .advisorName("call_advisor")
                .chatClientRequest(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
        assertThat(observationConvention.getContextualName(context)).isEqualTo("call");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = AdvisorObservationContext.builder()
                .advisorName("call")
                .chatClientRequest(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task")
        );
    }

}
