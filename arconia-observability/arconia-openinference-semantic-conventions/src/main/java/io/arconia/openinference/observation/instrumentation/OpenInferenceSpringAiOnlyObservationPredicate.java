package io.arconia.openinference.observation.instrumentation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;

import org.springframework.util.StringUtils;

/**
 * Removes all observations that are not part of the Spring AI workflows.
 */
public class OpenInferenceSpringAiOnlyObservationPredicate implements ObservationPredicate {

    @Override
    public boolean test(String observationName, Observation.Context context) {
        return StringUtils.hasText(observationName) && observationName.startsWith("spring.ai");
    }

}
