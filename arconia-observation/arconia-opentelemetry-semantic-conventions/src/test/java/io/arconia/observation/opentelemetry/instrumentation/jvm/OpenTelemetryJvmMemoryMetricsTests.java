package io.arconia.observation.opentelemetry.instrumentation.jvm;

import java.util.List;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.semconv.JvmAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryJvmMemoryMetrics}.
 */
class OpenTelemetryJvmMemoryMetricsTests {

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void registersUsedAfterLastGcGauges() {
        var conventions = new OpenTelemetryJvmMemoryMeterConventions(Tags.empty());
        var metrics = new OpenTelemetryJvmMemoryMetrics(List.of(), conventions);

        metrics.bindTo(meterRegistry);

        List<Gauge> gauges = meterRegistry.find("jvm.memory.used_after_last_gc").gauges()
                .stream().toList();

        // Only pools with collectionUsage get this gauge — at least one should exist on any JVM
        assertThat(gauges).isNotEmpty();

        for (Gauge gauge : gauges) {
            assertThat(gauge.getId().getTag(JvmAttributes.JVM_MEMORY_POOL_NAME.getKey())).isNotNull();
            assertThat(gauge.getId().getTag(JvmAttributes.JVM_MEMORY_TYPE.getKey()))
                    .isIn(JvmAttributes.JvmMemoryTypeValues.HEAP, JvmAttributes.JvmMemoryTypeValues.NON_HEAP);
            assertThat(gauge.getId().getBaseUnit()).isEqualTo(BaseUnits.BYTES);
        }
    }

    @Test
    void alsoRegistersParentMetrics() {
        var conventions = new OpenTelemetryJvmMemoryMeterConventions(Tags.empty());
        var metrics = new OpenTelemetryJvmMemoryMetrics(List.of(), conventions);

        metrics.bindTo(meterRegistry);

        // Verify that parent JvmMemoryMetrics metrics are also registered
        assertThat(meterRegistry.find("jvm.memory.used").gauges()).isNotEmpty();
        assertThat(meterRegistry.find("jvm.memory.committed").gauges()).isNotEmpty();
        assertThat(meterRegistry.find("jvm.memory.limit").gauges()).isNotEmpty();
    }

}
