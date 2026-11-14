package io.arconia.core.bootstrap.autoconfigure;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.bootstrap.BootstrapConfigurationFile;
import io.arconia.core.bootstrap.BootstrapMode;
import io.arconia.core.bootstrap.BootstrapModeDetector;

/**
 * Activates the Spring profiles for the current bootstrap mode before the Spring Boot
 * configuration data import runs, so that profile-specific configuration files are loaded
 * by the regular, single import pass.
 * <p>
 * Since this post-processor runs before the application configuration is loaded, the bootstrap
 * settings can only come from pre-configuration sources: JVM system properties, environment
 * variables, command line arguments, or the {@value BootstrapConfigurationFile#LOCATION} file.
 * {@link MisplacedBootstrapPropertiesEnvironmentPostProcessor} warns about values defined
 * in the application configuration, where they are not supported.
 */
class BootstrapEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * Name of the property source holding the content of the bootstrap configuration file.
     */
    static final String PROPERTY_SOURCE_NAME = "arconiaBootstrap";

    private final Log logger;

    /**
     * The logger is deferred until the logging system is initialized, since any message
     * logged before that would be lost.
     */
    BootstrapEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(BootstrapEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(application, "application cannot be null");

        BootstrapMode mode = BootstrapMode.detect();
        logBootstrapMode(mode);

        // No profile is activated in prod mode, so the bootstrap settings are not needed.
        if (mode == BootstrapMode.PROD) {
            return;
        }

        addBootstrapConfigurationFile(environment, application);

        Binder binder = Binder.get(environment);

        Boolean profilesEnabled = binder.bind(BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY, Boolean.class).orElse(true);
        if (!profilesEnabled) {
            return;
        }

        List<String> modeProfiles = switch (mode) {
            case DEV -> binder.bind(BootstrapConfigurationFile.DEV_PROFILES_PROPERTY, Bindable.listOf(String.class)).orElse(List.of("dev"));
            case TEST -> binder.bind(BootstrapConfigurationFile.TEST_PROFILES_PROPERTY, Bindable.listOf(String.class)).orElse(List.of("test"));
            case PROD -> List.of();
        };

        // Consider both the property-declared and the programmatically-set active profiles,
        // so already-active profiles are not activated twice.
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        Set<String> currentProfiles = new LinkedHashSet<>(binder.bind(StandardEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, Bindable.listOf(String.class)).orElse(List.of()));
        currentProfiles.addAll(activeProfiles);

        Set<String> mergedProfiles = new LinkedHashSet<>();
        for (String profile : modeProfiles) {
            if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                logger.debug("Adding active profile '%s' for the current bootstrap mode".formatted(profile));
                mergedProfiles.add(profile);
            }
        }

        if (mergedProfiles.isEmpty()) {
            return;
        }

        // The mode profiles come first so that their profile-specific configuration files have
        // lower precedence than the ones for the profiles activated by the user.
        mergedProfiles.addAll(activeProfiles);
        environment.setActiveProfiles(mergedProfiles.toArray(String[]::new));
    }

    /**
     * Reports the detected bootstrap mode, together with any problem with how it was set.
     * The detection itself doesn't log, since it typically happens before the logging
     * system is initialized.
     */
    private void logBootstrapMode(BootstrapMode mode) {
        String explicitMode = BootstrapModeDetector.getExplicitValue();
        if (explicitMode != null && BootstrapModeDetector.parse(explicitMode) == null) {
            logger.warn("Invalid %s property value: '%s'. Defaulting to PROD mode."
                    .formatted(BootstrapMode.SYSTEM_PROPERTY, explicitMode));
        }

        // The test mode is expected when processing the tests ahead-of-time, whereas the dev mode
        // is never detected during AOT processing, so it can only come from an explicit value.
        if (mode == BootstrapMode.DEV && Boolean.getBoolean(AbstractAotProcessor.AOT_PROCESSING)) {
            logger.warn("The bootstrap mode is set to DEV while Spring AOT processing is running. "
                    + "The DEV mode behavior will be baked into the AOT artifacts.");
        }

        if (mode == BootstrapMode.PROD) {
            logger.debug("The application is running in prod mode");
        } else {
            logger.info("The application is running in %s mode".formatted(mode.name().toLowerCase(Locale.ROOT)));
        }
    }

    /**
     * Exposes the bootstrap settings from the bootstrap configuration file as a property source,
     * below the system and command line ones, so they participate in the binding of the bootstrap
     * settings and remain inspectable at runtime. Any other property in the file is reported and
     * left out, so that the file cannot be used to configure the application.
     */
    private void addBootstrapConfigurationFile(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Properties properties;
        try {
            properties = BootstrapConfigurationFile.load(application.getClassLoader());
        } catch (IOException ex) {
            logger.warn("Failed to load the bootstrap configuration file from %s".formatted(BootstrapConfigurationFile.LOCATION), ex);
            return;
        }
        Map<String, Object> bootstrapProperties = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (isSupported(key)) {
                bootstrapProperties.put(key, properties.getProperty(key));
            } else {
                logger.warn("The '%s' property is not supported in the %s file and is ignored. Supported properties: %s."
                        .formatted(key, BootstrapConfigurationFile.LOCATION,
                                String.join(", ", BootstrapConfigurationFile.SUPPORTED_PROPERTIES)));
            }
        }
        if (!bootstrapProperties.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, bootstrapProperties));
        }
    }

    /**
     * Whether the key is one of the bootstrap settings, including the indexed form of a list.
     */
    private static boolean isSupported(String key) {
        for (String property : BootstrapConfigurationFile.SUPPORTED_PROPERTIES) {
            if (key.equals(property) || key.startsWith(property + "[")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        // Right before the Spring Boot configuration data import.
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }

}
