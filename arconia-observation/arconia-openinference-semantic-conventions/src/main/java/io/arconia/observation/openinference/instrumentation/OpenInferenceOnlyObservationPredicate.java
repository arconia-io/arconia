package io.arconia.observation.openinference.instrumentation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;

import org.springframework.util.StringUtils;

/**
 * Removes any observation not being of the OpenInference kind.
 */
public class OpenInferenceOnlyObservationPredicate implements ObservationPredicate {

    @Override
    public boolean test(String observationName, Observation.Context context) {
        return StringUtils.hasText(observationName) && observationName.startsWith("spring.ai");
    }

}
