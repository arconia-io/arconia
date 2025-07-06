package io.arconia.boot.autoconfigure.bootstrap.dev;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for bootstrapping the development mode.
 */
@ConfigurationProperties(prefix = BootstrapDevProperties.CONFIG_PREFIX)
public class BootstrapDevProperties {

    public static final String CONFIG_PREFIX = "arconia.dev";

    /**
     * Name of the profiles to activate in development mode.
     */
    private List<String> profiles = List.of("dev");

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

}
