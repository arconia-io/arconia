package io.arconia.boot.env;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuring profiles based on the application mode.
 */
@ConfigurationProperties(prefix = ProfilesProperties.CONFIG_PREFIX)
public class ProfilesProperties {

    public static final String CONFIG_PREFIX = "arconia.config.profiles";

    /**
     * Whether the profiles are enabled based on the application mode.
     */
    private boolean enabled = true;

    /**
     * Names of the profiles to activate in development mode.
     */
    private List<String> development = List.of("dev");

    /**
     * Names of the profiles to activate in test mode.
     */
    private List<String> test = List.of("test");

    /**
     * Names of the profiles to activate in production mode.
     */
    private List<String> production = List.of("prod");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDevelopment() {
        return development;
    }

    public void setDevelopment(List<String> development) {
        this.development = development;
    }

    public List<String> getTest() {
        return test;
    }

    public void setTest(List<String> test) {
        this.test = test;
    }

    public List<String> getProduction() {
        return production;
    }

    public void setProduction(List<String> production) {
        this.production = production;
    }

}
