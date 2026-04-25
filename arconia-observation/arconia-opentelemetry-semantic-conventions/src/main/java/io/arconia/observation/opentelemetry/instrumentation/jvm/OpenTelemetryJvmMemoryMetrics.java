package io.arconia.observation.opentelemetry.instrumentation.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.opentelemetry.semconv.JvmAttributes;

/**
 * Extends JvmMemoryMetrics to provide the missing OpenTelemetry-compatible metrics for JVM memory usage.
 */
public final class OpenTelemetryJvmMemoryMetrics extends JvmMemoryMetrics {

    public OpenTelemetryJvmMemoryMetrics(Iterable<? extends Tag> extraTags, JvmMemoryMeterConventions conventions) {
        super(extraTags, conventions);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        super.bindTo(registry);

        for (MemoryPoolMXBean memoryPoolBean : ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class)) {
            if (memoryPoolBean.getCollectionUsage() != null) {
                Gauge.builder("jvm.memory.used_after_last_gc", memoryPoolBean, pool -> {
                            MemoryUsage collectionUsage = pool.getCollectionUsage();
                            return collectionUsage != null ? collectionUsage.getUsed() : Double.NaN;
                        })
                        .tags(Tags.of(
                                JvmAttributes.JVM_MEMORY_POOL_NAME.getKey(), memoryPoolBean.getName(),
                                JvmAttributes.JVM_MEMORY_TYPE.getKey(), MemoryType.HEAP.equals(memoryPoolBean.getType()) ?
                                        JvmAttributes.JvmMemoryTypeValues.HEAP :
                                        JvmAttributes.JvmMemoryTypeValues.NON_HEAP))
                        .description("Measure of memory used, as measured after the most recent garbage collection event on this pool.")
                        .baseUnit(BaseUnits.BYTES)
                        .register(registry);
            }
        }
    }

}
