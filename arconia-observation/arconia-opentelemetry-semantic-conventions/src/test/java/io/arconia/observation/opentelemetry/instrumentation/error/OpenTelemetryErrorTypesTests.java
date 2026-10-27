package io.arconia.observation.opentelemetry.instrumentation.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryErrorTypes}.
 */
class OpenTelemetryErrorTypesTests {

    @Test
    void usesCanonicalNameOfTopLevelClass() {
        assertThat(OpenTelemetryErrorTypes.of(new IllegalStateException())).isEqualTo("java.lang.IllegalStateException");
    }

    @Test
    void usesCanonicalNameOfNestedClass() {
        assertThat(OpenTelemetryErrorTypes.of(new NestedException()))
                .isEqualTo("io.arconia.observation.opentelemetry.instrumentation.error.OpenTelemetryErrorTypesTests.NestedException");
    }

    @Test
    void usesClassNameOfAnonymousClass() {
        RuntimeException error = new RuntimeException() {};

        assertThat(OpenTelemetryErrorTypes.of(error)).isEqualTo(error.getClass().getName());
    }

    static class NestedException extends RuntimeException {
    }

}
