package io.arconia.dev.ui.server.pages;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.api.page.DevUiSection;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;

/**
 * The page of the Dev UI that shows the configuration properties of the application: every property the
 * application and its libraries document, with the value it has, whether that is its default
 * or a value set somewhere, and where.
 * <p>
 * The properties and what is known about them (description, type, default value, deprecation)
 * come from the configuration metadata that each library ships in its jar. The values come
 * from the environment, looked up the way Spring Boot binds them, so that an environment
 * variable named {@code SERVER_PORT} is the value of {@code server.port}.
 * <p>
 * A property that is set without being documented is listed too, since it may well be one
 * the application reads. The exceptions are the system properties and the environment
 * variables, which are mostly not configuration and have the Environment page to themselves:
 * they are listed here only when they set a documented property.
 */
@Internal
public final class PropertiesPage {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesPage.class);

    private static final String METADATA_LOCATION = "classpath*:META-INF/spring-configuration-metadata.json";

    private static final Set<String> SOURCES_NOT_ABOUT_CONFIGURATION = Set.of("systemProperties", "systemEnvironment");

    private static final String SET = "Set";

    private static final String DEFAULT = "Default";

    private PropertiesPage() {}

    /**
     * Creates the page for the given environment, with the configuration metadata found by the
     * given class loader. The sanitizing functions are the ones the application defines to say
     * which values are sensitive.
     */
    public static TablePage create(ConfigurableEnvironment environment, @Nullable ClassLoader classLoader,
            Collection<SanitizingFunction> sanitizingFunctions
    ) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(sanitizingFunctions, "sanitizingFunctions cannot be null");
        SensitiveProperties sensitiveProperties = new SensitiveProperties(sanitizingFunctions);
        // The metadata is in the jars of the application, which don't change while it runs.
        Supplier<Metadata> metadata = SingletonSupplier.of(() -> loadMetadata(classLoader));
        return TablePage.builder()
                .id("properties")
                .title("Properties")
                .icon("sliders-horizontal")
                .section(DevUiSection.CONFIGURATION)
                .order(-100)
                .columns(List.of("Property", "Value", "Status", "Source"))
                .filterColumn("Status")
                .rows(() -> rows(environment, metadata.get(), sensitiveProperties))
                .build();
    }

    /**
     * The properties the application and its libraries document, and for each of them the
     * class that declares it, when the metadata tells.
     */
    private record Metadata(Map<String, ConfigurationMetadataProperty> properties, Map<String, String> declaringTypes) {}

    private static Metadata loadMetadata(@Nullable ClassLoader classLoader) {
        ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver(classLoader).getResources(METADATA_LOCATION)) {
                try (InputStream inputStream = resource.getInputStream()) {
                    builder.withJsonResource(inputStream);
                } catch (IOException | RuntimeException ex) {
                    logger.debug("Cannot read the configuration metadata in '{}'", resource, ex);
                }
            }
        } catch (IOException ex) {
            logger.debug("Cannot look for configuration metadata", ex);
        }
        ConfigurationMetadataRepository repository = builder.build();
        Map<String, String> declaringTypes = new HashMap<>();
        repository.getAllGroups().values().forEach(group -> group.getSources().values().forEach(source -> {
            if (StringUtils.hasText(source.getType())) {
                source.getProperties().keySet().forEach(id -> declaringTypes.putIfAbsent(id, source.getType()));
            }
        }));
        return new Metadata(repository.getAllProperties(), declaringTypes);
    }

    private static List<TableRow> rows(ConfigurableEnvironment environment, Metadata metadata,
            SensitiveProperties sensitiveProperties
    ) {
        List<ConfigurationPropertySource> sources = new ArrayList<>();
        ConfigurationPropertySources.from(environment.getPropertySources()).forEach(sources::add);
        Map<String, TableRow> rows = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        metadata.properties().forEach((id, property) -> {
            ConfigurationPropertyName name = ConfigurationPropertyName.ofIfValid(id);
            ConfigurationProperty value = (name != null) ? find(name, sources) : null;
            if (value != null) {
                rows.put(id, setRow(id, property, value, metadata, environment, sensitiveProperties));
            } else if (!isRemoved(property)) {
                rows.put(id, defaultRow(id, property, metadata, sensitiveProperties));
            }
        });
        for (ConfigurationPropertySource source : sources) {
            if (source instanceof IterableConfigurationPropertySource iterableSource) {
                addUndocumentedProperties(rows, iterableSource, sources, metadata, environment, sensitiveProperties);
            }
        }
        return List.copyOf(rows.values());
    }

    /**
     * Adds the properties a source holds that the metadata doesn't document as such. One that
     * is an entry of a documented property, such as the level of a logger, is described by
     * that property.
     */
    private static void addUndocumentedProperties(Map<String, TableRow> rows, IterableConfigurationPropertySource source,
            List<ConfigurationPropertySource> sources, Metadata metadata,
            ConfigurableEnvironment environment, SensitiveProperties sensitiveProperties
    ) {
        boolean aboutConfiguration = !SOURCES_NOT_ABOUT_CONFIGURATION.contains(sourceName(source));
        try {
            source.stream().forEach(name -> {
                String id = name.toString();
                if (rows.containsKey(id)) {
                    return;
                }
                ConfigurationMetadataProperty parent = documentedParent(name, metadata);
                ConfigurationProperty value = (parent != null || aboutConfiguration) ? find(name, sources) : null;
                if (value != null) {
                    rows.put(id, setRow(id, parent, value, metadata, environment, sensitiveProperties));
                }
            });
        } catch (RuntimeException ex) {
            logger.debug("Cannot read the property source '{}'", sourceName(source), ex);
        }
    }

    private static @Nullable ConfigurationMetadataProperty documentedParent(ConfigurationPropertyName name, Metadata metadata) {
        for (ConfigurationPropertyName parent = name.getParent(); !parent.isEmpty(); parent = parent.getParent()) {
            ConfigurationMetadataProperty property = metadata.properties().get(parent.toString());
            if (property != null) {
                return property;
            }
        }
        return null;
    }

    /**
     * The value of a property, from the first source that holds it, as Spring Boot does.
     */
    private static @Nullable ConfigurationProperty find(ConfigurationPropertyName name, List<ConfigurationPropertySource> sources) {
        for (ConfigurationPropertySource source : sources) {
            try {
                ConfigurationProperty property = source.getConfigurationProperty(name);
                if (property != null) {
                    return property;
                }
            } catch (RuntimeException ex) {
                logger.debug("Cannot read the property '{}' from the source '{}'", name, sourceName(source), ex);
            }
        }
        return null;
    }

    private static TableRow setRow(String id, @Nullable ConfigurationMetadataProperty property, ConfigurationProperty value,
            Metadata metadata, ConfigurableEnvironment environment, SensitiveProperties sensitiveProperties
    ) {
        String rawValue = String.valueOf(value.getValue());
        String resolvedValue = resolve(rawValue, environment);
        String source = sourceName(value.getSource());
        PropertySource<?> propertySource = (value.getSource() != null
                && value.getSource().getUnderlyingSource() instanceof PropertySource<?> underlyingSource) ? underlyingSource : null;
        boolean sensitive = sensitiveProperties.isSensitive(propertySource, id, resolvedValue);
        List<InfoItem> details = new ArrayList<>();
        addDocumentation(details, property, metadata);
        // The default of a property with a sensitive value is as sensitive as the value.
        if (property != null && property.getDefaultValue() != null && !sensitive) {
            details.add(Details.code("Default value", "undo-2", format(property.getDefaultValue())));
        }
        // What a sensitive value is set as can hold the value itself, as the default of a placeholder.
        if (!rawValue.equals(resolvedValue) && !sensitive) {
            details.add(Details.code("Set as", "pencil", rawValue));
        }
        // Where in a file the property is set. A source that is not a file has nothing more
        // precise to say than its name.
        Origin origin = (value.getOrigin() instanceof PropertySourceOrigin sourceOrigin) ? sourceOrigin.getOrigin() : value.getOrigin();
        if (origin != null) {
            details.add(Details.text("Origin", "map-pin", origin.toString()));
        }
        addDeprecation(details, property);
        String friendlySource = EnvironmentPage.sourceName(source);
        return TableRow.builder()
                .id(id)
                .cells(List.of(TableCell.text(id),
                        sensitive ? TableCell.secret(resolvedValue) : TableCell.text(resolvedValue),
                        TableCell.text(SET),
                        TableCell.text(friendlySource)))
                .details(details)
                .build();
    }

    private static TableRow defaultRow(String id, ConfigurationMetadataProperty property, Metadata metadata,
            SensitiveProperties sensitiveProperties) {
        List<InfoItem> details = new ArrayList<>();
        addDocumentation(details, property, metadata);
        if (property.getDefaultValue() == null) {
            details.add(Details.text("Default value", "undo-2", "Not documented"));
        }
        addDeprecation(details, property);
        String defaultValue = (property.getDefaultValue() != null) ? format(property.getDefaultValue()) : "";
        return TableRow.builder()
                .id(id)
                .cells(List.of(TableCell.text(id),
                        // A property without a documented default has nothing to hide.
                        (!defaultValue.isEmpty() && sensitiveProperties.isSensitive(null, id, defaultValue)) ? TableCell.secret(defaultValue)
                                : TableCell.text(defaultValue),
                        TableCell.text(DEFAULT),
                        TableCell.text("")))
                .details(details)
                .build();
    }

    private static void addDocumentation(List<InfoItem> details, @Nullable ConfigurationMetadataProperty property, Metadata metadata) {
        if (property == null) {
            details.add(Details.text("Description", "text", "This property is not documented by the application or its libraries."));
            return;
        }
        if (StringUtils.hasText(property.getDescription())) {
            details.add(Details.text("Description", "text", property.getDescription()));
        }
        if (StringUtils.hasText(property.getType())) {
            details.add(Details.code("Type", "braces", property.getType()));
        }
        List<String> allowedValues = property.getHints().getValueHints().stream()
                .map(hint -> String.valueOf(hint.getValue()))
                .toList();
        if (!allowedValues.isEmpty()) {
            details.add(Details.code("Suggested values", "list", allowedValues));
        }
        String declaringType = metadata.declaringTypes().get(property.getId());
        if (declaringType != null) {
            details.add(Details.code("Declared in", "file-code", declaringType));
        }
    }

    private static void addDeprecation(List<InfoItem> details, @Nullable ConfigurationMetadataProperty property) {
        if (property == null || !property.isDeprecated()) {
            return;
        }
        Deprecation deprecation = property.getDeprecation();
        StringBuilder text = new StringBuilder(isRemoved(property) ? "This property is no longer supported." : "This property is deprecated.");
        if (StringUtils.hasText(deprecation.getReason())) {
            text.append(' ').append(deprecation.getReason());
        }
        if (StringUtils.hasText(deprecation.getReplacement())) {
            text.append(" Use '").append(deprecation.getReplacement()).append("' instead.");
        }
        details.add(Details.text("Deprecation", "triangle-alert", text.toString()));
    }

    /**
     * Whether the property is kept in the metadata only to tell what replaced it. Such a
     * property is listed when the application still sets it, and left out otherwise.
     */
    private static boolean isRemoved(ConfigurationMetadataProperty property) {
        return property.isDeprecated() && property.getDeprecation().getLevel() == Deprecation.Level.ERROR;
    }

    private static String resolve(String value, ConfigurableEnvironment environment) {
        try {
            return environment.resolvePlaceholders(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    /**
     * A default value as text. The default of a property with several values is an array.
     */
    private static String format(Object defaultValue) {
        if (defaultValue instanceof Object[] values) {
            return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(", "));
        }
        return String.valueOf(defaultValue);
    }

    private static String sourceName(@Nullable ConfigurationPropertySource source) {
        return (source != null && source.getUnderlyingSource() instanceof PropertySource<?> propertySource)
                ? propertySource.getName()
                : "";
    }

}
