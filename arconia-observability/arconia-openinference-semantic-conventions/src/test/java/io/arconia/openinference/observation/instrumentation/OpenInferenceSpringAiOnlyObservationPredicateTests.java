package io.arconia.openinference.observation.instrumentation;

import io.micrometer.observation.Observation;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceSpringAiOnlyObservationPredicate}.
 */
class OpenInferenceSpringAiOnlyObservationPredicateTests {

    OpenInferenceSpringAiOnlyObservationPredicate predicate = new OpenInferenceSpringAiOnlyObservationPredicate();

    @Test
    void whenSpringAiObservationThenKeep() {
        assertThat(predicate.test("spring.ai.client", Mockito.mock(Observation.Context.class))).isTrue();
    }

    @Test
    void whenNonSpringAiObservationThenRemove() {
        assertThat(predicate.test("http.requests.count", Mockito.mock(Observation.Context.class))).isFalse();
    }

}
