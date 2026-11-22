package io.arconia.dev.ui.server.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.api.page.DevUiSection;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;

/**
 * The page of the Dev UI that shows the environment of the application: the sources its
 * properties come from, in the order Spring looks them up, with every property each of them
 * holds. A property that several sources hold gets its value from the first one, and the page
 * tells which ones are overridden.
 */
@Internal
public final class EnvironmentPage {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentPage.class);

    /**
     * The name Spring Boot gives to the source of a configuration file, which carries the
     * description of the file between quotes and, for a file with several documents, the
     * number of the document.
     */
    private static final Pattern CONFIG_FILE_SOURCE = Pattern.compile("Config resource '(.+?)' via location '.*?'( \\(document #\\d+\\))?");

    private static final Pattern RESOURCE_PATH = Pattern.compile(".*\\[(.+)]");

    private EnvironmentPage() {}

    /**
     * Creates the page for the given environment. The sanitizing functions are the ones the
     * application defines to say which values are sensitive.
     */
    public static TablePage create(ConfigurableEnvironment environment, Collection<SanitizingFunction> sanitizingFunctions) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(sanitizingFunctions, "sanitizingFunctions cannot be null");
        SensitiveProperties sensitiveProperties = new SensitiveProperties(sanitizingFunctions);
        return TablePage.builder()
                .id("environment")
                .title("Environment")
                .icon("variable")
                .section(DevUiSection.CONFIGURATION)
                .order(-90)
                .columns(List.of("Property", "Value", "Status", "Source"))
                .filterColumn("Source")
                .rows(() -> rows(environment, sensitiveProperties))
                .build();
    }

    private static List<TableRow> rows(ConfigurableEnvironment environment, SensitiveProperties sensitiveProperties) {
        List<TableRow> rows = new ArrayList<>();
        List<PropertySource<?>> earlierSources = new ArrayList<>();
        int position = 0;
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            position++;
            // The source Spring Boot adds to read all the others in its own way holds nothing itself.
            if (ConfigurationPropertySources.isAttachedConfigurationPropertySource(propertySource)) {
                continue;
            }
            if (propertySource instanceof EnumerablePropertySource<?> enumerableSource) {
                try {
                    addRows(rows, enumerableSource, position, earlierSources, sensitiveProperties);
                } catch (RuntimeException ex) {
                    logger.debug("Cannot read the property source '{}'", propertySource.getName(), ex);
                }
            }
            earlierSources.add(propertySource);
        }
        return rows;
    }

    private static void addRows(List<TableRow> rows, EnumerablePropertySource<?> propertySource, int position,
            List<PropertySource<?>> earlierSources, SensitiveProperties sensitiveProperties
    ) {
        String source = sourceName(propertySource.getName());
        String[] names = propertySource.getPropertyNames().clone();
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            String value = String.valueOf(propertySource.getProperty(name));
            String effectiveSource = overridingSource(name, earlierSources);
            List<InfoItem> details = new ArrayList<>();
            if (!source.equals(propertySource.getName())) {
                details.add(Details.text("Source", "layers", propertySource.getName()));
            }
            Origin origin = OriginLookup.getOrigin(propertySource, name);
            if (origin != null) {
                details.add(Details.text("Origin", "map-pin", origin.toString()));
            }
            if (effectiveSource != null) {
                details.add(Details.text("Overridden by", "arrow-up-to-line", effectiveSource));
            }
            rows.add(TableRow.builder()
                    .id(position + ":" + name)
                    .cells(List.of(TableCell.text(name),
                            sensitiveProperties.isSensitive(propertySource, name, value) ? TableCell.secret(value) : TableCell.text(value),
                            TableCell.text((effectiveSource == null) ? "Effective" : "Overridden"),
                            TableCell.text(source)))
                    .details(details)
                    .build());
        }
    }

    /**
     * The earlier source that holds the property with the given name, if any, which is then
     * the one Spring takes its value from. Each source is asked in its own way, so that an
     * environment variable named {@code SERVER_PORT} overrides {@code server.port}.
     */
    private static @Nullable String overridingSource(String name, List<PropertySource<?>> earlierSources) {
        for (PropertySource<?> earlierSource : earlierSources) {
            try {
                if (earlierSource.containsProperty(name)) {
                    return sourceName(earlierSource.getName());
                }
            } catch (RuntimeException ex) {
                logger.debug("Cannot read the property source '{}'", earlierSource.getName(), ex);
            }
        }
        return null;
    }

    /**
     * The name to show for a source: the one Spring gives it, which is also the one its
     * documentation uses. The exception is a configuration file, whose name is a sentence
     * about where it was found, and which is shown as the name of the file.
     */
    static String sourceName(String name) {
        Matcher configFile = CONFIG_FILE_SOURCE.matcher(name);
        if (!configFile.matches()) {
            return name;
        }
        String resource = configFile.group(1);
        Matcher path = RESOURCE_PATH.matcher(resource);
        String file = path.matches() ? path.group(1).substring(path.group(1).lastIndexOf('/') + 1) : resource;
        return file + ((configFile.group(2) != null) ? configFile.group(2) : "");
    }

}
