package io.arconia.dev.ui.server.web;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.Status;
import io.arconia.dev.ui.api.page.StatusLevel;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;
import io.arconia.dev.ui.api.page.LogItem;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiApi}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevUiApiTests {

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    private final DevUiLogs logs = new DevUiLogs();

    private final DevUiApi api = new DevUiApi(new DevUiPages(List.of(
            CardsPage.builder()
                    .id("dev-services")
                    .title("Dev Services")
                    .icon("container")
                    .maxColumns(2)
                    .cards(() -> List.of(
                            Card.builder()
                                    .id("postgresql")
                                    .title("PostgreSQL")
                                    .description("Relational database")
                                    .status(new Status("Running", StatusLevel.SUCCESS))
                                    .items(List.of(
                                            InfoItem.of("Image", "postgres"),
                                            InfoItem.of("Ports", List.of("5432 → 54321", "9187 → 59187")),
                                            LinkItem.of("Console", "http://localhost:8080")))
                                    .build(),
                            Card.builder()
                                    .id("redis")
                                    .title("Redis & more")
                                    .items(List.of(LogItem.of("Logs", lines -> lines.accept("ready to accept connections"))))
                                    .build()))
                    .build(),
            CardsPage.builder()
                    .id("failing")
                    .title("Failing")
                    .order(10)
                    .cards(() -> {
                        throw new IllegalStateException("container runtime not available");
                    })
                    .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());

    @Test
    void pagesAreListedWithoutTheirContent() throws Exception {
        MockHttpServletResponse response = write("/api/pages");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                {
                  "navigation": [
                    { "page": { "id": "overview", "title": "Overview", "type": "overview", "icon": "layout-dashboard" } },
                    { "page": { "id": "dev-services", "title": "Dev Services", "type": "cards", "icon": "container" } },
                    { "page": { "id": "failing", "title": "Failing", "type": "cards", "icon": "layout-grid" } }
                  ]
                }
                """);
    }

    @Test
    void pageIsReturnedWithItsContent() throws Exception {
        MockHttpServletResponse response = write("/api/pages/dev-services");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                {
                  "id": "dev-services",
                  "title": "Dev Services",
                  "type": "cards",
                  "icon": "container",
                  "columns": 2,
                  "cards": [
                    {
                      "id": "postgresql",
                      "title": "PostgreSQL",
                      "description": "Relational database",
                      "status": { "text": "Running", "level": "success" },
                      "items": [
                        { "type": "info", "label": "Image", "values": [ "postgres" ] },
                        { "type": "info", "label": "Ports", "values": [ "5432 → 54321", "9187 → 59187" ] },
                        { "type": "link", "label": "Console", "url": "http://localhost:8080" }
                      ]
                    },
                    { "id": "redis", "title": "Redis & more", "items": [ { "type": "log", "label": "Logs" } ] }
                  ]
                }
                """);
    }

    @Test
    void whenThePageDoesNotExistThenNotFound() throws Exception {
        MockHttpServletResponse response = write("/api/pages/unknown");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                { "error": { "status": 404, "message": "The page 'unknown' doesn't exist" } }
                """);
    }

    @Test
    void whenTheContentOfThePageCannotBeLoadedThenTheFailureIsReported(CapturedOutput output) throws Exception {
        MockHttpServletResponse response = write("/api/pages/failing");

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getErrorMessage()).isNull();
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                {
                  "error": {
                    "status": 500,
                    "message": "The content of the page 'Failing' cannot be loaded: container runtime not available"
                  }
                }
                """);
        assertThat(output).contains("Cannot load the content of the Dev UI page 'failing'");
    }

    @Test
    void logIsFoundByTheIdentifierOfItsCardAndTheLabelOfItsItem() throws Exception {
        this.logs.start();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pages/dev-services/logs");
        request.setParameter("card", "redis");
        request.setParameter("item", "Logs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.api.write("/api/pages/dev-services/logs", request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/event-stream;charset=UTF-8");
        assertThat(response.getContentAsString()).startsWith("data: ready to accept connections\n\n");
    }

    @ParameterizedTest
    @CsvSource({ "postgresql, Logs", "redis, Console", "Redis & more, Logs", "unknown, Logs" })
    void whenThereIsNoSuchLogThenTheStreamTellsWhy(String card, String item) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pages/dev-services/logs");
        request.setParameter("card", card);
        request.setParameter("item", item);
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.api.write("/api/pages/dev-services/logs", request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/event-stream;charset=UTF-8");
        assertThat(response.getContentAsString())
                .isEqualTo("event: failure\ndata: The page 'Dev Services' has no log '%s' for the card '%s'\n\n".formatted(item, card));
    }

    @Test
    void itemHasItsIconWhenThereIsOneByThatNameAndTellsWhenItsValuesAreCode(CapturedOutput output) throws Exception {
        DevUiApi icons = new DevUiApi(new DevUiPages(List.of(CardsPage.builder()
                .id("databases")
                .title("Databases")
                .cards(() -> List.of(Card.builder().id("postgresql").title("PostgreSQL").icon("database")
                        .documentation("https://example.org/docs/postgresql/").propertiesPrefix("spring.datasource").items(List.of(
                        InfoItem.builder().label("Image").value("postgres").icon("package").code(true).build(),
                        InfoItem.builder().label("Owner").value("shop").icon("no-such-icon").build(),
                        InfoItem.of("Size", "12 MB"))).build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        icons.write("/api/pages/databases", new MockHttpServletRequest("GET", "/api/pages/databases"), response);
        icons.write("/api/pages/databases", new MockHttpServletRequest("GET", "/api/pages/databases"), new MockHttpServletResponse());

        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                {
                  "id": "databases",
                  "title": "Databases",
                  "type": "cards",
                  "icon": "layout-grid",
                  "columns": 3,
                  "cards": [
                    {
                      "id": "postgresql",
                      "title": "PostgreSQL",
                      "icon": "database",
                      "documentation": "https://example.org/docs/postgresql/",
                      "propertiesPrefix": "spring.datasource",
                      "items": [
                        { "type": "info", "label": "Image", "values": [ "postgres" ], "icon": "package", "code": true },
                        { "type": "info", "label": "Owner", "values": [ "shop" ] },
                        { "type": "info", "label": "Size", "values": [ "12 MB" ] }
                      ]
                    }
                  ]
                }
                """);
        assertThat(output.getOut().split("The Dev UI page 'Databases' has a card or an item with the icon 'no-such-icon', and there is no icon with that name", -1))
                .hasSize(2);
    }

    @Test
    void linkToPathOfTheApplicationStartsFromItsContextPath() throws Exception {
        DevUiApi links = new DevUiApi(new DevUiPages(List.of(CardsPage.builder()
                .id("docs")
                .title("Docs")
                .cards(() -> List.of(Card.builder().id("OpenAPI 3").title("OpenAPI")
                        .items(LinkItem.of("Swagger UI", "/swagger-ui.html"), LinkItem.of("Specification", "https://spec.openapis.org")).build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shop/a/dev-ui/api/pages/docs");
        request.setContextPath("/shop");
        MockHttpServletResponse response = new MockHttpServletResponse();

        links.write("/api/pages/docs", request, response);

        assertThat(this.json.from(response.getContentAsString())).extractingJsonPathArrayValue("$.cards[0].items[*].url")
                .containsExactly("/shop/swagger-ui.html", "https://spec.openapis.org");
        assertThat(this.json.from(response.getContentAsString())).extractingJsonPathStringValue("$.cards[0].id").isEqualTo("OpenAPI 3");
    }

    /**
     * A log or a row is found by the identifier of its card or row, which must then tell it
     * apart from the others.
     */
    @Test
    void whenSeveralCardsHaveTheSameIdentifierThenThePageFails() throws Exception {
        DevUiApi duplicates = new DevUiApi(new DevUiPages(List.of(CardsPage.builder()
                .id("databases")
                .title("Databases")
                .cards(() -> List.of(
                        Card.builder().id("postgresql").title("First").build(),
                        Card.builder().id("postgresql").title("Second").build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        duplicates.write("/api/pages/databases", new MockHttpServletRequest("GET", "/api/pages/databases"), response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .contains("The content of the page 'Databases' cannot be loaded: There are several cards with the identifier 'postgresql'");
    }

    @Test
    void whenSeveralRowsHaveTheSameIdentifierThenThePageFails() throws Exception {
        DevUiApi duplicates = new DevUiApi(new DevUiPages(List.of(TablePage.builder()
                .id("tables")
                .title("Tables")
                .columns("Name", "Rows")
                .rows(() -> List.of(
                        TableRow.builder().id("orders").cells("orders", "12").build(),
                        TableRow.builder().id("orders").cells("orders", "13").build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        duplicates.write("/api/pages/tables", new MockHttpServletRequest("GET", "/api/pages/tables"), response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .contains("The content of the page 'Tables' cannot be loaded: There are several rows with the identifier 'orders'");
    }

    @Test
    void whenRowsMissCellsThenTheyAreShownWithOneWarning(CapturedOutput output) throws Exception {
        DevUiApi tables = new DevUiApi(new DevUiPages(List.of(TablePage.builder()
                .id("tables")
                .title("Tables")
                .columns("Name", "Rows")
                .rows(() -> List.of(
                        TableRow.builder().id("orders").cells("orders", "12").build(),
                        TableRow.builder().id("invoices").cells("invoices").build(),
                        TableRow.builder().id("customers").cells("customers").build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse first = new MockHttpServletResponse();

        tables.write("/api/pages/tables", new MockHttpServletRequest("GET", "/api/pages/tables"), first);
        tables.write("/api/pages/tables", new MockHttpServletRequest("GET", "/api/pages/tables"), new MockHttpServletResponse());

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(first.getContentAsString()).contains("customers");
        assertThat(output.getOut().split("The Dev UI page 'Tables' has rows that don't have one cell for each of its 2 columns", -1))
                .hasSize(2);
    }

    /**
     * A text too long to be read in the table is shown in full with the details of its row,
     * before the details the row comes with. A secret is not repeated there.
     */
    @Test
    void longTextOfCellIsShownInFullWithTheDetailsOfItsRow() throws Exception {
        String longText = "jdbc:postgresql://localhost:5432/catalog?ssl=true&sslmode=verify-full&applicationName=shop";
        DevUiApi tables = new DevUiApi(new DevUiPages(List.of(TablePage.builder()
                .id("tables")
                .title("Tables")
                .columns("Property", "Value", "Password")
                .rows(() -> List.of(
                        TableRow.builder().id("url").cells(List.of(TableCell.text("url"), TableCell.text(longText),
                                TableCell.secret(longText))).details(List.of(InfoItem.of("Source", "application.yaml"))).build(),
                        TableRow.builder().id("port").cells("port", "5432", "").build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        tables.write("/api/pages/tables", new MockHttpServletRequest("GET", "/api/pages/tables"), response);

        assertThat(this.json.from(response.getContentAsString())).extractingJsonPathValue("$.table.rows[0].details")
                .isEqualTo(List.of(Map.of("label", "Value", "values", List.of(longText)),
                        Map.of("label", "Source", "values", List.of("application.yaml"))));
        assertThat(this.json.from(response.getContentAsString())).doesNotHaveJsonPath("$.table.rows[1].details");
    }

    @Test
    void pieceOfInformationWithoutValueIsShownWithDash() throws Exception {
        DevUiApi unknown = new DevUiApi(new DevUiPages(List.of(CardsPage.builder()
                .id("databases")
                .title("Databases")
                .cards(() -> List.of(Card.builder().id("postgresql").title("PostgreSQL")
                        .items(InfoItem.builder().label("Image").value(null).code(true).build())
                        .build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        unknown.write("/api/pages/databases", new MockHttpServletRequest("GET", "/api/pages/databases"), response);

        assertThat(this.json.from(response.getContentAsString())).extractingJsonPathValue("$.cards[0].items[0]")
                .isEqualTo(Map.of("type", "info", "label", "Image", "values", List.of("—")));
    }

    @Test
    @SuppressWarnings("NullAway")
    void whenTheSupplierOfThePageReturnsNullThenTheFailureSaysSo() throws Exception {
        DevUiApi nothing = new DevUiApi(new DevUiPages(List.of(CardsPage.builder().id("nothing").title("Nothing").cards(() -> null).build()),
                new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        nothing.write("/api/pages/nothing", new MockHttpServletRequest("GET", "/api/pages/nothing"), response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .contains("The content of the page 'Nothing' cannot be loaded: The supplier of the cards of the page returned null");
    }

    @Test
    void whenTheCardsOfTheLogCannotBeSuppliedThenTheStreamTellsWhy() throws Exception {
        DevUiApi failing = new DevUiApi(new DevUiPages(List.of(CardsPage.builder().id("broken").title("Broken")
                .cards(() -> {
                    throw new IllegalStateException("No connection");
                })
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pages/broken/logs");
        request.setParameter("card", "main");
        request.setParameter("item", "Logs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failing.write("/api/pages/broken/logs", request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .isEqualTo("event: failure\ndata: The content of the page 'Broken' cannot be loaded: No connection\n\n");
    }

    @Test
    void whenThePageOfTheLogDoesNotExistThenTheStreamTellsWhy() throws Exception {
        MockHttpServletResponse response = write("/api/pages/unknown/logs");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("event: failure\ndata: The page 'unknown' doesn't exist\n\n");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/", "/api/unknown", "/api/pages/", "/api/pagesx" })
    void anythingElseIsNotFound(String path) throws Exception {
        MockHttpServletResponse response = write(path);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).isEqualTo("application/json");
    }

    @Test
    void tablePageIsReturnedWithItsRows() throws Exception {
        DevUiApi tables = new DevUiApi(new DevUiPages(List.of(TablePage.builder()
                .id("environment")
                .title("Environment")
                .columns(List.of("Property", "Value"))
                .filterColumn("Value")
                .rows(() -> List.of(
                        TableRow.builder().id("server.port").cells("server.port", "8080")
                                .details(List.of(InfoItem.of("Origin", "application.yml - 3:9"))).build(),
                        TableRow.builder().id("db.password")
                                .cells(List.of(TableCell.text("db.password"), TableCell.secret("s3cret"))).build()))
                .build()), new DevUiIcons(DevUiImportMap.load())), this.logs, application());
        MockHttpServletResponse response = new MockHttpServletResponse();

        tables.write("/api/pages/environment", new MockHttpServletRequest("GET", "/api/pages/environment"), response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(this.json.from(response.getContentAsString())).isStrictlyEqualToJson("""
                {
                  "id": "environment",
                  "title": "Environment",
                  "type": "table",
                  "icon": "table",
                  "table": {
                    "columns": [ "Property", "Value" ],
                    "filterColumn": 1,
                    "rows": [
                      {
                        "id": "server.port",
                        "cells": [ { "type": "text", "text": "server.port" }, { "type": "text", "text": "8080" } ],
                        "details": [ { "label": "Origin", "values": [ "application.yml - 3:9" ] } ]
                      },
                      {
                        "id": "db.password",
                        "cells": [ { "type": "text", "text": "db.password" }, { "type": "secret", "text": "s3cret" } ]
                      }
                    ]
                  }
                }
                """);
    }

    @Test
    void overviewOfTheApplicationIsThePageTheDevUiProvidesItself() throws Exception {
        MockHttpServletResponse response = write("/api/pages/overview");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(this.json.from(response.getContentAsString()))
                .extractingJsonPathStringValue("$.type").isEqualTo("overview");
        assertThat(this.json.from(response.getContentAsString()))
                .extractingJsonPathStringValue("$.application.name").isEqualTo("catalog-service");
        assertThat(this.json.from(response.getContentAsString()))
                .extractingJsonPathStringValue("$.application.version").isEqualTo("1.4.0");
        assertThat(this.json.from(response.getContentAsString()))
                .extractingJsonPathArrayValue("$.application.facts[*].label").containsExactly("Spring Boot", "Java", "Port", "Profiles");
        assertThat(this.json.from(response.getContentAsString()))
                .extractingJsonPathArrayValue("$.cards[*].id").containsExactly("application", "spring", "java", "system");
        assertThat(this.json.from(response.getContentAsString())).extractingJsonPathNumberValue("$.columns").isNull();
    }

    private static DevUiApplication application() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        TestPropertyValues.of("spring.application.name=catalog-service", "spring.application.version=1.4.0").applyTo(applicationContext);
        applicationContext.refresh();
        return new DevUiApplication(applicationContext);
    }

    private MockHttpServletResponse write(String path) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        this.api.write(path, new MockHttpServletRequest("GET", path), response);
        return response;
    }

}
