package io.arconia.observation.opentelemetry.instrumentation.error;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.opentelemetry.semconv.ErrorAttributes;

/**
 * {@link ObservationFilter} recording the {@code error.type} attribute on observations ending in an error,
 * as a low cardinality key value so that it applies to both the span and the metrics of the observation.
 * <p>
 * The attribute holds the class name of the error, unless the observation convention already set it.
 * The {@code none} value the Spring Framework conventions use when there is no error type to report
 * is always removed, since the conventions don't record the attribute for successful operations: a convention
 * reporting {@code none} for an observation carrying an error, such as an HTTP request that answered a handled
 * exception with a status code, has decided that the error isn't one to report.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/error/">OpenTelemetry Semantic Conventions for Errors</a>
 */
public final class OpenTelemetryErrorTypeObservationFilter implements ObservationFilter {

    private static final String ERROR_TYPE = ErrorAttributes.ERROR_TYPE.getKey();

    @Override
    public Observation.Context map(Observation.Context context) {
        KeyValue errorType = context.getLowCardinalityKeyValue(ERROR_TYPE);
        if (errorType != null) {
            if (KeyValue.NONE_VALUE.equals(errorType.getValue())) {
                context.removeLowCardinalityKeyValue(ERROR_TYPE);
            }
            return context;
        }

        Throwable error = context.getError();
        if (error != null) {
            context.addLowCardinalityKeyValue(KeyValue.of(ERROR_TYPE, OpenTelemetryErrorTypes.of(error)));
        }
        return context;
    }

}
