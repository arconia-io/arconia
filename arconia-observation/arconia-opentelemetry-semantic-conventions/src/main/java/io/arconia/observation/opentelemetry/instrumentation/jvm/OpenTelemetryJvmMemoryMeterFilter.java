package io.arconia.observation.opentelemetry.instrumentation.jvm;

import java.util.List;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.opentelemetry.semconv.JvmAttributes;

/**
 * Filter to map missing Micrometer JVM memory metrics to OpenTelemetry semantic conventions.
 */
public final class OpenTelemetryJvmMemoryMeterFilter implements MeterFilter {

    @Override
    public Meter.Id map(Meter.Id id) {
        String name = id.getName();
        if ("jvm.gc.pause".equals(name) || "jvm.gc.concurrent.phase.time".equals(name)) {

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
