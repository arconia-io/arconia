package io.arconia.dev.services.lgtm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.images.builder.Transferable;

/**
 * Grafana dashboards provisioned in the Grafana LGTM Dev Service: the built-in dashboards
 * matching the application, and the ones contributed by the classpath under
 * {@value #CONTRIBUTED_LOCATION}. They replace the dashboards bundled with the container image.
 */
final class LgtmDashboards {

    private static final Logger logger = LoggerFactory.getLogger(LgtmDashboards.class);

    /**
     * Classpath location where applications and libraries contribute Grafana dashboards.
     */
    static final String CONTRIBUTED_LOCATION = "META-INF/grafana/dashboards";

    static final String CONTRIBUTED_LOCATION_PATTERN = "classpath*:" + CONTRIBUTED_LOCATION + "/*.json";

    static final String BUILT_IN_LOCATION = "io/arconia/dev/services/lgtm/dashboards";

    /**
     * Directory inside the container where all the dashboards are copied.
     */
    static final String CONTAINER_DASHBOARDS_PATH = "/otel-lgtm/dashboards";

    /**
     * Dashboard provider configuration of the container image, replaced with the Arconia one
     * so that only the Arconia dashboards are provisioned.
     */
    static final String PROVIDER_FILE_PATH = "/otel-lgtm/grafana/conf/provisioning/dashboards/grafana-dashboards.yaml";

    static final String PROVIDER_CONFIG = """
            apiVersion: 1

            providers:
              - name: Arconia
                type: file
                options:
                  path: %s
                  foldersFromFilesStructure: false
            """.formatted(CONTAINER_DASHBOARDS_PATH);

    /**
     * Resources extracted from a jar carry no usable permissions, so an explicit mode
     * makes the files readable by the Grafana process.
     */
    private static final int FILE_MODE = 0644;

    private static final String OPENTELEMETRY_JVM_CONVENTIONS_CLASS = "io.arconia.observation.opentelemetry.autoconfigure.JvmConventionsAutoConfiguration";

    private static final String OPENTELEMETRY_JVM_CONVENTIONS_PROPERTY = "arconia.observations.conventions.opentelemetry.jvm.enabled";

    private static final String LOGBACK_CLASS = "ch.qos.logback.classic.LoggerContext";

    static final List<BuiltInDashboard> BUILT_IN = List.of(
            new BuiltInDashboard("jvm-micrometer.json", openTelemetryJvmConventions().negate()),
            new BuiltInDashboard("jvm-opentelemetry.json", openTelemetryJvmConventions()),
            new BuiltInDashboard("logging.json", classPresent(LOGBACK_CLASS)));

    /**
     * Grafana environment variable pointing at the dashboard to open as the home page.
     */
    static final String HOME_DASHBOARD_ENV = "GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH";

    private final boolean enabled;

    private final Map<String, Resource> dashboards;

    @Nullable
    private final String home;

    private LgtmDashboards(boolean enabled, Map<String, Resource> dashboards, @Nullable String home) {
        this.enabled = enabled;
        this.dashboards = Collections.unmodifiableMap(dashboards);
        this.home = home;
    }

    /**
     * Collect the dashboards to provision for the application: the built-in dashboards whose
     * condition holds, unless turned off, then the contributed ones. When two dashboards share
     * a file name, the first one wins and the other is ignored with a warning. The home dashboard,
     * if configured, must be one of the collected dashboards. When provisioning is disabled,
     * nothing is collected and the container is left as the image ships it.
     */
    static LgtmDashboards collect(Environment environment, ResourceLoader resourceLoader, LgtmDevServicesProperties.Dashboards options) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(resourceLoader, "resourceLoader cannot be null");
        Assert.notNull(options, "options cannot be null");

        if (!options.isEnabled()) {
            logger.debug("Grafana dashboards provisioning is disabled");
            return new LgtmDashboards(false, Map.of(), null);
        }

