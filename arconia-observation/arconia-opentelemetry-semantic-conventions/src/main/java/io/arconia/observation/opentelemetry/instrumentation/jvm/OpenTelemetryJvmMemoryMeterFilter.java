package io.arconia.observation.opentelemetry.instrumentation.jvm;

import java.util.List;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.opentelemetry.semconv.JvmAttributes;

/**
 * Filter to map missing Micrometer JVM memory metrics to OpenTelemetry semantic conventions.
 */
public final class OpenTelemetryJvmMemoryMeterFilter implements MeterFilter {

    @Override
    public MeterFilterReply accept(Meter.Id id) {
        if ("jvm.memory.usage.after.gc".equals(id.getName())) {
            return MeterFilterReply.DENY;
        }
        return MeterFilterReply.NEUTRAL;
    }

    @Override
    public Meter.Id map(Meter.Id id) {
        String name = id.getName();
        if ("jvm.gc.pause".equals(name) || "jvm.gc.concurrent.phase.time".equals(name)) {

            // 'jvm.gc.cause' is not stable yet, but it's kept: the stable 'jvm.gc.duration' metric defines it as opt-in,
            // the OpenTelemetry Java instrumentation records it, and together with 'jvm.gc.name' it tells GC pauses
            // from concurrent phases, which Micrometer records in separate timers.
            List<Tag> mappedTags = id.getTags().stream()
                    .map(t -> {
                        if ("gc".equals(t.getKey())) return Tag.of(JvmAttributes.JVM_GC_NAME.getKey(), t.getValue());
                        if ("action".equals(t.getKey())) return Tag.of(JvmAttributes.JVM_GC_ACTION.getKey(), t.getValue());
                        if ("cause".equals(t.getKey())) return Tag.of("jvm.gc.cause", t.getValue());
                        return t;
                    })
                    .toList();

            return id.withName("jvm.gc.duration").replaceTags(mappedTags);
        }
        return id;
    }

}
