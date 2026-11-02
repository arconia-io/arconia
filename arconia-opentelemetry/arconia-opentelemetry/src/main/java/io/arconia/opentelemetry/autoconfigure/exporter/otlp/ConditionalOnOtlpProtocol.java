package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import io.arconia.core.annotations.Internal;

/**
 * Whether the OTLP exporter of a signal uses the specified protocol. The protocol configured
 * for the signal takes precedence over the common one, which defaults to HTTP/Protobuf.
 */
@Internal
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnOtlpProtocolCondition.class)
public @interface ConditionalOnOtlpProtocol {

    /**
     * The prefix of the exporter properties of the signal, such as {@code arconia.otel.logs.exporter}.
     */
    String prefix();

    /**
     * The protocol used by the OTLP exporter.
     */
    Protocol value();

}
