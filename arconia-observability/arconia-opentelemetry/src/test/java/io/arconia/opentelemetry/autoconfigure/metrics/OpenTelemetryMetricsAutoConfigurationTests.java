package io.arconia.opentelemetry.autoconfigure.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryMetricsAutoConfiguration}.
 */
class OpenTelemetryMetricsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryMetricsAutoConfiguration.class))
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, () -> mock(OpenTelemetry.class));

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkMeterProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkMeterProvider.class));
    }

    @Test
    void meterProviderAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SdkMeterProvider.class);
            assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
            assertThat(context).hasSingleBean(ExemplarFilter.class);
            assertThat(context).hasSingleBean(Meter.class);
        });
    }

    @Test
    void cardinalityLimitSelectorConfigurationApplied() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.cardinality-limit=200")
                .run(context -> {
                    CardinalityLimitSelector cardinalityLimitSelector = context.getBean(CardinalityLimitSelector.class);
                    assertThat(cardinalityLimitSelector.getCardinalityLimit(InstrumentType.COUNTER)).isEqualTo(200);
                });
    }

    @Test
    void exemplarFilterConfigurationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplars.filter=always-on")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOn());
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplars.filter=always-off")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOff());
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplars.filter=trace-based")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.traceBased());
            });
    }

    @Test
    void customCardinalityLimitSelectorAvailable() {
        contextRunner
            .withUserConfiguration(CustomCardinalityLimitSelectorConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
                assertThat(context.getBean(CardinalityLimitSelector.class))
                    .isSameAs(context.getBean(CustomCardinalityLimitSelectorConfiguration.class).customCardinalityLimitSelector());
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCardinalityLimitSelectorConfiguration {

        private final CardinalityLimitSelector customCardinalityLimitSelector = mock(CardinalityLimitSelector.class);

        @Bean
        CardinalityLimitSelector customCardinalityLimitSelector() {
            return customCardinalityLimitSelector;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMetricBuilderCustomizerConfiguration {

        private final OpenTelemetryMeterProviderBuilderCustomizer customMetricBuilderCustomizer = mock(OpenTelemetryMeterProviderBuilderCustomizer.class);

        @Bean
        OpenTelemetryMeterProviderBuilderCustomizer customMetricBuilderCustomizer() {
            return customMetricBuilderCustomizer;
        }

    }

}
