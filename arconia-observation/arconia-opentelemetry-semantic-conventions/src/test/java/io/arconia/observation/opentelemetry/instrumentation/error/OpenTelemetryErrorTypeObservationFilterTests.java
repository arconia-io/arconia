package io.arconia.observation.opentelemetry.instrumentation.error;

import java.net.UnknownHostException;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryErrorTypeObservationFilter}.
 */
class OpenTelemetryErrorTypeObservationFilterTests {

    private final OpenTelemetryErrorTypeObservationFilter filter = new OpenTelemetryErrorTypeObservationFilter();

    @Nested
    class Mapping {

        @Test
        void shouldRecordErrorTypeWhenObservationEndsInError() {
            Observation.Context context = new Observation.Context();
            context.setError(new UnknownHostException("example.com"));

            filter.map(context);

            assertThat(context.getLowCardinalityKeyValue("error.type"))
                    .isEqualTo(KeyValue.of("error.type", "java.net.UnknownHostException"));
        }

        @Test
        void shouldKeepErrorTypeSetByConvention() {
            Observation.Context context = new Observation.Context();
            context.setError(new IllegalStateException());
            context.addLowCardinalityKeyValue(KeyValue.of("error.type", "500"));

            filter.map(context);

            assertThat(context.getLowCardinalityKeyValue("error.type")).isEqualTo(KeyValue.of("error.type", "500"));
        }

        @Test
        void shouldReplaceNoneErrorTypeWhenObservationEndsInError() {
            Observation.Context context = new Observation.Context();
            context.setError(new IllegalStateException());
            context.addLowCardinalityKeyValue(KeyValue.of("error.type", KeyValue.NONE_VALUE));

            filter.map(context);

            assertThat(context.getLowCardinalityKeyValue("error.type"))
                    .isEqualTo(KeyValue.of("error.type", "java.lang.IllegalStateException"));
        }

        @Test
        void shouldNotRecordErrorTypeWhenObservationSucceeds() {
            Observation.Context context = new Observation.Context();

            filter.map(context);

            assertThat(context.getLowCardinalityKeyValue("error.type")).isNull();
        }

        @Test
        void shouldRemoveNoneErrorTypeWhenObservationSucceeds() {
            Observation.Context context = new Observation.Context();
            context.addLowCardinalityKeyValue(KeyValue.of("error.type", KeyValue.NONE_VALUE));

            filter.map(context);

            assertThat(context.getLowCardinalityKeyValue("error.type")).isNull();
        }

    }

    @Nested
    class Metrics {

        private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        private final ObservationRegistry observationRegistry = ObservationRegistry.create();

        Metrics() {
            observationRegistry.observationConfig()
                    .observationFilter(filter)
                    .observationHandler(new DefaultMeterObservationHandler(meterRegistry));
        }

        @Test
        void shouldTagTimerWithErrorTypeWhenObservationEndsInError() {
            Observation observation = Observation.start("test.operation", observationRegistry);
            observation.error(new UnknownHostException("example.com"));
            observation.stop();

            Timer timer = meterRegistry.get("test.operation").timer();
            assertThat(timer.getId().getTag("error.type")).isEqualTo("java.net.UnknownHostException");
        }

    }

}
