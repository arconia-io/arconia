package io.arconia.dev.ui.server.pages;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.dev.ui.api.page.DevUiSection;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PropertiesPage}.
 */
class PropertiesPageTests {

    /**
     * Compares details by what they say. Their icons have a test of their own.
     */
    private static final Comparator<InfoItem> LABEL_AND_VALUES = Comparator.comparing(InfoItem::getLabel)
            .thenComparing(item -> String.join("\n", item.getValues()));

    private static final String METADATA = """
            {
              "groups": [
                { "name": "shop", "type": "com.example.ShopProperties", "sourceType": "com.example.ShopProperties" }
              ],
              "properties": [
                {
                  "name": "server.port",
                  "type": "java.lang.Integer",
                  "description": "Server HTTP port.",
                  "defaultValue": 8080
                },
                {
                  "name": "shop.currencies",
                  "type": "java.util.List<java.lang.String>",
                  "description": "Currencies the shop accepts.",
                  "defaultValue": [ "EUR", "USD" ]
                },
                {
                  "name": "shop.password",
                  "type": "java.lang.String",
                  "description": "Password of the shop."
                },
                {
                  "name": "shop.token",
                  "type": "java.lang.String",
                  "sourceType": "com.example.ShopProperties",
                  "description": "Token of the shop.",
                  "defaultValue": "letmein"
                },
                {
                  "name": "logging.level",
                  "type": "java.util.Map<java.lang.String,java.lang.String>",
                  "description": "Log levels severity mapping."
                },
                {
                  "name": "shop.mode",
                  "type": "java.lang.String",
                  "description": "Mode of the shop.",
                  "deprecation": { "level": "warning", "reason": "Modes are gone.", "replacement": "shop.kind" }
                },
                {
                  "name": "shop.legacy",
                  "type": "java.lang.String",
                  "deprecation": { "level": "error", "replacement": "shop.kind" }
                }
              ],
              "hints": [
                { "name": "shop.mode", "values": [ { "value": "retail" }, { "value": "wholesale" } ] }
              ]
            }
            """;

    @TempDir
    Path directory;

    private ClassLoader classLoader;

    @BeforeEach
    void writeMetadata() throws Exception {
        Files.createDirectories(this.directory.resolve("META-INF"));
        Files.writeString(this.directory.resolve("META-INF/spring-configuration-metadata.json"), METADATA);
        this.classLoader = new URLClassLoader(new URL[] { this.directory.toUri().toURL() }, null);
    }

    @Test
    void pageShowsTheConfigurationAsTable() {
        TablePage page = PropertiesPage.create(new MockEnvironment(), this.classLoader, List.of());

        assertThat(page.getId()).isEqualTo("properties");
        assertThat(page.getTitle()).isEqualTo("Properties");
        assertThat(page.getSection()).isSameAs(DevUiSection.CONFIGURATION);
        assertThat(page.getIcon()).isEqualTo("sliders-horizontal");
        assertThat(page.getColumns()).containsExactly("Property", "Value", "Status", "Source");
    }

    @Test
    void everyDocumentedPropertyIsListedWithItsDefaultValueSortedByName() {
        List<TableRow> rows = rows();

        assertThat(rows).extracting(TableRow::getId)
                .containsExactly("logging.level", "server.port", "shop.currencies", "shop.mode", "shop.password", "shop.token");
        assertThat(row(rows, "server.port").getCells()).containsExactly(TableCell.text("server.port"), TableCell.text("8080"),
                TableCell.text("Default"), TableCell.text(""));
        assertThat(row(rows, "server.port").getDetails()).usingElementComparator(LABEL_AND_VALUES).containsExactly(
                InfoItem.of("Description", "Server HTTP port."),
                InfoItem.of("Type", "java.lang.Integer"));
        assertThat(row(rows, "shop.currencies").getCells().get(1)).isEqualTo(TableCell.text("EUR, USD"));
    }

