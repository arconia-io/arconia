package io.arconia.dev.ui.server.autoconfigure;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;

import io.arconia.dev.ui.server.web.DevUiHosts;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiAutoConfiguration}.
 */
class DevUiAutoConfigurationTests {

    private static final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevUiAutoConfiguration.class));

    @Test
    void devUiIsAvailableWhenDevModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasBean("devUiServlet");
                    assertThat(context).hasBean("devUiFilter");
                    assertThat(context).hasBean("devUiPropertiesPage");
                    assertThat(context).hasBean("devUiEnvironmentPage");
                });
    }

    @Test
    void devUiIsNotAvailableWhenTestModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiAutoConfiguration.class));
    }

    @Test
    void devUiIsNotAvailableWhenProdModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=prod")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiAutoConfiguration.class));
    }

    @Test
    void devUiIsNotAvailableWhenDisabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.ui.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiAutoConfiguration.class));
    }

    @Test
    void devUiIsNotAvailableWhenNotWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DevUiAutoConfiguration.class))
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context).doesNotHaveBean(DevUiAutoConfiguration.class));
    }

    @Test
    void servletIsMappedToTheDevUiPath() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    ServletRegistrationBean<?> registration = context.getBean("devUiServlet", ServletRegistrationBean.class);
                    assertThat(registration.getUrlMappings()).containsExactly("/a/dev-ui/*");
                    assertThat(registration).extracting("loadOnStartup").isEqualTo(1);
                });
    }

    @Test
    void filterCoversTheDevUiPathAheadOfTheOtherFilters() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    FilterRegistrationBean<?> registration = context.getBean("devUiFilter", FilterRegistrationBean.class);
                    assertThat(registration.getUrlPatterns()).containsExactly("/a/dev-ui/*");
                    assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
                });
    }

    @Test
    void allowedHostsAreBound() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.ui.allowed-hosts=dev.example.com,workspace.example.com")
                .run(context -> assertThat(context.getBean(DevUiProperties.class).getAllowedHosts())
                        .containsExactly("dev.example.com", "workspace.example.com"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            '',     /a/dev-ui,                    false
            '',     /a/dev-ui/,                   false
            '',     /a/dev-ui/api/pages,          false
            /app,   /app/a/dev-ui/static/app.js,  false
            '',     /a/dev-ui-of-the-application, true
            '',     /books,                       true
            /app,   /app/books,                   true
            """)
    void requestsToTheDevUiAreNotObserved(String contextPath, String uri, boolean observed) {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
                    request.setContextPath(contextPath);
                    var observationContext = new ServerRequestObservationContext(request, new MockHttpServletResponse());

                    assertThat(context.getBean(ObservationPredicate.class).test("http.server.requests", observationContext))
                            .isEqualTo(observed);
                });
    }

    @Test
    void otherObservationsAreNotAffected() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context.getBean(ObservationPredicate.class).test("books.find", new Observation.Context()))
                        .isTrue());
    }

    @Test
    void requestsToTheDevUiAreObservedWhenEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.ui.observations.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(ObservationPredicate.class));
    }

    @Test
    void sanitizingFunctionsOfTheApplicationHideValuesOnTheEnvironmentPage() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("app.customer-number=12345")
                .withBean(SanitizingFunction.class, () -> SanitizingFunction.sanitizeValue().ifKeyEquals("app.customer-number"))
                .run(context -> assertThat(context.getBean("devUiEnvironmentPage", TablePage.class).getRows().get())
                        .filteredOn(row -> row.getCells().getFirst().getText().equals("app.customer-number"))
                        .singleElement()
                        .satisfies(row -> assertThat(row.getCells().get(1)).isEqualTo(TableCell.secret("12345"))));
    }

    @Test
    void requestsFromOtherMachinesAreNotAcceptedUnlessRemoteAccessIsEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a/dev-ui/");
        request.setRemoteAddr("172.17.0.1");
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context.getBean(DevUiHosts.class).accepts(request)).isFalse());
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.ui.remote-access.enabled=true")
                .run(context -> assertThat(context.getBean(DevUiHosts.class).accepts(request)).isTrue());
    }

}
