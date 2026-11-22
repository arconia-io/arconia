package io.arconia.dev.ui.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import io.arconia.core.bootstrap.BootstrapMode;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser tests of the Dev UI frontend, in a Spring MVC application with a custom context
 * path. A real browser opens the pages the way a developer does, so what only happens in the
 * browser, such as routing and filtering, is covered too.
 */
@UsePlaywright(DevUiBrowser.class)
class DevUiFrontendIT {

    private static ConfigurableApplicationContext context;

    private static int port;

    private final List<String> errors = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void startApplication() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        context = new SpringApplicationBuilder(FrontendTestApplication.class)
                .properties("server.port=0", "server.servlet.context-path=/app")
                .run();
        port = ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    @AfterAll
    static void stopApplication() {
        context.close();
        System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
    }

    /**
     * Records what the browser reports as an error, which no page of the Dev UI should cause.
     */
    @BeforeEach
    void recordErrors(Page page) {
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                this.errors.add(message.text());
            }
        });
        page.onPageError(this.errors::add);
    }

    @AfterEach
    void noErrorsInTheBrowser() {
        assertThat(this.errors).isEmpty();
    }

    @Test
    void devUiOpensOnTheOverviewOfTheApplication(Page page) {
        page.navigate(url("/a/dev-ui/"));

        assertThat(page).hasURL(url("/a/dev-ui/pages/overview"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Overview").setLevel(1))).isVisible();
        assertThat(page).hasTitle("Overview · Application · Arconia Dev UI");
    }

    private static String url(String path) {
        return "http://localhost:" + port + "/app" + path;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class FrontendTestApplication {

    }

}
