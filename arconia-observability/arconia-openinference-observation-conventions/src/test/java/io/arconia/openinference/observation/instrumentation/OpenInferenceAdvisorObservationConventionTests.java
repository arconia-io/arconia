package io.arconia.openinference.observation.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceAdvisorObservationConvention}.
 */
class OpenInferenceAdvisorObservationConventionTests {

    private final OpenInferenceAdvisorObservationConvention observationConvention
            = new OpenInferenceAdvisorObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
                .isEqualTo(OpenInferenceAdvisorObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "CHAIN")
        );
    }

}
