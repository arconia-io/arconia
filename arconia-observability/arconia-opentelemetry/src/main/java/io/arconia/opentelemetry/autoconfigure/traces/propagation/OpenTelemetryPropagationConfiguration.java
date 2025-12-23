/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.arconia.opentelemetry.autoconfigure.traces.propagation;

import java.util.List;

import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;

import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.traces.exporter.ConditionalOnOpenTelemetryTracingExporter;

/**
 * Configuration for OpenTelemetry propagation.
 * <p>
 * Code adapted from <a href="https://github.com/spring-projects/spring-boot/blob/v4.0.1/module/spring-boot-micrometer-tracing-opentelemetry/src/main/java/org/springframework/boot/micrometer/tracing/opentelemetry/autoconfigure/OpenTelemetryPropagationConfigurations.java">OpenTelemetryPropagationConfigurations.java</a>
 * authored by Moritz Halbritter in the Spring Boot project.
 */
public class OpenTelemetryPropagationConfiguration {

	/**
	 * Propagates traces but no baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty(name = "management.tracing.baggage.enabled", havingValue = false)
	@EnableConfigurationProperties(TracingProperties.class)
    public static class PropagationWithoutBaggage {

		@Bean
        @ConditionalOnOpenTelemetryTracingExporter("otlp")
		TextMapPropagator textMapPropagator(TracingProperties properties) {
			return CompositeTextMapPropagator.create(properties.getPropagation(), null);
		}

	}

	/**
	 * Propagates traces and baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty(name = "management.tracing.baggage.enabled", matchIfMissing = true)
	@EnableConfigurationProperties(TracingProperties.class)
    public static class PropagationWithBaggage {

		@Bean
		@ConditionalOnOpenTelemetryTracingExporter("otlp")
		TextMapPropagator textMapPropagatorWithBaggage(OtelCurrentTraceContext otelCurrentTraceContext, TracingProperties tracingProperties) {
			List<String> remoteFields = tracingProperties.getBaggage().getRemoteFields();
			List<String> tagFields = tracingProperties.getBaggage().getTagFields();
			BaggageTextMapPropagator baggagePropagator = new BaggageTextMapPropagator(remoteFields,
					new OtelBaggageManager(otelCurrentTraceContext, remoteFields, tagFields));
			return CompositeTextMapPropagator.create(tracingProperties.getPropagation(), baggagePropagator);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBooleanProperty(name = "management.tracing.baggage.correlation.enabled", matchIfMissing = true)
		Slf4JBaggageEventListener otelSlf4JBaggageEventListener(TracingProperties tracingProperties) {
			return new Slf4JBaggageEventListener(tracingProperties.getBaggage().getCorrelation().getFields());
		}

	}

	/**
	 * Propagates neither traces nor baggage.
	 */
	@Configuration(proxyBeanMethods = false)
    public static class NoPropagation {

		@Bean
		@ConditionalOnMissingBean
		TextMapPropagator noopTextMapPropagator() {
			return TextMapPropagator.noop();
		}

	}

}
