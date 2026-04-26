package io.arconia.observation.opentelemetry.instrumentation.jvm;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.opentelemetry.semconv.JvmAttributes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryJvmMemoryMeterFilter}.
 */
class OpenTelemetryJvmMemoryMeterFilterTests {

    private final OpenTelemetryJvmMemoryMeterFilter filter = new OpenTelemetryJvmMemoryMeterFilter();

    @Test
    void deniesMemoryUsageAfterGc() {
        Meter.Id id = new Meter.Id("jvm.memory.usage.after.gc",
                Tags.empty(),
                null, null, Meter.Type.GAUGE);

        assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.DENY);
    }

    @Test
    void acceptsUnrelatedMeters() {
        Meter.Id id = new Meter.Id("jvm.memory.used",
                Tags.of("area", "heap"),
                null, null, Meter.Type.GAUGE);

        assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.NEUTRAL);
    }

    @Test
    void mapsGcPauseToGcDuration() {
        Meter.Id id = new Meter.Id("jvm.gc.pause",
                Tags.of("gc", "G1 Young Generation", "action", "end of minor GC", "cause", "G1 Evacuation Pause"),
                null, null, Meter.Type.TIMER);

        Meter.Id mapped = filter.map(id);

        assertThat(mapped.getName()).isEqualTo("jvm.gc.duration");
        assertThat(mapped.getTag(JvmAttributes.JVM_GC_NAME.getKey())).isEqualTo("G1 Young Generation");
        assertThat(mapped.getTag(JvmAttributes.JVM_GC_ACTION.getKey())).isEqualTo("end of minor GC");
        assertThat(mapped.getTag("jvm.gc.cause")).isEqualTo("G1 Evacuation Pause");
    }

    @Test
    void mapsGcConcurrentPhaseTimeToGcDuration() {
        Meter.Id id = new Meter.Id("jvm.gc.concurrent.phase.time",
                Tags.of("gc", "G1 Concurrent GC", "action", "No GC", "cause", "No GC"),
                null, null, Meter.Type.TIMER);

        Meter.Id mapped = filter.map(id);

        assertThat(mapped.getName()).isEqualTo("jvm.gc.duration");
        assertThat(mapped.getTag(JvmAttributes.JVM_GC_NAME.getKey())).isEqualTo("G1 Concurrent GC");
        assertThat(mapped.getTag(JvmAttributes.JVM_GC_ACTION.getKey())).isEqualTo("No GC");
        assertThat(mapped.getTag("jvm.gc.cause")).isEqualTo("No GC");
    }

    @Test
    void preservesUnrelatedTagsOnGcMeters() {
        Meter.Id id = new Meter.Id("jvm.gc.pause",
                Tags.of("gc", "G1", "action", "end of minor GC", "custom", "value"),
                null, null, Meter.Type.TIMER);

        Meter.Id mapped = filter.map(id);

        assertThat(mapped.getTag("custom")).isEqualTo("value");
    }

    @Test
    void doesNotMapUnrelatedMeters() {
        Meter.Id id = new Meter.Id("jvm.memory.used",
                Tags.of("area", "heap"),
                null, null, Meter.Type.GAUGE);

        Meter.Id mapped = filter.map(id);

        assertThat(mapped).isSameAs(id);
    }

}
