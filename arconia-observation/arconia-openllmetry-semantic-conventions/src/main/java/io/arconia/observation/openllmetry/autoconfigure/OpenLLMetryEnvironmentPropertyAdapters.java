package io.arconia.observation.openllmetry.autoconfigure;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;

/**
 * Provides adapters from OpenLLMetry/Traceloop environment variables to Arconia properties.
 *
 * @see <a href="https://www.traceloop.com/docs/openllmetry/configuration">OpenLLMetry Configuration</a>
 */
class OpenLLMetryEnvironmentPropertyAdapters {

    /**
     * OpenLLMetry tracing configuration.
     *
     * @see <a href="https://www.traceloop.com/docs/openllmetry/configuration">Tracing Configuration</a>
     */
    static PropertyAdapter traces(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        String prefix = OpenLLMetryProperties.CONFIG_PREFIX;
        return PropertyAdapter.builder(environment)
                .mapBoolean("TRACELOOP_TRACE_CONTENT", prefix + ".trace-content")
                .build();
    }

}
