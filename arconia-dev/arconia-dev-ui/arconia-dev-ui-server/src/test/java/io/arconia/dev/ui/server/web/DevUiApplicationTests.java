package io.arconia.dev.ui.server.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;
import io.arconia.dev.ui.server.web.DevUiApplication.Fact;
import io.arconia.dev.ui.server.web.DevUiApplication.Overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link DevUiApplication}.
 */
class DevUiApplicationTests {

    @Test
    void nameIsTheOneTheApplicationSets() {
        assertThat(application(CatalogApplication.class, "spring.application.name=catalog-service").name()).isEqualTo("catalog-service");
    }

    @Test
    void applicationWithoutNameIsKnownByItsMainClass() {
        assertThat(application(CatalogApplication.class).name()).isEqualTo("CatalogApplication");
    }

    @Test
    void applicationWithoutNameAndMainClassStillHasName() {
        assertThat(application(null).name()).isEqualTo("Application");
    }

    @Test
    void versionIsTheOneTheApplicationSets() {
        assertThat(application(BuiltApplication.class, "spring.application.version=2.0.0").version()).isEqualTo("2.0.0");
    }

    @Test
    void versionComesFromTheBuildOtherwise() {
        assertThat(application(BuiltApplication.class).version()).isEqualTo("1.4.0");
    }

    @Test
    void versionIsNotKnownWithoutEither() {
        assertThat(application(CatalogApplication.class).version()).isNull();
    }

    /**
     * A point in time stays true for as long as the page is open, which a duration would not.
     */
    @Test
    void startTimeIsTimeOfDayWithTheDateWhenItWasNotToday() {
        Instant start = LocalDateTime.of(2026, 11, 21, 9, 5).atZone(ZoneId.systemDefault()).toInstant();

        assertThat(DevUiApplication.startTime(start, LocalDate.of(2026, 11, 21))).isEqualTo("09:05");
        assertThat(DevUiApplication.startTime(start, LocalDate.of(2026, 11, 22))).isEqualTo("Nov 21, 09:05");
    }

    @Test
    void overviewShowsTheFactsWorthSeeingAtGlance() {
        Overview overview = application(CatalogApplication.class, "spring.application.name=catalog-service", "spring.profiles.active=dev,local")
                .overview(request());

        assertThat(overview.name()).isEqualTo("catalog-service");
        assertThat(overview.version()).isNull();
        assertThat(overview.startedAt()).matches("\\d{2}:\\d{2}");
        assertThat(overview.facts()).containsExactly(
                new Fact("Spring Boot", SpringBootVersion.getVersion()),
                new Fact("Java", System.getProperty("java.version")),
                new Fact("Port", "8080"),
                new Fact("Profiles", "dev, local"));
    }

    @Test
    void overviewDescribesTheApplicationItsFrameworkAndWhereItRuns() {
        Overview overview = application(CatalogApplication.class).overview(request());

        assertThat(overview.cards()).extracting(Card::getId).containsExactly("application", "spring", "java", "system");
        assertThat(overview.cards().get(0).getItems()).containsExactly(
                InfoItem.of("Name", "CatalogApplication"),
                InfoItem.of("Main class", CatalogApplication.class.getName()));
        assertThat(overview.cards().get(1).getItems()).contains(
                InfoItem.of("Spring Boot", SpringBootVersion.getVersion()),
                InfoItem.of("Mode", "Development"),
                InfoItem.of("Profiles", "None"),
                InfoItem.of("Web server", "MockServletContext"),
                InfoItem.of("Port", "8080"),
                InfoItem.of("Context path", "/shop"),
                InfoItem.of("Virtual threads", "Disabled"),
                LinkItem.of("Arconia documentation", "https://docs.arconia.io"));
        assertThat(overview.cards().get(2).getItems()).extracting("label").containsExactly("Version", "Vendor", "Runtime", "Virtual machine");
        assertThat(overview.cards().get(3).getItems()).extracting("label")
                .containsExactly("Operating system", "Architecture", "Processors", "Process ID", "Heap memory");
    }

    @Test
    void overviewShowsWhatTheBuildProvides() {
        Card card = application(BuiltApplication.class).overview(request()).cards().getFirst();

        assertThat(card.getItems()).contains(
                InfoItem.of("Version", "1.4.0"),
                InfoItem.of("Group", "com.example"),
                InfoItem.of("Artifact", "catalog"),
                InfoItem.of("Branch", "main"),
                InfoItem.of("Commit", "abc1234"));
    }

    @Test
    void applicationContextCannotBeNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevUiApplication(null))
                .withMessage("applicationContext cannot be null");
    }

    private static DevUiApplication application(Class<?> mainClass, String... properties) {
        GenericApplicationContext applicationContext = (mainClass != null) ? new AnnotationConfigApplicationContext() : new GenericApplicationContext();
        TestPropertyValues.of(properties).applyTo(applicationContext);
        if (applicationContext instanceof AnnotationConfigApplicationContext annotationContext) {
            annotationContext.register(mainClass);
        }
        applicationContext.refresh();
        return new DevUiApplication(applicationContext);
    }

    private static MockHttpServletRequest request() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/shop/a/dev-ui/api/pages/overview");
        request.setContextPath("/shop");
        request.setLocalPort(8080);
        return request;
    }

    @SpringBootConfiguration
    static class CatalogApplication {}

    @SpringBootConfiguration
    static class BuiltApplication {

        @Bean
        BuildProperties buildProperties() {
            Properties properties = new Properties();
            properties.setProperty("version", "1.4.0");
            properties.setProperty("group", "com.example");
            properties.setProperty("artifact", "catalog");
            return new BuildProperties(properties);
        }

        @Bean
        GitProperties gitProperties() {
            Properties properties = new Properties();
            properties.setProperty("branch", "main");
            properties.setProperty("commit.id.abbrev", "abc1234");
            return new GitProperties(properties);
        }

    }

}
