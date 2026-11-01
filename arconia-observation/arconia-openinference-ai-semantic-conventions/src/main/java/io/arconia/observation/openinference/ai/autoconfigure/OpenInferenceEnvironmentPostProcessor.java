package io.arconia.observation.openinference.ai.autoconfigure;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

/**
 * Converts OpenInference Environment Variables to Arconia properties.
 */
class OpenInferenceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "openinference-environment-variable-specification";

    private final Log logger;

    /**
     * The logger is deferred until the logging system is initialized, since any message
     * logged before that would be lost.
     */
    OpenInferenceEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(OpenInferenceEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(new OpenInferenceEnvironmentPropertyAdapter(environment, logger));
        environment.getPropertySources().addFirst(adapter.toPropertySource(PROPERTY_SOURCE_NAME));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