        Map<String, Resource> dashboards = new LinkedHashMap<>();
        ClassLoader classLoader = resourceLoader.getClassLoader();
        if (options.getBuiltIn().isEnabled()) {
            for (BuiltInDashboard dashboard : BUILT_IN) {
                if (dashboard.condition().test(environment, classLoader)) {
                    dashboards.put(dashboard.fileName(),
                            new ClassPathResource(BUILT_IN_LOCATION + "/" + dashboard.fileName(), LgtmDashboards.class.getClassLoader()));
                }
            }
        }
        for (Resource resource : contributedDashboards(resourceLoader)) {
            String fileName = resource.getFilename();
            Assert.notNull(fileName, "fileName cannot be null");
            Resource existing = dashboards.putIfAbsent(fileName, resource);
            if (existing != null) {
                logger.warn("Ignoring Grafana dashboard '{}' from {} because another one with the same file name is provisioned from {}",
                        fileName, resource, existing);
            }
        }
        String home = options.getHome();
        if (home != null) {
            Assert.state(dashboards.containsKey(home),
                    () -> "Grafana home dashboard '%s' is not among the provisioned dashboards: %s".formatted(home, dashboards.keySet()));
        }
        logger.debug("Grafana dashboards to provision: {}", dashboards.keySet());
        return new LgtmDashboards(true, dashboards, home);
    }

    private static List<Resource> contributedDashboards(ResourceLoader resourceLoader) {
        try {
            return List.of(ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(CONTRIBUTED_LOCATION_PATTERN));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan the classpath for Grafana dashboards in " + CONTRIBUTED_LOCATION, ex);
        }
    }

    /**
     * Whether dashboards are provisioned at all.
     */
    boolean enabled() {
        return enabled;
    }

    /**
     * The dashboards to provision, by file name.
     */
    Map<String, Resource> dashboards() {
        return dashboards;
    }

    /**
     * File name of the dashboard to open as the Grafana home page, if any.
     */
    @Nullable
    String home() {
        return home;
    }

    /**
     * Copy the provider configuration and the dashboards into the container, replacing the
     * dashboards bundled with the image, and point Grafana at the home dashboard, if any.
     * Does nothing when provisioning is disabled.
     */
    void applyTo(LgtmStackContainer container) {
        Assert.notNull(container, "container cannot be null");
        if (!enabled) {
            return;
        }
        container.withCopyToContainer(Transferable.of(PROVIDER_CONFIG.getBytes(StandardCharsets.UTF_8), FILE_MODE), PROVIDER_FILE_PATH);
        dashboards.forEach((fileName, resource) -> container.withCopyToContainer(
                Transferable.of(readContent(resource), FILE_MODE), CONTAINER_DASHBOARDS_PATH + "/" + fileName));
        if (home != null) {
            container.withEnv(HOME_DASHBOARD_ENV, CONTAINER_DASHBOARDS_PATH + "/" + home);
        }
    }

    private static byte[] readContent(Resource resource) {
        try {
            return resource.getContentAsByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read Grafana dashboard " + resource, ex);
        }
    }

    private static BiPredicate<Environment, @Nullable ClassLoader> openTelemetryJvmConventions() {
        return classPresent(OPENTELEMETRY_JVM_CONVENTIONS_CLASS).and(propertyEnabled(OPENTELEMETRY_JVM_CONVENTIONS_PROPERTY));
    }

    private static BiPredicate<Environment, @Nullable ClassLoader> classPresent(String className) {
        return (environment, classLoader) -> ClassUtils.isPresent(className, classLoader);
    }

    private static BiPredicate<Environment, @Nullable ClassLoader> propertyEnabled(String name) {
        return (environment, classLoader) -> environment.getProperty(name, Boolean.class, true);
    }

    /**
     * A dashboard shipped with the Dev Service, provisioned when its condition holds
     * for the application environment and class loader.
     */
    record BuiltInDashboard(String fileName, BiPredicate<Environment, @Nullable ClassLoader> condition) {}

}
