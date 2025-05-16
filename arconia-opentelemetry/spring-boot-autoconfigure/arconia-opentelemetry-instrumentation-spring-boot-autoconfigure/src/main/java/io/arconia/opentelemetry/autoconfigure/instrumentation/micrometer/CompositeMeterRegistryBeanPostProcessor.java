package io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Ensure that the {@link OpenTelemetryMeterRegistry} is always last in the
 * {@link CompositeMeterRegistry} registry list. This is important because
 * Spring Boot Actuator (MetricsEndpoint) reads metrics from the first registry
 * in the list, an operation not supported by the OpenTelemetry registry
 * (focused on exporting metrics).
 * <p>
 * See more information about the <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/12719">issue</a>
 * which inspired this solution.
 */
public class CompositeMeterRegistryBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // The CompositeMeterRegistryAutoConfiguration in Spring Boot doesn't support ordering of the registries
        // via the usual @Order annotation. Therefore, we need to do the sorting manually after the initialization
        // of the CompositeMeterRegistry bean.
        if (bean instanceof CompositeMeterRegistry compositeMeterRegistry) {
            List<MeterRegistry> meterRegistries = new ArrayList<>(compositeMeterRegistry.getRegistries()).stream()
                    .sorted(Comparator.comparingInt(value -> value instanceof OpenTelemetryMeterRegistry ? 1 : 0))
                    .toList();
            Set<MeterRegistry> sortedMeterRegistries = new LinkedHashSet<>(meterRegistries);
            return new CompositeMeterRegistry(compositeMeterRegistry.config().clock(), List.of(compositeMeterRegistry)) {
                @Override
                public Set<MeterRegistry> getRegistries() {
                    return sortedMeterRegistries;
                }
            };
        } else {
            return bean;
        }
    }

}
