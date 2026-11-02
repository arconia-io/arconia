package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOtlpProtocolCondition}.
 */
class OnOtlpProtocolConditionTests {

    private static final String SIGNAL_PREFIX = "arconia.otel.test.exporter";

    private final OnOtlpProtocolCondition condition = new OnOtlpProtocolCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @Test
    void httpProtobufByDefault() {
        assertThat(outcome(HttpProtobuf.class).isMatch()).isTrue();
        assertThat(outcome(Grpc.class).isMatch()).isFalse();
    }

    @Test
    void commonProtocol() {
        environment.setProperty("arconia.otel.exporter.otlp.protocol", "grpc");

        ConditionOutcome outcome = outcome(Grpc.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.exporter.otlp.protocol is GRPC");
        assertThat(outcome(HttpProtobuf.class).isMatch()).isFalse();
    }

    @Test
    void signalProtocolTakesPrecedenceOverCommonProtocol() {
        environment.setProperty("arconia.otel.exporter.otlp.protocol", "grpc");
        environment.setProperty(SIGNAL_PREFIX + ".otlp.protocol", "http_protobuf");

        ConditionOutcome outcome = outcome(HttpProtobuf.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains(SIGNAL_PREFIX + ".otlp.protocol is HTTP_PROTOBUF");
        assertThat(outcome(Grpc.class).isMatch()).isFalse();
    }

    @Test
    void protocolBoundLikeConfigurationProperties() {
        environment.setProperty(SIGNAL_PREFIX + ".otlp.protocol", "http-protobuf");

        assertThat(outcome(HttpProtobuf.class).isMatch()).isTrue();
        assertThat(outcome(Grpc.class).isMatch()).isFalse();
    }

    @Test
    void emptySignalProtocolFallsBackToCommonProtocol() {
        environment.setProperty("arconia.otel.exporter.otlp.protocol", "grpc");
        environment.setProperty(SIGNAL_PREFIX + ".otlp.protocol", "");

        assertThat(outcome(Grpc.class).isMatch()).isTrue();
    }

    @Test
    void invalidProtocolReportsProperty() {
        environment.setProperty(SIGNAL_PREFIX + ".otlp.protocol", "http/json");

        assertThatExceptionOfType(BindException.class)
                .isThrownBy(() -> outcome(HttpProtobuf.class))
                .withMessageContaining(SIGNAL_PREFIX + ".otlp.protocol");
    }

    private ConditionOutcome outcome(Class<?> annotatedClass) {
        when(context.getEnvironment()).thenReturn(environment);
        return condition.getMatchOutcome(context, AnnotationMetadata.introspect(annotatedClass));
    }

    @ConditionalOnOtlpProtocol(prefix = SIGNAL_PREFIX, value = Protocol.HTTP_PROTOBUF)
    static class HttpProtobuf {
    }

    @ConditionalOnOtlpProtocol(prefix = SIGNAL_PREFIX, value = Protocol.GRPC)
    static class Grpc {
    }

}
