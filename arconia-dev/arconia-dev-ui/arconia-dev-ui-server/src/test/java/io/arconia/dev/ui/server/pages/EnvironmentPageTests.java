package io.arconia.dev.ui.server.pages;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link EnvironmentPage}.
 */
class EnvironmentPageTests {

    /**
     * Compares details by what they say. Their icons have a test of their own.
     */
    private static final Comparator<InfoItem> LABEL_AND_VALUES = Comparator.comparing(InfoItem::getLabel)
            .thenComparing(item -> String.join("\n", item.getValues()));

    @Test
    void pageShowsTheEnvironmentAsTable() {
        TablePage page = EnvironmentPage.create(new MockEnvironment(), List.of());

        assertThat(page.getId()).isEqualTo("environment");
        assertThat(page.getTitle()).isEqualTo("Environment");
        assertThat(page.getIcon()).isEqualTo("variable");
        assertThat(page.getColumns()).containsExactly("Property", "Value", "Status", "Source");
    }

    @Test
    void everyPropertyOfEverySourceIsListedInTheOrderSpringLooksThemUp() {
        List<TableRow> rows = rows(
                new MapPropertySource("commandLineArgs", Map.of("server.port", "9090")),
                new MapPropertySource("defaultProperties", Map.of("server.port", "8080", "app.greeting", "hello")));

        assertThat(rows).extracting(TableRow::getId).containsExactly("1:server.port", "2:app.greeting", "2:server.port");
        assertThat(rows.get(0).getCells()).containsExactly(TableCell.text("server.port"), TableCell.text("9090"),
                TableCell.text("Effective"), TableCell.text("commandLineArgs"));
        assertThat(rows.get(1).getCells()).containsExactly(TableCell.text("app.greeting"), TableCell.text("hello"),
                TableCell.text("Effective"), TableCell.text("defaultProperties"));
    }

    @Test
    void detailsHaveTheirIcons() {
        TableRow row = rows(
                new MapPropertySource("commandLineArgs", Map.of("server.port", "9090")),
                new MapPropertySource("defaultProperties", Map.of("server.port", "8080"))).get(1);

        assertThat(row.getDetails()).containsExactly(
                InfoItem.builder().label("Overridden by").value("commandLineArgs").icon("arrow-up-to-line").build());
    }

    @Test
    void propertyThatAnEnvironmentVariableSetsUnderItsOwnNameIsOverridden() {
        TableRow row = rows(
                new SystemEnvironmentPropertySource("systemEnvironment", Map.of("SERVER_PORT", "9090")),
                new MapPropertySource("defaultProperties", Map.of("server.port", "8080"))).get(1);

        assertThat(row.getCells()).containsExactly(TableCell.text("server.port"), TableCell.text("8080"),
                TableCell.text("Overridden"), TableCell.text("defaultProperties"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES)
                .containsExactly(InfoItem.of("Overridden by", "systemEnvironment"));
    }

    @Test
    void propertyThatAnEarlierSourceHoldsIsOverridden() {
        TableRow row = rows(
                new MapPropertySource("commandLineArgs", Map.of("server.port", "9090")),
                new MapPropertySource("defaultProperties", Map.of("server.port", "8080"))).get(1);

        assertThat(row.getCells()).containsExactly(TableCell.text("server.port"), TableCell.text("8080"),
                TableCell.text("Overridden"), TableCell.text("defaultProperties"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).containsExactly(
                InfoItem.of("Overridden by", "commandLineArgs"));
    }

    @Test
    void valueOfPropertyThatIsLikelyToHoldSecretIsHidden() {
        TableRow row = rows(new MapPropertySource("vault", Map.of("spring.datasource.password", "s3cret"))).getFirst();

        assertThat(row.getCells().get(1)).isEqualTo(TableCell.secret("s3cret"));
    }

    @Test
    void detailsTellWhereInFileThePropertyIsSet() {
        TextResourceOrigin origin = new TextResourceOrigin(new ClassPathResource("application.yml"), new TextResourceOrigin.Location(2, 8));
        String sourceName = "Config resource 'class path resource [application.yml]' via location 'optional:classpath:/'";
        TableRow row = rows(new OriginTrackedMapPropertySource(sourceName, Map.of("server.port", OriginTrackedValue.of("8080", origin))))
                .getFirst();

        assertThat(row.getCells().get(3)).isEqualTo(TableCell.text("application.yml"));
        assertThat(row.getCells().get(1)).isEqualTo(TableCell.text("8080"));
        assertThat(row.getDetails()).usingElementComparator(LABEL_AND_VALUES).containsExactly(
                InfoItem.of("Source", sourceName),
                InfoItem.of("Origin", "class path resource [application.yml] - 3:9"));
    }

    @Test
    void rowWithNothingMoreToSayHasNoDetails() {
        assertThat(rows(new MapPropertySource("vault", Map.of("app.greeting", "hello"))).getFirst().getDetails()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            systemProperties | systemProperties
            systemEnvironment | systemEnvironment
            commandLineArgs | commandLineArgs
            Config resource 'class path resource [application.yml]' via location 'optional:classpath:/' | application.yml
            Config resource 'class path resource [config/application-dev.properties]' via location 'optional:classpath:/config/' | application-dev.properties
            Config resource 'file [/workspace/app/application.yml]' via location 'optional:file:./' (document #1) | application.yml (document #1)
            Dev Services | Dev Services
            """)
    void sourceKeepsItsNameUnlessItIsConfigurationFile(String name, String expectedName) {
        assertThat(EnvironmentPage.sourceName(name)).isEqualTo(expectedName);
    }

    @Test
    void environmentCannotBeNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EnvironmentPage.create(null, List.of()))
                .withMessage("environment cannot be null");
    }

    private static List<TableRow> rows(PropertySource<?>... propertySources) {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().forEach(source -> environment.getPropertySources().remove(source.getName()));
        for (PropertySource<?> propertySource : propertySources) {
            environment.getPropertySources().addLast(propertySource);
        }
        return EnvironmentPage.create(environment, List.of()).getRows().get();
    }

}
