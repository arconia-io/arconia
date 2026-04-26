package io.arconia.observation.langsmith.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.observation.langsmith.instrumentation.LangSmithOptions;

/**
 * Configuration properties for the LangSmith Semantic Conventions.
 */
@ConfigurationProperties(prefix = LangSmithConventionsProperties.CONFIG_PREFIX)
public class LangSmithConventionsProperties extends LangSmithOptions {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.langsmith";

    /**
     * Whether to enable LangSmith semantic conventions.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
