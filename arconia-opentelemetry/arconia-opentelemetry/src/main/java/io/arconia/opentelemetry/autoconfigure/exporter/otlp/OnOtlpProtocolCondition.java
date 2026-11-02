package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotatedTypeMetadata;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;

/**
 * Determines if the OTLP exporter of a signal uses a certain protocol.
 */
class OnOtlpProtocolCondition extends SpringBootCondition {

    private static final String COMMON_PROTOCOL = OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MergedAnnotation<ConditionalOnOtlpProtocol> annotation = metadata.getAnnotations().get(ConditionalOnOtlpProtocol.class);
        String signalProtocol = annotation.getString("prefix") + ".otlp.protocol";
        Protocol requiredProtocol = annotation.getEnum(MergedAnnotation.VALUE, Protocol.class);
        ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnOtlpProtocol.class, requiredProtocol);

        // Bound like the configuration properties, so that values such as "http-protobuf" are accepted.
        Binder binder = Binder.get(context.getEnvironment());
        String property = signalProtocol;
        Protocol protocol = binder.bind(signalProtocol, Protocol.class).orElse(null);
        if (protocol == null) {
            property = COMMON_PROTOCOL;
            protocol = binder.bind(COMMON_PROTOCOL, Protocol.class).orElse(Protocol.HTTP_PROTOBUF);
        }

        return new ConditionOutcome(protocol == requiredProtocol, message.because(property + " is " + protocol));
    }

}
