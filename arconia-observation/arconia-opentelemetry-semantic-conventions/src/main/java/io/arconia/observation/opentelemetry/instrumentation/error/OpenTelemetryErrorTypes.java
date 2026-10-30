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
     * Returns the fully qualified class name of the given error, as the conventions require for exception types.
     * It's the binary name the JVM uses, such as {@code com.example.OrderService$NotFoundException} for a nested
     * class, which is the form Spring Framework and the OpenTelemetry Java instrumentation report too, so that
     * the attribute can be aggregated across services.
     */
    public static String of(Throwable error) {
        return error.getClass().getName();
    }

}
