package io.arconia.opentelemetry.autoconfigure.traces.bridge;

import java.util.List;

import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelSpanCustomizer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;

import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
public class MicrometerTracingOpenTelemetryBridgeConfiguration {

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
    OtelTracer micrometerOtelTracer(Tracer tracer,
                                    OtelCurrentTraceContext otelCurrentTraceContext,
                                    OtelTracer.EventPublisher eventPublisher,
                                    TracingProperties tracingProperties
    ) {
        List<String> remoteFields = tracingProperties.getBaggage().getRemoteFields();
        List<String> tagFields = tracingProperties.getBaggage().getTagFields();
        OtelBaggageManager baggageManager = new OtelBaggageManager(otelCurrentTraceContext, remoteFields, tagFields);
        return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher, baggageManager);
    }

    @Bean
    @ConditionalOnMissingBean
    OtelCurrentTraceContext otelCurrentTraceContext() {
        return new OtelCurrentTraceContext();
    }

    @Bean
    @ConditionalOnMissingBean
    OtelPropagator otelPropagator(ContextPropagators contextPropagators, Tracer tracer) {
        return new OtelPropagator(contextPropagators, tracer);
    }

    @Bean
    @ConditionalOnMissingBean(SpanCustomizer.class)
    OtelSpanCustomizer otelSpanCustomizer() {
        return new OtelSpanCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    OtelTracer.EventPublisher otelTracerEventPublisher(List<EventListener> eventListeners) {
        return new OTelTracerEventPublisher(eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean
    Slf4JEventListener otelSlf4JEventListener() {
        return new Slf4JEventListener();
    }

    static final class OTelTracerEventPublisher implements OtelTracer.EventPublisher {

        private final List<EventListener> eventListeners;

        OTelTracerEventPublisher(List<EventListener> eventListeners) {
            Assert.notNull(eventListeners, "eventListeners cannot be null");
            Assert.noNullElements(eventListeners, "eventListeners cannot contain null elements");
            this.eventListeners = eventListeners;
        }

        @Override
        public void publishEvent(Object event) {
            for (EventListener eventListener : eventListeners) {
                eventListener.onEvent(event);
            }
        }

    }

}
