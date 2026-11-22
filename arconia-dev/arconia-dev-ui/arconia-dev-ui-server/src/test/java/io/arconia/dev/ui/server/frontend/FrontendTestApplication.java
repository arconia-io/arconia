package io.arconia.dev.ui.server.frontend;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;
import io.arconia.dev.ui.api.page.LogItem;
import io.arconia.dev.ui.api.page.Status;
import io.arconia.dev.ui.api.page.StatusLevel;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;

/**
 * The application the frontend tests open the Dev UI of, with a page of cards and a page with a
 * table contributed to it. It doesn't scan for components, so that the configurations of other
 * tests stay out of it, and it isn't a component itself, so that the applications of other tests
 * that do scan, such as the one of {@code DevUiIT}, don't get its pages.
 */
@EnableAutoConfiguration
class FrontendTestApplication {

    @Bean
    ServerLog serverLog() {
        return new ServerLog();
    }

    @Bean
    CardsPage servicesPage(ServerLog serverLog) {
        return CardsPage.builder()
                .id("services")
                .title("Services")
                .cards(() -> List.of(Card.builder()
                        .id("catalog")
                        .title("Catalog")
                        .description("The catalog of the shop.")
                        .status(new Status("Running", StatusLevel.SUCCESS))
                        .propertiesPrefix("server")
                        .items(InfoItem.of("Port", "8081"), LinkItem.of("Console", "https://catalog.example.com"),
                                LogItem.of("Access log", FrontendTestApplication::accessLog),
                                LogItem.of("Server log", serverLog::follow),
                                LogItem.of("Audit log", lines -> {
                                    throw new IllegalStateException("The audit trail is offline");
                                }))
                        .build()))
                .build();
    }

    /**
     * A log that ends after a few lines.
     */
    private static void accessLog(Consumer<String> lines) {
        lines.accept("GET /orders 200");
        lines.accept("GET /orders/42 404");
        lines.accept("See the guide at https://shop.example.com/guide.");
    }

    @Bean
    Settings settings() {
        return new Settings();
    }

    @Bean
    TablePage settingsPage(Settings settings) {
        return TablePage.builder()
                .id("settings")
                .title("Settings")
                .columns("Name", "Value", "Kind")
                .filterColumn("Kind")
                .rows(settings::rows)
                .build();
    }

    /**
     * The rows of the table of settings, of three kinds. A test can remove the rows of a kind,
     * as a change to the application would, and put them back.
     */
    static class Settings {

        static final List<TableRow> ROWS = List.of(
                row("catalog.url", TableCell.text("https://catalog.example.com"), "Service"),
                row("catalog.password", TableCell.secret("hunter2"), "Credential"),
                row("orders.url", TableCell.text("https://orders.example.com"), "Service"),
                row("orders.token", TableCell.secret("s3cr3t-token"), "Credential"),
                row("orders.retries", TableCell.text("3"), "Tuning"));

        private final Set<String> removedKinds = new CopyOnWriteArraySet<>();

        List<TableRow> rows() {
            return ROWS.stream().filter(row -> !this.removedKinds.contains(row.getCells().get(2).getText())).toList();
        }

        void remove(String kind) {
            this.removedKinds.add(kind);
        }

        void reset() {
            this.removedKinds.clear();
        }

        private static TableRow row(String name, TableCell value, String kind) {
            return TableRow.builder()
                    .id(name)
                    .cells(List.of(TableCell.text(name), value, TableCell.text(kind)))
                    .details(List.of(InfoItem.of("Source", "application.properties")))
                    .build();
        }

    }

    /**
     * A log that goes on until nobody watches it anymore, and tells how many watch it. After its
     * first lines, it writes the ones a test asks for.
     */
    static class ServerLog {

        private final AtomicInteger viewers = new AtomicInteger();

        private final BlockingQueue<String> next = new LinkedBlockingQueue<>();

        void follow(Consumer<String> lines) throws InterruptedException {
            this.viewers.incrementAndGet();
            try {
                lines.accept("Server started");
                lines.accept("Connection error on port 8081");
                lines.accept("Request handled");
                while (true) {
                    lines.accept(this.next.take());
                }
            } finally {
                this.viewers.decrementAndGet();
            }
        }

        void write(String line) {
            this.next.add(line);
        }

        int viewers() {
            return this.viewers.get();
        }

    }

}
