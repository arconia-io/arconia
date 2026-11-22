package io.arconia.dev.ui.server.frontend;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import io.arconia.core.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The base of the frontend tests: a real browser opens the Dev UI of a running application the
 * way a developer does, so that what only happens in the browser, such as routing and
 * filtering, is covered too.
 * <p>
 * Each test class gets its own application, with the pages of {@link FrontendTestApplication},
 * served under a context path. Each test gets its own page, and fails when the browser reports
 * an error, which no page of the Dev UI should cause.
 */
@UsePlaywright(DevUiBrowser.class)
abstract class BaseFrontendIT {

    private static ConfigurableApplicationContext context;

    private static int port;

    private final List<String> errors = new CopyOnWriteArrayList<>();

    /**
     * The Dev UI only starts in dev mode. The mode is set while the application starts and
     * cleared right after, so that it doesn't reach the other tests that run in the same JVM.
     */
    @BeforeAll
    static void startApplication() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        try {
            context = new SpringApplicationBuilder(FrontendTestApplication.class)
                    .properties("server.port=0", "server.servlet.context-path=/app")
                    .run();
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
        }
        port = ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    @AfterAll
    static void stopApplication() {
        context.close();
    }

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

    /**
     * A bean of the application, for a test to change what a page shows.
     */
    static <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    /**
     * The address of a path of the Dev UI, as the browser opens it.
     */
    static String devUi(String path) {
        return "http://localhost:" + port + "/app/a/dev-ui" + path;
    }

    /**
     * The address of a page of the Dev UI, as the browser opens it.
     */
    static String pageUrl(String pageId) {
        return devUi("/pages/" + pageId);
    }

}
