package io.arconia.observation.opentelemetry.instrumentation.error;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.opentelemetry.semconv.ErrorAttributes;

/**
 * {@link ObservationFilter} recording the {@code error.type} attribute on observations ending in an error,
 * as a low cardinality key value so that it applies to both the span and the metrics of the observation.
 * <p>
 * The attribute holds the fully qualified class name of the error, unless the observation convention already
 * set it. It's not recorded when the observation succeeds, so the {@code none} value some conventions use
 * for successful operations is removed.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/error/">OpenTelemetry Semantic Conventions for Errors</a>
 */
public final class OpenTelemetryErrorTypeObservationFilter implements ObservationFilter {

    private static final String ERROR_TYPE = ErrorAttributes.ERROR_TYPE.getKey();

    @Override
    public Observation.Context map(Observation.Context context) {
        KeyValue errorType = context.getLowCardinalityKeyValue(ERROR_TYPE);
        Throwable error = context.getError();

        if (error == null) {
            if (errorType != null && KeyValue.NONE_VALUE.equals(errorType.getValue())) {
                context.removeLowCardinalityKeyValue(ERROR_TYPE);
            }
            return context;
        }

        if (errorType == null || KeyValue.NONE_VALUE.equals(errorType.getValue())) {
            context.addLowCardinalityKeyValue(KeyValue.of(ERROR_TYPE, error.getClass().getName()));
        }
        return context;
    }

}