    @Test
    void propertyWithoutDocumentedDefaultSaysSo() {
        TableRow row = row(rows(), "shop.password");

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.text(""));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).contains(InfoItem.of("Default value", "Not documented"));
    }

    @Test
    void propertyThatIsSetShowsItsValueItsSourceAndItsDefault() {
        TableRow row = row(rows(new MapPropertySource("commandLineArgs", Map.of("server.port", "9090"))), "server.port");

        assertThat(row.getCells()).containsExactly(TableCell.text("server.port"), TableCell.text("9090"), TableCell.text("Set"),
                TableCell.text("commandLineArgs"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).containsExactly(
                InfoItem.of("Description", "Server HTTP port."),
                InfoItem.of("Type", "java.lang.Integer"),
                InfoItem.of("Default value", "8080"));
    }

    @Test
    void firstSourceThatHoldsThePropertyGivesItsValue() {
        TableRow row = row(rows(
                new MapPropertySource("commandLineArgs", Map.of("server.port", "9090")),
                new MapPropertySource("defaultProperties", Map.of("server.port", "7070"))), "server.port");

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.text("9090"));
    }

    @Test
    void environmentVariableSetsThePropertyItIsNamedAfter() {
        TableRow row = row(rows(new SystemEnvironmentPropertySource("systemEnvironment", Map.of("SERVER_PORT", "9090"))), "server.port");

        assertThat(row.getCells()).containsExactly(TableCell.text("server.port"), TableCell.text("9090"), TableCell.text("Set"),
                TableCell.text("systemEnvironment"));
    }

    @Test
    void detailsTellWhereInFileThePropertyIsSet() {
        TextResourceOrigin origin = new TextResourceOrigin(new ClassPathResource("application.yml"), new TextResourceOrigin.Location(2, 8));
        String sourceName = "Config resource 'class path resource [application.yml]' via location 'optional:classpath:/'";
        TableRow row = row(rows(new OriginTrackedMapPropertySource(sourceName, Map.of("server.port", OriginTrackedValue.of("9090", origin)))),
                "server.port");

        assertThat(row.getCells().get(3)).isEqualTo(TableCell.text("application.yml"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).contains(InfoItem.of("Origin", "class path resource [application.yml] - 3:9"));
    }

    @Test
    void whatSensitiveValueIsSetAsIsNotShown() {
        TableRow row = row(rows(new MapPropertySource("defaultProperties", Map.of("shop.password", "${SHOP_PASSWORD:changeme}"))),
                "shop.password");

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.secret("changeme"));
        assertThat(row.getDetails()).extracting(InfoItem::getLabel).doesNotContain("Set as");
        assertThat(row.getDetails()).flatExtracting(InfoItem::getValues).noneMatch(value -> value.contains("changeme"));
    }

    @Test
    void valueIsShownWithItsPlaceholdersResolved() {
        TableRow row = row(rows(new MapPropertySource("defaultProperties", Map.of("shop.name", "books", "shop.greeting", "Welcome to ${shop.name}"))),
                "shop.greeting");

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.text("Welcome to books"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).contains(InfoItem.of("Set as", "Welcome to ${shop.name}"));
    }

    @Test
    void valueThatIsLikelyToBeSecretIsHidden() {
        TableRow row = row(rows(new MapPropertySource("defaultProperties", Map.of("shop.password", "s3cret"))), "shop.password");

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.secret("s3cret"));
    }

    @Test
    void defaultValueThatIsLikelyToBeSecretIsHiddenToo() {
        TableRow unset = row(rows(), "shop.token");
        TableRow set = row(rows(new MapPropertySource("defaultProperties", Map.of("shop.token", "changed"))), "shop.token");

        assertThat(unset.getCells().get(1)).isEqualTo(TableCell.secret("letmein"));
        assertThat(set.getCells().get(1)).isEqualTo(TableCell.secret("changed"));
        assertThat(set.getDetails()).extracting(InfoItem::getLabel).doesNotContain("Default value");
    }

    @Test
    void sanitizingFunctionsOfTheApplicationDecideWhatIsSecretToo() {
        MockEnvironment environment = environment(new MapPropertySource("defaultProperties", Map.of("server.port", "9090")));
        List<TableRow> rows = PropertiesPage
                .create(environment, this.classLoader, List.of(SanitizingFunction.sanitizeValue().ifKeyEquals("server.port")))
                .getRows().get();

        assertThat(row(rows, "server.port").getCells().get(1)).isEqualTo(TableCell.secret("9090"));
    }

    @Test
    void propertyThatIsSetWithoutBeingDocumentedIsListed() {
        TableRow row = row(rows(new MapPropertySource("defaultProperties", Map.of("shop.greeting", "hello"))), "shop.greeting");

        assertThat(row.getCells()).containsExactly(TableCell.text("shop.greeting"), TableCell.text("hello"), TableCell.text("Set"),
                TableCell.text("defaultProperties"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).containsExactly(
                InfoItem.of("Description", "This property is not documented by the application or its libraries."));
    }

    @Test
    void entryOfDocumentedPropertyIsDescribedByIt() {
        TableRow row = row(rows(new MapPropertySource("defaultProperties", Map.of("logging.level.org.springframework.web", "DEBUG"))),
                "logging.level.org.springframework.web");

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.text("DEBUG"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).contains(InfoItem.of("Description", "Log levels severity mapping."));
    }

    @Test
    void systemPropertiesAndEnvironmentVariablesAreListedOnlyWhenTheySetDocumentedProperty() {
        List<TableRow> rows = rows(
                new MapPropertySource("systemProperties", Map.of("java.home", "/jdk", "logging.level.root", "WARN")),
                new SystemEnvironmentPropertySource("systemEnvironment", Map.of("PATH", "/usr/bin", "SERVER_PORT", "9090")));

        assertThat(rows).extracting(TableRow::getId).contains("logging.level.root", "server.port").doesNotContain("java.home", "path");
    }

    @Test
    void deprecatedPropertyTellsWhatReplacesIt() {
        TableRow row = row(rows(), "shop.mode");

        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).contains(
                InfoItem.of("Suggested values", List.of("retail", "wholesale")),
                InfoItem.of("Deprecation", "This property is deprecated. Modes are gone. Use 'shop.kind' instead."));
    }

    @Test
    void propertyThatIsNoLongerSupportedIsListedOnlyWhenItIsStillSet() {
        assertThat(rows()).extracting(TableRow::getId).doesNotContain("shop.legacy");

        TableRow row = row(rows(new MapPropertySource("defaultProperties", Map.of("shop.legacy", "yes"))), "shop.legacy");

        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).contains(
                InfoItem.of("Deprecation", "This property is no longer supported. Use 'shop.kind' instead."));
    }

    @Test
    void environmentCannotBeNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PropertiesPage.create(null, this.classLoader, List.of()))
                .withMessage("environment cannot be null");
    }

    @Test
    void detailsHaveTheirIconsAndTellWhichAreCode() {
        TableRow row = row(rows(new MapPropertySource("commandLineArgs", Map.of("server.port", "9090"))), "server.port");

        assertThat(row.getDetails()).containsExactly(
                InfoItem.builder().label("Description").value("Server HTTP port.").icon("text").build(),
                InfoItem.builder().label("Type").value("java.lang.Integer").icon("braces").code(true).build(),
                InfoItem.builder().label("Default value").value("8080").icon("undo-2").code(true).build());
    }

    @Test
    void detailsTellTheClassThatDeclaresThePropertyWhenTheMetadataDoes() {
        List<TableRow> rows = rows();

        assertThat(row(rows, "shop.token").getDetails())
                .contains(InfoItem.builder().label("Declared in").value("com.example.ShopProperties").icon("file-code").code(true).build());
        assertThat(row(rows, "server.port").getDetails()).extracting(InfoItem::getLabel).doesNotContain("Declared in");
    }

    private List<TableRow> rows(PropertySource<?>... propertySources) {
        return PropertiesPage.create(environment(propertySources), this.classLoader, List.of()).getRows().get();
    }

    private static MockEnvironment environment(PropertySource<?>... propertySources) {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().forEach(source -> environment.getPropertySources().remove(source.getName()));
        for (PropertySource<?> propertySource : propertySources) {
            environment.getPropertySources().addLast(propertySource);
        }
        return environment;
    }

    private static TableRow row(List<TableRow> rows, String id) {
        return rows.stream().filter(row -> row.getId().equals(id)).findFirst().orElseThrow();
    }

}
