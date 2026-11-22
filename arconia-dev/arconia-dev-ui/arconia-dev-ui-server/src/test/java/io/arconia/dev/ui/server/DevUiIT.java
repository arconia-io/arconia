package io.arconia.dev.ui.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.arconia.core.bootstrap.BootstrapMode;
import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LogItem;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Dev UI on a Servlet web server, in a Spring MVC application
 * with its own static resources and a custom context path, secured with Spring Security.
 */
class DevUiIT {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String SECURITY_USER = "spring.security.user.name=user";

    private static final String SECURITY_PASSWORD = "spring.security.user.password=secret";

    private static final String CREDENTIALS = "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));

    private static final BasicJsonTester json = new BasicJsonTester(DevUiIT.class);

    private static ConfigurableApplicationContext context;

    private static int port;

    @BeforeAll
    static void startApplication() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        context = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "server.servlet.context-path=/app", SECURITY_USER, SECURITY_PASSWORD)
                .run();
        port = ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    @AfterAll
    static void stopApplication() {
        context.close();
        System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "localhost", "127.0.0.1" })
    void shellIsServedUnderTheContextPath(String host) throws Exception {
        HttpResponse<String> response = get(host, "/app/a/dev-ui/", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/html;charset=UTF-8");
        assertThat(response.body()).contains("<title>TestApplication · Arconia Dev UI</title>");
    }

    @Test
    void shellIsServedForPage() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/pages/greetings", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<title>TestApplication · Arconia Dev UI</title>");
    }

    @Test
    void requestWithoutTrailingSlashIsRedirected() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui", null);

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).endsWith("/app/a/dev-ui/"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/app/a/dev-ui/static/app/app.js", "/app/a/dev-ui/_static/lit/3.3.3/index.js" })
    void scriptIsServedForTheBrowserToLoadAsModule(String path) throws Exception {
        HttpResponse<String> response = get("localhost", path, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/javascript;charset=UTF-8");
    }

    @Test
    void requestFromTheDevUiItselfIsAccepted() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/", "http://localhost:" + port);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/app/a/dev-ui", "/app/a/dev-ui/", "/app/a/dev-ui/unknown" })
    void requestFromAnotherWebsiteIsRejected(String path) throws Exception {
        HttpResponse<String> response = get("localhost", path, "https://evil.example.com");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).isEqualTo("The Dev UI only accepts requests for the local machine");
    }

    @Test
    void unknownAddressIsAnsweredByTheDevUiItself() throws Exception {
        HttpResponse<String> page = get("localhost", "/app/a/dev-ui/unknown", null);
        HttpResponse<String> file = get("localhost", "/app/a/dev-ui/unknown.js", null);

        assertThat(page.statusCode()).isEqualTo(404);
        assertThat(page.body()).contains("<arconia-dev-ui");
        assertThat(file.statusCode()).isEqualTo(404);
        assertThat(file.body()).isEqualTo("Not Found");
    }

    @Test
    void unsupportedMethodIsNotAllowed() throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/app/a/dev-ui"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Arconia-Dev-UI", "1");

        HttpResponse<String> response = send(request, "http://localhost:" + port);

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow")).hasValue("GET, HEAD");
        assertThat(response.body()).isEqualTo("Method Not Allowed");
    }

    @Test
    void contributedPageIsListedByTheApi() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(json.from(response.body())).isStrictlyEqualToJson("""
                {
                  "navigation": [
                    { "page": { "id": "overview", "title": "Overview", "type": "overview", "icon": "layout-dashboard" } },
                    {
                      "section": { "id": "configuration", "title": "Configuration", "icon": "settings" },
                      "pages": [
                        { "id": "properties", "title": "Properties", "type": "table", "icon": "sliders-horizontal" },
                        { "id": "environment", "title": "Environment", "type": "table", "icon": "variable" }
                      ]
                    },
                    { "page": { "id": "greetings", "title": "Greetings", "type": "cards", "icon": "layout-grid" } }
                  ]
                }
                """);
    }

    @Test
    void overviewOfTheApplicationIsServedByTheApi() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages/overview", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.from(response.body())).extractingJsonPathStringValue("$.application.name").isEqualTo("TestApplication");
        assertThat(json.from(response.body())).extractingJsonPathArrayValue("$.application.facts[*].value").contains(String.valueOf(port));
        assertThat(json.from(response.body())).extractingJsonPathArrayValue("$.cards[1].items[?(@.label == 'Context path')].values[0]")
                .containsExactly("/app");
    }

    @Test
    void configurationOfTheApplicationIsServedByTheApi() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages/properties", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.from(response.body()))
                .extractingJsonPathArrayValue("$.table.rows[?(@.id == 'server.servlet.context-path')].cells[*].text")
                .containsExactly("server.servlet.context-path", "/app", "Set", "defaultProperties");
        assertThat(json.from(response.body()))
                .extractingJsonPathArrayValue("$.table.rows[?(@.id == 'server.tomcat.threads.max')].cells[*].text")
                .containsExactly("server.tomcat.threads.max", "200", "Default", "");
        assertThat(json.from(response.body()))
                .extractingJsonPathArrayValue("$.table.rows[?(@.id == 'server.tomcat.threads.max')].details[0].values[0]")
                .containsExactly("Maximum amount of worker threads. Doesn't have an effect if virtual threads are enabled.");
    }

    @Test
    void environmentOfTheApplicationIsServedByTheApi() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages/environment", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.from(response.body())).extractingJsonPathArrayValue("$.table.columns")
                .containsExactly("Property", "Value", "Status", "Source");
        assertThat(json.from(response.body()))
                .extractingJsonPathArrayValue("$.table.rows[?(@.cells[0].text == 'spring.security.user.password')].cells[1].type")
                .containsExactly("secret");
        assertThat(json.from(response.body()))
                .extractingJsonPathArrayValue("$.table.rows[?(@.cells[0].text == 'server.servlet.context-path')].cells[1].text")
                .containsExactly("/app");
    }

    @Test
    void pageNamesTheApplicationItBelongsTo() throws Exception {
        assertThat(get("localhost", "/app/a/dev-ui/", null).body())
                .contains("<title>TestApplication · Arconia Dev UI</title>")
                .contains("application-name=\"TestApplication\"");
    }

    @Test
    void contributedPageIsServedByTheApi() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages/greetings", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.from(response.body())).isStrictlyEqualToJson("""
                {
                  "id": "greetings",
                  "title": "Greetings",
                  "type": "cards",
                  "icon": "layout-grid",
                  "columns": 3,
                  "cards": [
                    {
                      "id": "hello",
                      "title": "Hello",
                      "items": [
                        { "type": "info", "label": "Language", "values": [ "English" ] },
                        { "type": "log", "label": "Logs" },
                        { "type": "log", "label": "Endless" }
                      ]
                    }
                  ]
                }
                """);
    }

    @Test
    void logIsStreamedUntilItEnds() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages/greetings/logs?card=hello&item=Logs", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/event-stream;charset=UTF-8");
        assertThat(response.body()).isEqualTo("""
                data: Hello, world

                event: end
                data:

                """);
    }

    @Test
    void requestThatChangesSomethingIsOnlyAcceptedFromTheDevUiItself() throws Exception {
        URI restart = URI.create("http://localhost:" + port + "/app/a/dev-ui/api/actions/restart");
        HttpRequest.Builder fromTheDevUi = HttpRequest.newBuilder(restart).POST(HttpRequest.BodyPublishers.noBody())
                .header("Arconia-Dev-UI", "1");
        HttpRequest.Builder withoutHeader = HttpRequest.newBuilder(restart).POST(HttpRequest.BodyPublishers.noBody());
        HttpRequest.Builder fromAnotherWebsite = HttpRequest.newBuilder(restart).POST(HttpRequest.BodyPublishers.noBody())
                .header("Arconia-Dev-UI", "1");

        // Spring Boot DevTools is not there in this test, so an accepted request is answered that it cannot be done.
        assertThat(send(fromTheDevUi, "http://localhost:" + port).statusCode()).isEqualTo(409);
        assertThat(send(withoutHeader, "http://localhost:" + port).statusCode()).isEqualTo(403);
        assertThat(send(fromAnotherWebsite, "https://evil.example.com").statusCode()).isEqualTo(403);
    }

    /**
     * A page of another website, or of another application on the local machine, cannot add
     * the header of the Dev UI without asking first, and gets no permission to.
     */
    @Test
    void browserAskingWhetherAnotherWebsiteMayAddTheHeaderOfTheDevUiGetsNoPermission() throws Exception {
        HttpRequest.Builder preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/app/a/dev-ui/api/actions/restart"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "arconia-dev-ui");

        HttpResponse<String> response = send(preflight, "http://localhost:3000");

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        assertThat(response.headers().firstValue("Access-Control-Allow-Headers")).isEmpty();
    }

    @Test
    void logsOfTheApplicationAreStreamedFromItsStart() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/app/a/dev-ui/api/logs")).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            assertThat(response.headers().firstValue("Content-Type")).hasValue("text/event-stream;charset=UTF-8");
            String firstLine = reader.readLine();
            assertThat(firstLine).startsWith("data: {\"time\":").contains("\"level\":\"INFO\"").contains("\"marker\":false");
        }
        assertThat(get("localhost", "/app/a/dev-ui/", null).body()).contains("application-logs=\"true\"");
    }

    @Test
    void openLogDoesNotDelayTheShutdownOfTheApplication() throws Exception {
        ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "server.shutdown=graceful")
                .run();
        int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + otherPort + "/a/dev-ui/api/pages/greetings/logs?card=hello&item=Endless")).build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            assertThat(reader.readLine()).isEqualTo("data: still running");

            long start = System.nanoTime();
            otherContext.close();

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        }
    }

    @Test
    void apiErrorIsNotHandledByTheApplication() throws Exception {
        HttpResponse<String> response = get("localhost", "/app/a/dev-ui/api/pages/unknown", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json.from(response.body())).isStrictlyEqualToJson("""
                { "error": { "status": 404, "message": "The page 'unknown' doesn't exist" } }
                """);
    }

    @Test
    void applicationIsSecuredAndTheDevUiIsNot() throws Exception {
        assertThat(get("localhost", "/app/application.txt", null).statusCode()).isEqualTo(401);
        assertThat(get("localhost", "/app/a/dev-ui/", null).statusCode()).isEqualTo(200);
        assertThat(get("localhost", "/app/a/dev-ui/api/pages", null).statusCode()).isEqualTo(200);
        assertThat(get("localhost", "/app/a/dev-ui/static/app/app.js", null).statusCode()).isEqualTo(200);
    }

    @Test
    void staticResourcesOfTheApplicationAreServedBySpringMvc() throws Exception {
        HttpResponse<String> response = getAsUser("localhost", port, "/app/application.txt", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Served by the application");
    }

    @Test
    void staticResourcesOfTheApplicationUnderTheDevUiPathAreNotServed() throws Exception {
        HttpResponse<String> response = getAsUser("localhost", port, "/app/a/dev-ui/static/app/application.txt", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).isEqualTo("Not Found");
    }

    @Test
    void requestsOutsideTheDevUiPathAreNotFiltered() throws Exception {
        HttpResponse<String> response = getAsUser("localhost", port, "/app/application.txt", "https://evil.example.com");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void servletPathOfSpringMvcDoesNotMoveTheDevUi() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "spring.mvc.servlet.path=/api", SECURITY_USER, SECURITY_PASSWORD)
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            assertThat(get("localhost", otherPort, "/a/dev-ui/", null).statusCode()).isEqualTo(200);
            assertThat(get("localhost", otherPort, "/a/dev-ui/static/app/app.js", null).statusCode()).isEqualTo(200);
            assertThat(getAsUser("localhost", otherPort, "/api/application.txt", null).body()).isEqualTo("Served by the application");
            assertThat(getAsUser("localhost", otherPort, "/api/a/dev-ui", null).statusCode()).isEqualTo(404);
            // The endpoints of the application are under the path of Spring MVC.
            HttpResponse<String> notFound = send(HttpRequest.newBuilder(URI.create("http://localhost:" + otherPort + "/api/missing")).GET()
                    .header("Authorization", CREDENTIALS)
                    .header("Accept", "text/html"), null);
            assertThat(notFound.body()).contains("<span class=\"methods\">GET</span><a href=\"/api/fail\">/api/fail</a>");
        }
    }

    @Test
    void welcomePageIsShownToBrowserAtTheRootPathInPlaceOfTheWhitelabelErrorPage() throws Exception {
        HttpResponse<String> response = getPageAsUser("/app/");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/html;charset=UTF-8");
        assertThat(response.body())
                .contains("<h1>Your application is running</h1>")
                .contains("<span class=\"methods\">GET</span><a href=\"/app/fail\">/fail</a>")
                .contains("<a class=\"dev-ui\" href=\"/app/a/dev-ui/\">Open the Dev UI</a>")
                .doesNotContain("Whitelabel");
    }

    @Test
    void notFoundPageIsShownToBrowserForPathThatDoesNotExist() throws Exception {
        HttpResponse<String> response = getPageAsUser("/app/missing");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body())
                .contains("<h1>Nothing here</h1>")
                .contains("The application serves nothing at <code>/missing</code>.");
    }

    @Test
    void errorPageIsShownToBrowserForRequestThatFails() throws Exception {
        HttpResponse<String> response = getPageAsUser("/app/fail");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body())
                .contains("<h1>500 Internal Server Error</h1>")
                .contains("<h2>java.lang.IllegalStateException</h2>")
                .contains("<p class=\"message\">The catalog is closed</p>")
                .contains("at io.arconia.dev.ui.server.DevUiIT$FailingController.fail");
    }

    @Test
    void errorPathOpenedDirectlyInBrowserIsNotShownAsFailure() throws Exception {
        HttpResponse<String> response = getPageAsUser("/app/error");

        assertThat(response.body()).contains("<h1>Nothing failed</h1>").contains("<code>/error</code>");
    }

    @Test
    void errorIsStillReportedAsJsonToOtherClients() throws Exception {
        HttpResponse<String> response = getAsUser("localhost", port, "/app/missing", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(response.body()).contains("\"status\":404").doesNotContain("<html");
    }

    @Test
    void errorPagesOfTheDevUiAreNotShownWhenTurnedOff() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "arconia.dev.ui.error-pages.enabled=false", SECURITY_USER, SECURITY_PASSWORD)
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + otherPort + "/missing")).GET()
                    .header("Authorization", CREDENTIALS)
                    .header("Accept", "text/html");

            assertThat(send(request, null).body()).contains("Whitelabel Error Page");
            assertThat(get("localhost", otherPort, "/a/dev-ui/", null).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void filtersOfTheApplicationDoNotGetInTheWayOfTheDevUi() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class, RejectingConfiguration.class)
                .properties("server.port=0")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            assertThat(get("localhost", otherPort, "/application.txt", null).statusCode()).isEqualTo(403);
            assertThat(get("localhost", otherPort, "/a/dev-ui/", null).statusCode()).isEqualTo(200);
            assertThat(get("localhost", otherPort, "/a/dev-ui/pages/greetings", null).statusCode()).isEqualTo(200);
            assertThat(get("localhost", otherPort, "/a/dev-ui/api/pages/greetings", null).statusCode()).isEqualTo(200);
            assertThat(get("localhost", otherPort, "/a/dev-ui/api/pages/greetings/logs?card=hello&item=Logs", null).body())
                    .startsWith("data: Hello, world");
            assertThat(get("localhost", otherPort, "/a/dev-ui/static/app/app.css", null).statusCode()).isEqualTo(200);
            assertThat(get("localhost", otherPort, "/a/dev-ui/_static/lit/3.3.3/index.js", null).statusCode()).isEqualTo(200);
            assertThat(get("localhost", otherPort, "/a/dev-ui/", "https://evil.example.com").statusCode()).isEqualTo(403);
        }
    }

    /**
     * A web page reaching the Dev UI through a host name it controls can add any header to its
     * requests, including the ones an application behind a proxy trusts to know its host.
     */
    @Test
    void forwardedHeadersCannotMakeAnotherHostPassForTheLocalMachine() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "server.forward-headers-strategy=framework")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            assertThat(statusLine(otherPort, "/a/dev-ui/api/pages", "localhost", null)).contains(" 200");
            assertThat(statusLine(otherPort, "/a/dev-ui/api/pages", "evil.example.com", null)).contains(" 403");
            assertThat(statusLine(otherPort, "/a/dev-ui/api/pages", "evil.example.com", "localhost")).contains(" 403");
        }
    }

    /**
     * When the web server applies the forwarded headers itself, it also replaces the address a
     * request comes from with the one a header claims, so no request can be told to come from
     * the local machine.
     */
    @Test
    void whenTheWebServerAppliesForwardedHeadersThenTheLocalMachineIsOnlyAcceptedWithRemoteAccess() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "server.forward-headers-strategy=native")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            assertThat(statusLine(otherPort, "/a/dev-ui/api/pages", "localhost", null)).contains(" 403");
        }
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class)
                .properties("server.port=0", "server.forward-headers-strategy=native", "arconia.dev.ui.remote-access.enabled=true")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            assertThat(statusLine(otherPort, "/a/dev-ui/api/pages", "localhost", null)).contains(" 200");
            assertThat(statusLine(otherPort, "/a/dev-ui/api/pages", "evil.example.com", "localhost")).contains(" 403");
        }
    }

    @Test
    void failureOfContributedPageIsNotHandledByTheApplication() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class, BrokenPageConfiguration.class)
                .properties("server.port=0")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + otherPort + "/a/dev-ui/api/pages/broken"))
                    .GET()
                    .header("Accept", "text/html");

            HttpResponse<String> response = send(request, null);

            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(json.from(response.body())).isStrictlyEqualToJson("""
                    { "error": { "status": 500, "message": "The content of the page 'Broken' cannot be loaded: com/example/Missing" } }
                    """);
        }
    }

    @Test
    void requestsToTheDevUiAreLeftOutOfTheObservationsOfTheApplication() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class, ObservationConfiguration.class)
                .properties("server.port=0")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            get("localhost", otherPort, "/a/dev-ui/", null);
            get("localhost", otherPort, "/a/dev-ui/api/pages/greetings", null);
            get("localhost", otherPort, "/a/dev-ui/api/pages/greetings/logs?card=hello&item=Logs", null);
            get("localhost", otherPort, "/application.txt", null);

            assertThat(otherContext.getBean(RecordingObservationHandler.class).requests).containsExactly("/application.txt");
        }
    }

    @Test
    void requestsToTheDevUiAreObservedWhenEnabled() throws Exception {
        try (ConfigurableApplicationContext otherContext = new SpringApplicationBuilder(TestApplication.class, ObservationConfiguration.class)
                .properties("server.port=0", "arconia.dev.ui.observations.enabled=true")
                .run()) {
            int otherPort = ((WebServerApplicationContext) otherContext).getWebServer().getPort();

            get("localhost", otherPort, "/a/dev-ui/api/pages", null);

            assertThat(otherContext.getBean(RecordingObservationHandler.class).requests).containsExactly("/a/dev-ui/api/pages");
        }
    }

    /**
     * Sends a request with the given {@code Host} header, which the HTTP client of the JDK
     * derives from the URL and doesn't let a caller set, and returns the status line.
     */
    private static String statusLine(int port, String path, String hostHeader, String forwardedHost) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            String request = "GET " + path + " HTTP/1.1\r\nHost: " + hostHeader + ":" + port + "\r\n"
                    + (forwardedHost != null ? "X-Forwarded-Host: " + forwardedHost + "\r\n" : "")
                    + "Connection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
        }
    }

    private static HttpResponse<String> get(String host, String path, String origin) throws Exception {
        return get(host, port, path, origin);
    }

    private static HttpResponse<String> get(String host, int port, String path, String origin) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + path)).GET(), origin);
    }

    private static HttpResponse<String> getAsUser(String host, int port, String path, String origin) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + path)).GET()
                .header("Authorization", CREDENTIALS), origin);
    }

    /**
     * Requests a path of the application the way a browser does when it opens a page.
     */
    private static HttpResponse<String> getPageAsUser(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET()
                .header("Authorization", CREDENTIALS)
                .header("Accept", "text/html"), null);
    }

    private static HttpResponse<String> send(HttpRequest.Builder request, String origin) throws Exception {
        if (origin != null) {
            request.header("Origin", origin);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * An application that rejects every request: Spring Security denies them all, and so does
     * a filter placed where a filter resolving the current tenant would be.
     */
    @Configuration(proxyBeanMethods = false)
    static class RejectingConfiguration {

        @Bean
        SecurityFilterChain denyAll(HttpSecurity http) {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll()).build();
        }

        @Bean
        FilterRegistrationBean<Filter> rejectingFilter() {
            Filter filter = (request, response, chain) -> ((HttpServletResponse) response).setStatus(403);
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
            registration.setOrder(Ordered.LOWEST_PRECEDENCE);
            return registration;
        }

    }

    /**
     * A page from a library used without one of its optional dependencies.
     */
    @Configuration(proxyBeanMethods = false)
    static class BrokenPageConfiguration {

        @Bean
        CardsPage brokenPage() {
            return CardsPage.builder().id("broken").title("Broken").cards(() -> {
                throw new NoClassDefFoundError("com/example/Missing");
            }).build();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ObservationConfiguration {

        @Bean
        RecordingObservationHandler recordingObservationHandler() {
            return new RecordingObservationHandler();
        }

    }

    /**
     * Records the path of each request that the application observes.
     */
    static class RecordingObservationHandler implements ObservationHandler<ServerRequestObservationContext> {

        final List<String> requests = new CopyOnWriteArrayList<>();

        @Override
        public void onStart(ServerRequestObservationContext context) {
            this.requests.add(context.getCarrier().getRequestURI());
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return context instanceof ServerRequestObservationContext;
        }

    }

    @RestController
    static class FailingController {

        @GetMapping("/fail")
        String fail() {
            throw new IllegalStateException("The catalog is closed");
        }

    }

    @SpringBootApplication
    @Import(FailingController.class)
    static class TestApplication {

        @Bean
        CardsPage greetingsPage() {
            return CardsPage.builder()
                    .id("greetings")
                    .title("Greetings")
                    .cards(() -> List.of(Card.builder()
                            .id("hello")
                            .title("Hello")
                            .items(List.of(
                                    InfoItem.of("Language", "English"),
                                    LogItem.of("Logs", lines -> lines.accept("Hello, world")),
                                    LogItem.of("Endless", lines -> {
                                        lines.accept("still running");
                                        new CountDownLatch(1).await();
                                    })))
                            .build()))
                    .build();
        }

    }

}
