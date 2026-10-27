package io.arconia.observation.opentelemetry.instrumentation.error;

/**
 * Computes the value of the {@code error.type} attribute for errors, following the OpenTelemetry Semantic Conventions.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/error/">OpenTelemetry Semantic Conventions for Errors</a>
 */
public final class OpenTelemetryErrorTypes {

    private OpenTelemetryErrorTypes() {
    }

    /**
     * Returns the canonical class name of the given error, as the conventions require for exception types,
     * such as {@code com.example.OrderService.NotFoundException} for a nested class. Anonymous and local classes
     * don't have a canonical name, so their class name is returned instead.
     */
    public static String of(Throwable error) {
        String canonicalName = error.getClass().getCanonicalName();
        return canonicalName != null ? canonicalName : error.getClass().getName();
    }

}
