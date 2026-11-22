package io.arconia.dev.ui.server.error;

import java.util.Map;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import io.arconia.dev.ui.server.autoconfigure.DevUiAutoConfiguration;
import io.arconia.dev.ui.server.autoconfigure.DevUiErrorPagesAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiErrorViewResolver}.
 */
class DevUiErrorViewResolverTests {

    private static final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
                    ErrorMvcAutoConfiguration.class, DevUiAutoConfiguration.class, DevUiErrorPagesAutoConfiguration.class))
            .withSystemProperties("arconia.bootstrap.mode=dev");

    @Test
    void welcomePageIsShownForTheRootPath() {
        contextRunner.run(context -> assertThat(page(context, "localhost", "/", HttpStatus.NOT_FOUND))
                .contains("<h1>Your application is running</h1>"));
    }

    @Test
    void pageIsShownFromTheFirstErrorWithLazyInitialization() {
        contextRunner
                .withInitializer(context -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
                .run(context -> assertThat(page(context, "localhost", "/", HttpStatus.NOT_FOUND))
                        .contains("<h1>Your application is running</h1>"));
    }

    @Test
    void notFoundPageIsShownForAnyOtherPath() {
        contextRunner.run(context -> assertThat(page(context, "localhost", "/books", HttpStatus.NOT_FOUND))
                .contains("<h1>Nothing here</h1>")
                .contains("<code>/books</code>"));
    }

    @Test
    void errorPageIsShownForAnyOtherStatus() {
        contextRunner.run(context -> assertThat(page(context, "localhost", "/books", HttpStatus.INTERNAL_SERVER_ERROR))
                .contains("<h1>500 Internal Server Error</h1>")
                .contains("<code>GET /books</code> failed."));
    }

    @Test
    void contextPathIsNotPartOfThePath() {
        contextRunner.run(context -> {
            MockHttpServletRequest request = request("localhost", "/shop/");
            request.setContextPath("/shop");

            assertThat(render(resolver(context).resolveErrorView(request, HttpStatus.NOT_FOUND, Map.of())))
                    .contains("<h1>Your application is running</h1>")
                    .contains("href=\"/shop/a/dev-ui/\"");
        });
    }

    @Test
    void errorPathOpenedDirectlyIsNotShownAsFailure() {
        contextRunner.run(context -> {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
            request.setServerName("localhost");

            String page = render(resolver(context).resolveErrorView(request, HttpStatus.INTERNAL_SERVER_ERROR, Map.of()));

            assertThat(page).contains("<h1>Nothing failed</h1>").contains("<code>/error</code>");
        });
    }

    @Test
    void noPageIsShownForRequestThatTheDevUiDoesNotAccept() {
        contextRunner.run(context -> assertThat(
                resolver(context).resolveErrorView(request("evil.example.com", "/"), HttpStatus.NOT_FOUND, Map.of())).isNull());
    }

    @Test
    void noPageIsShownWhenTheWhitelabelErrorPageIsTurnedOff() {
        contextRunner
                .withPropertyValues("spring.web.error.whitelabel.enabled=false")
                .run(context -> assertThat(
                        resolver(context).resolveErrorView(request("localhost", "/"), HttpStatus.NOT_FOUND, Map.of())).isNull());
    }

    @Test
    void noPageIsShownWhenTheApplicationHasItsOwnErrorView() {
        contextRunner
                .withUserConfiguration(ErrorViewConfiguration.class)
                .run(context -> assertThat(
                        resolver(context).resolveErrorView(request("localhost", "/"), HttpStatus.NOT_FOUND, Map.of())).isNull());
    }

    @Test
    void noPageIsShownWhenAnotherResolverHasPageForTheError() {
        contextRunner
                .withUserConfiguration(ErrorViewResolverConfiguration.class)
                .run(context -> {
                    DevUiErrorViewResolver resolver = resolver(context);

                    assertThat(resolver.resolveErrorView(request("localhost", "/books"), HttpStatus.NOT_FOUND, Map.of())).isNull();
                    assertThat(resolver.resolveErrorView(request("localhost", "/books"), HttpStatus.INTERNAL_SERVER_ERROR, Map.of()))
                            .isNotNull();
                });
    }

    private static String page(AssertableWebApplicationContext context, String host, String path, HttpStatus status) throws Exception {
        return render(resolver(context).resolveErrorView(request(host, path), status, Map.of()));
    }

    private static DevUiErrorViewResolver resolver(AssertableWebApplicationContext context) {
        return context.getBean(DevUiErrorViewResolver.class);
    }

    private static String render(ModelAndView modelAndView) throws Exception {
        assertThat(modelAndView).isNotNull();
        View view = modelAndView.getView();
        assertThat(view).isNotNull();
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(Map.of(), new MockHttpServletRequest(), response);
        return response.getContentAsString();
    }

    /**
     * A request as Spring Boot sees it when it handles an error: for the error path, with the
     * path that was requested as an attribute.
     */
    private static MockHttpServletRequest request(String host, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setServerName(host);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
        return request;
    }

    @Configuration(proxyBeanMethods = false)
    static class ErrorViewConfiguration {

        @Bean
        View error() {
            return (model, request, response) -> response.getWriter().write("Custom error view");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ErrorViewResolverConfiguration {

        @Bean
        ErrorViewResolver notFoundViewResolver() {
            return (request, status, model) -> (status == HttpStatus.NOT_FOUND) ? new ModelAndView("not-found") : null;
        }

    }

}
