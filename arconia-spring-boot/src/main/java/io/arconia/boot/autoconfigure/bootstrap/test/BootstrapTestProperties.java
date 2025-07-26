package io.arconia.boot.autoconfigure.bootstrap.test;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for bootstrapping the test mode.
 */
@ConfigurationProperties(prefix = BootstrapTestProperties.CONFIG_PREFIX)
public class BootstrapTestProperties {

    public static final String CONFIG_PREFIX = "arconia.test";

    /**
     * Name of the profiles to activate in test mode.
     */
    private List<String> profiles = List.of("test");

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

}
