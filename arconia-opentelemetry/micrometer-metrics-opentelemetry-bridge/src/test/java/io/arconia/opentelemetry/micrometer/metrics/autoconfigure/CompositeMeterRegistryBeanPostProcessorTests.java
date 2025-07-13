package io.arconia.opentelemetry.micrometer.metrics.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link CompositeMeterRegistryBeanPostProcessor}.
 */
class CompositeMeterRegistryBeanPostProcessorTests {

    private final CompositeMeterRegistryBeanPostProcessor processor = new CompositeMeterRegistryBeanPostProcessor();

    @Test
    void shouldNotModifyOtherBeans() {
        var otherBean = new Object();
        var result = processor.postProcessAfterInitialization(otherBean, "otherBean");
        assertThat(result).isSameAs(otherBean);
    }

    @Test
    void shouldReorderRegistriesWithOpenTelemetryLast() {
        var clock = new MockClock();
        var otelRegistry = mock(OpenTelemetryMeterRegistry.class);
        var simpleRegistry1 = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
        var simpleRegistry2 = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        var registries = new LinkedHashSet<>(Arrays.asList(otelRegistry, simpleRegistry1, simpleRegistry2));
        var compositeRegistry = new CompositeMeterRegistry(clock, registries);

        var meterRegistry = (CompositeMeterRegistry) processor.postProcessAfterInitialization(compositeRegistry, "compositeMeterRegistry");

        assertThat(meterRegistry).isNotNull();
        List<MeterRegistry> reorderedRegistries = new ArrayList<>(meterRegistry.getRegistries());
        assertThat(reorderedRegistries).hasSize(3);
        assertThat(reorderedRegistries).contains(simpleRegistry1, simpleRegistry2);
        assertThat(reorderedRegistries.getLast()).isEqualTo(otelRegistry);
    }

    @Test
    void shouldHandleNoOpenTelemetryRegistry() {
        var clock = new MockClock();
        var simpleRegistry1 = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
        var simpleRegistry2 = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        var registries = new LinkedHashSet<MeterRegistry>(Arrays.asList(simpleRegistry1, simpleRegistry2));
        var compositeRegistry = new CompositeMeterRegistry(clock, registries);

        var result = (CompositeMeterRegistry) processor.postProcessAfterInitialization(compositeRegistry, "compositeMeterRegistry");

        assertThat(result).isNotNull();
        List<MeterRegistry> reorderedRegistries = new ArrayList<>(result.getRegistries());
        assertThat(reorderedRegistries).containsExactlyInAnyOrder(simpleRegistry1, simpleRegistry2);
    }

    @Test
    void shouldHandleOpenTelemetryRegistryAlreadyLast() {
        var clock = new MockClock();
        var otelRegistry = mock(OpenTelemetryMeterRegistry.class);
        var simpleRegistry1 = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        var registries = new LinkedHashSet<MeterRegistry>(Arrays.asList(simpleRegistry1, otelRegistry));
        var compositeRegistry = new CompositeMeterRegistry(clock, registries);

        var result = (CompositeMeterRegistry) processor.postProcessAfterInitialization(compositeRegistry, "compositeMeterRegistry");

        assertThat(result).isNotNull();
        List<MeterRegistry> reorderedRegistries = new ArrayList<>(result.getRegistries());
        assertThat(reorderedRegistries).hasSize(2);
        assertThat(reorderedRegistries.get(0)).isSameAs(simpleRegistry1);
        assertThat(reorderedRegistries.get(1)).isSameAs(otelRegistry);
    }

    @Test
    void shouldHandleEmptyRegistries() {
        var clock = new MockClock();
        var compositeRegistry = new CompositeMeterRegistry(clock, Collections.emptySet());

        var result = (CompositeMeterRegistry) processor.postProcessAfterInitialization(compositeRegistry, "compositeMeterRegistry");

        assertThat(result).isNotNull();
        assertThat(result.getRegistries()).isEmpty();
    }

    @Test
    void shouldHandleOnlyOpenTelemetryRegistry() {
        var clock = new MockClock();
        var otelRegistry = mock(OpenTelemetryMeterRegistry.class);

        var registries = new LinkedHashSet<MeterRegistry>(Collections.singletonList(otelRegistry));
        var compositeRegistry = new CompositeMeterRegistry(clock, registries);

        var result = (CompositeMeterRegistry) processor.postProcessAfterInitialization(compositeRegistry, "compositeMeterRegistry");

        assertThat(result).isNotNull();
        List<MeterRegistry> reorderedRegistries = new ArrayList<>(result.getRegistries());
        assertThat(reorderedRegistries).hasSize(1);
        assertThat(reorderedRegistries.getFirst()).isSameAs(otelRegistry);
    }

}
