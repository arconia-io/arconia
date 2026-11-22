package io.arconia.dev.ui.server.error;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.arconia.dev.ui.server.error.ApplicationEndpoints.Endpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApplicationEndpoints}.
 */
class ApplicationEndpointsTests {

    @Test
    void endpointsOfTheApplicationAreFoundAndSortedByPath() {
        try (AnnotationConfigWebApplicationContext context = context(true)) {
            assertThat(ApplicationEndpoints.find(context)).containsExactly(
                    new Endpoint("GET", "/books"),
                    new Endpoint("POST", "/books"),
                    new Endpoint("GET", "/books/{id}"),
                    new Endpoint("", "/status"));
        }
    }

    @Test
    void endpointsOfSpringAreLeftOutWhenThePackagesOfTheApplicationAreNotKnown() {
        try (AnnotationConfigWebApplicationContext context = context(false)) {
            assertThat(ApplicationEndpoints.find(context)).extracting(Endpoint::path)
                    .containsExactly("/books", "/books", "/books/{id}", "/status");
        }
    }

    @ParameterizedTest
    @CsvSource({ "GET, /books, true", "'', /status, true", "'GET, POST', /books, true", "POST, /books, false",
            "GET, /books/{id}, false", "GET, /files/**, false" })
    void endpointCanBeOpenedWhenItAnswersToGetAndItsPathHasNoVariable(String methods, String path, boolean canBeOpened) {
        assertThat(new Endpoint(methods, path).canBeOpened()).isEqualTo(canBeOpened);
    }

    private static AnnotationConfigWebApplicationContext context(boolean withApplicationPackages) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(BookController.class, RequestMappingHandlerMapping.class, SpringControllerConfiguration.class);
        if (withApplicationPackages) {
            context.addBeanFactoryPostProcessor(beanFactory -> AutoConfigurationPackages.register(
                    (BeanDefinitionRegistry) beanFactory, BookController.class.getPackageName()));
        }
        context.refresh();
        return context;
    }

    /**
     * A controller that Spring Boot brings along, which is not an endpoint of the application.
     */
    @Configuration(proxyBeanMethods = false)
    static class SpringControllerConfiguration {

        @Bean
        BasicErrorController basicErrorController() {
            return new BasicErrorController(new DefaultErrorAttributes(), new ErrorProperties());
        }

    }

    @RestController
    static class BookController {

        @GetMapping("/books")
        List<String> books() {
            return List.of();
        }

        @PostMapping("/books")
        void addBook() {
        }

        @GetMapping("/books/{id}")
        String book() {
            return "";
        }

        @RequestMapping("/status")
        String status() {
            return "";
        }

    }

}
