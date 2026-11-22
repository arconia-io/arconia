package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.CardItem;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.DevUiPage;
import io.arconia.dev.ui.api.page.DevUiSection;
import io.arconia.dev.ui.api.page.InfoItem;
import io.arconia.dev.ui.api.page.LinkItem;
import io.arconia.dev.ui.api.page.LogItem;
import io.arconia.dev.ui.api.page.LogSource;
import io.arconia.dev.ui.api.page.Status;
import io.arconia.dev.ui.api.page.TableCell;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.api.page.TableRow;
import io.arconia.dev.ui.server.logging.ApplicationLogs;
import io.arconia.dev.ui.server.web.DevUiApplication.Fact;
import io.arconia.dev.ui.server.web.DevUiApplication.Overview;

/**
 * The HTTP API the Dev UI frontend reads its data from.
 * <p>
 * The JSON it returns is defined here, by the records at the end of the class, and not by
 * the types that libraries contribute pages with. It is written with a mapper of its own, so
 * that the JSON configuration of the application has no effect on it.
 */
final class DevUiApi {

    /**
     * The path of the API, relative to the path of the Dev UI.
     */
    static final String PATH = "/api/";

    private static final String PAGES_PATH = PATH + "pages";

    private static final String LOGS_PATH = PATH + "logs";

    static final String ACTIONS_PATH = PATH + "actions/";

    private static final String RESTART_ACTION = ACTIONS_PATH + "restart";

    private static final Logger logger = LoggerFactory.getLogger(DevUiApi.class);

    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    private static final String LOGS_SUFFIX = "/logs";

    private final DevUiPages pages;

    private final DevUiLogs logs;

    private final Set<String> reportedProblems = ConcurrentHashMap.newKeySet();

    private final DevUiApplication application;

    DevUiApi(DevUiPages pages, DevUiLogs logs, DevUiApplication application) {
        this.pages = pages;
        this.logs = logs;
        this.application = application;
    }

    /**
     * Writes the response for the given path, relative to the path of the Dev UI.
     */
    void write(String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (path.equals(PAGES_PATH)) {
            writeJson(response, HttpServletResponse.SC_OK, NavigationJson.of(this.pages));
            return;
        }
        if (path.equals(LOGS_PATH)) {
            this.logs.write(DevUiApi::followApplicationLogs, response);
            return;
        }
        if (path.startsWith(PAGES_PATH + "/") && path.endsWith(LOGS_SUFFIX)) {
            String id = path.substring(PAGES_PATH.length() + 1, path.length() - LOGS_SUFFIX.length());
            writeLog(id, request.getParameter("card"), request.getParameter("item"), response);
            return;
        }
        if (path.startsWith(PAGES_PATH + "/")) {
            writePage(path.substring(PAGES_PATH.length() + 1), request, response);
            return;
        }
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
    }

    /**
     * Performs the action at the given path, relative to the path of the Dev UI. The request
     * has been checked to come from the Dev UI itself.
     */
    void act(String path, HttpServletResponse response) throws IOException {
        if (!path.equals(RESTART_ACTION)) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
            return;
        }
        if (!this.application.canRestart()) {
            writeError(response, HttpServletResponse.SC_CONFLICT,
                    "The application cannot be restarted: Spring Boot DevTools is not there or its restart support is not in use");
            return;
        }
        logger.info("Restarting the application, as asked from the Dev UI");
        this.application.restart();
        writeJson(response, HttpServletResponse.SC_ACCEPTED, Map.of());
    }

    /**
     * The logs of the application, for the console of the Dev UI: each line as JSON, starting
     * with the lines kept so far.
     */
    private static void followApplicationLogs(Consumer<String> lines) throws InterruptedException {
        ApplicationLogs.get().follow(line -> lines.accept(jsonMapper.writeValueAsString(line)));
    }

    /**
     * The content of a page is supplied by the library that contributes it. When that fails,
     * the failure is logged and reported to the frontend, which shows it in place of the page.
     */
    private void writePage(String id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DevUiPage page = this.pages.find(id);
        if (page == null && !id.equals(DevUiPages.OVERVIEW_ID)) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The page '%s' doesn't exist".formatted(id));
            return;
        }
        PageJson pageJson;
        try {
            if (page == null) {
                pageJson = PageJson.overview(this.application.overview(request), request.getContextPath(), icon -> icon);
            } else {
                pageJson = PageJson.withContent(page, this.pages.icon(page), request.getContextPath(), icon -> itemIcon(page, icon));
                checkContent(page, pageJson);
            }
        } catch (RuntimeException | LinkageError ex) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    failure(id, (page != null) ? page.getTitle() : PageJson.OVERVIEW_TITLE, ex));
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, pageJson);
    }

    /**
     * A log is identified by the identifier of its card and the label of its item. The cards
     * of a page are supplied again to find it, so no state is kept between requests. A log that
     * cannot be followed is answered with the reason, as a stream, the only answer the browser
     * can read when it asks for a log.
     */
    private void writeLog(String id, @Nullable String cardId, @Nullable String itemLabel, HttpServletResponse response)
            throws IOException {
        DevUiPage page = this.pages.find(id);
        if (page == null) {
            this.logs.writeFailure("The page '%s' doesn't exist".formatted(id), response);
            return;
        }
        LogSource source;
        try {
            source = findLog(page, cardId, itemLabel);
        } catch (RuntimeException | LinkageError ex) {
            this.logs.writeFailure(failure(id, page.getTitle(), ex), response);
            return;
        }
        if (source == null) {
            this.logs.writeFailure("The page '%s' has no log '%s' for the card '%s'".formatted(page.getTitle(), itemLabel, cardId),
                    response);
            return;
        }
        this.logs.write(source, response);
    }

    /**
     * Logs that the content of a page could not be loaded, and tells why for the developer.
     * The content is supplied by the library that contributes the page. A failure includes a
     * class that is missing at run time, as when a library is used without one of its optional
     * dependencies.
     */
    private static String failure(String pageId, String pageTitle, Throwable failure) {
        logger.error("Cannot load the content of the Dev UI page '{}'", pageId, failure);
        String reason = failure.getMessage() != null ? failure.getMessage() : failure.getClass().getName();
        return "The content of the page '%s' cannot be loaded: %s".formatted(pageTitle, reason);
    }

    private static @Nullable LogSource findLog(DevUiPage page, @Nullable String cardId, @Nullable String itemLabel) {
        List<Card> cards = switch (page) {
            case CardsPage cardsPage -> cardsPage.getCards().get();
            case TablePage tablePage -> List.of();
        };
        return cards.stream()
                .filter(card -> card.getId().equals(cardId))
                .flatMap(card -> card.getItems().stream())
                .filter(item -> item instanceof LogItem && item.getLabel().equals(itemLabel))
                .map(item -> ((LogItem) item).getSource())
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks the content of a page. Cards or rows with the same identifier fail the page, since
     * a log or a row is found by the identifier of its card or row, and only the first of them
     * could be. Rows that don't have one cell for each column are shown as they are, with a
     * warning logged once, not each time the page is shown.
     */
    private void checkContent(DevUiPage page, PageJson pageJson) {
        Set<String> ids = new HashSet<>();
        if (pageJson.cards() != null) {
            for (CardJson card : pageJson.cards()) {
                Assert.state(ids.add(card.id()), () -> "There are several cards with the identifier '%s'".formatted(card.id()));
            }
        }
        if (pageJson.table() != null) {
            int columns = pageJson.table().columns().size();
            for (TableRowJson row : pageJson.table().rows()) {
                Assert.state(ids.add(row.id()), () -> "There are several rows with the identifier '%s'".formatted(row.id()));
                if (row.cells().size() != columns) {
                    warnOnce(page, "cells", "rows that don't have one cell for each of its %d columns, such as the row '%s'"
                            .formatted(columns, row.id()));
                }
            }
        }
    }

    /**
     * The icon to show for a card or an item, which is one that the icon set has. One with the
     * name of an icon that doesn't exist is shown without it, with a warning.
     */
    private @Nullable String itemIcon(DevUiPage page, @Nullable String icon) {
        if (icon == null || this.pages.hasIcon(icon)) {
            return icon;
        }
        warnOnce(page, "icon " + icon, "a card or an item with the icon '%s', and there is no icon with that name".formatted(icon));
        return null;
    }

    /**
     * Logs a warning about a page once for each kind of problem, however many cards or rows
     * have it and however often the page is shown.
     */
    private void warnOnce(DevUiPage page, String kind, String problem) {
        if (this.reportedProblems.add(page.getId() + "/" + kind)) {
            logger.warn("The Dev UI page '{}' has {}", page.getTitle(), problem);
        }
    }

    /**
     * What a page supplies, which must be something: a page with nothing to show supplies an
     * empty list.
     */
    private static <T> List<T> supplied(@Nullable List<T> content, String what) {
        Assert.state(content != null, () -> "The supplier of the %s of the page returned null".formatted(what));
        return content;
    }

    private static @Nullable Integer filterColumn(TablePage page) {
        return (page.getFilterColumn() != null) ? page.getColumns().indexOf(page.getFilterColumn()) : null;
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, new ErrorResponseJson(new ErrorJson(status, message)));
    }

    private static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        byte[] json = jsonMapper.writeValueAsBytes(body);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        response.setContentLength(json.length);
        response.getOutputStream().write(json);
    }

    /**
     * What the navigation lists, in order: the pages without a section, each as an entry of its
     * own, and the sections, each as one entry with its pages. The Overview comes first.
     */
    record NavigationJson(List<NavigationEntryJson> navigation) {

        static NavigationJson of(DevUiPages pages) {
            List<NavigationEntryJson> entries = new ArrayList<>();
            entries.add(new NavigationEntryJson(PageJson.overview(), null, null));
            Map<DevUiSection, List<PageJson>> sections = new HashMap<>();
            for (DevUiPage page : pages.all()) {
                PageJson pageJson = PageJson.of(page, pages.icon(page));
                DevUiSection section = page.getSection();
                if (section == null) {
                    entries.add(new NavigationEntryJson(pageJson, null, null));
                } else {
                    // The pages of a section come one after the other, so the section is listed
                    // where its first page is.
                    sections.computeIfAbsent(section, key -> {
                        List<PageJson> sectionPages = new ArrayList<>();
                        entries.add(new NavigationEntryJson(null, SectionJson.of(key), sectionPages));
                        return sectionPages;
                    }).add(pageJson);
                }
            }
            return new NavigationJson(entries);
        }

    }

    /**
     * An entry of the navigation: either a page, or a section with its pages.
     */
    record NavigationEntryJson(@Nullable PageJson page, @Nullable SectionJson section, @Nullable List<PageJson> pages) {}

    record PageJson(String id, String title, String type, String icon, @Nullable Integer columns,
            @Nullable List<CardJson> cards, @Nullable ApplicationJson application, @Nullable TableJson table) {

        static final String OVERVIEW_TITLE = "Overview";

        /**
         * The overview of the application as listed in the navigation, without its content.
         */
        static PageJson overview() {
            return new PageJson(DevUiPages.OVERVIEW_ID, OVERVIEW_TITLE, "overview", "layout-dashboard", null, null, null, null);
        }

        static PageJson overview(Overview overview, String contextPath, ItemIcons icons) {
            return new PageJson(DevUiPages.OVERVIEW_ID, OVERVIEW_TITLE, "overview", "layout-dashboard", null,
                    overview.cards().stream().map(card -> CardJson.of(card, contextPath, icons)).toList(),
                    new ApplicationJson(overview.name(), overview.version(), overview.startedAt(), overview.facts()), null);
        }

        /**
         * The page as listed in the navigation, without its content.
         */
        static PageJson of(DevUiPage page, String icon) {
            return new PageJson(page.getId(), page.getTitle(), type(page), icon, null, null, null, null);
        }

        static PageJson withContent(DevUiPage page, String icon, String contextPath, ItemIcons icons) {
            return switch (page) {
                case CardsPage cardsPage -> new PageJson(cardsPage.getId(), cardsPage.getTitle(), type(page), icon,
                        cardsPage.getMaxColumns(), supplied(cardsPage.getCards().get(), "cards").stream().map(card -> CardJson.of(card, contextPath, icons)).toList(),
                        null, null);
                case TablePage tablePage -> new PageJson(tablePage.getId(), tablePage.getTitle(), type(page), icon, null, null, null,
                        new TableJson(tablePage.getColumns(), filterColumn(tablePage),
                                supplied(tablePage.getRows().get(), "rows").stream().map(row -> TableRowJson.of(row, tablePage.getColumns(), icons)).toList()));
            };
        }

        private static String type(DevUiPage page) {
            return switch (page) {
                case CardsPage cardsPage -> "cards";
                case TablePage tablePage -> "table";
            };
        }

    }

    /**
     * The section of the navigation a page is listed in.
     */
    record SectionJson(String id, String title, String icon) {

        static SectionJson of(DevUiSection section) {
            return new SectionJson(section.getId(), section.getTitle(), section.getIcon());
        }

    }

    /**
     * What the overview shows above its cards.
     */
    record ApplicationJson(String name, @Nullable String version, String startedAt, List<Fact> facts) {}

    record TableJson(List<String> columns, @Nullable Integer filterColumn, List<TableRowJson> rows) {}

    /**
     * A row of a table. Its details start with the text of the cells that is too long to be
     * read in the table, in full, followed by the details the row comes with. A secret is not
     * repeated there.
     */
    record TableRowJson(String id, List<TableCellJson> cells, @Nullable List<InfoJson> details) {

        /**
         * The number of characters from which the text of a cell is shown in full in the
         * details of its row.
         */
        static final int LONG_TEXT = 60;

        static TableRowJson of(TableRow row, List<String> columns, ItemIcons icons) {
            List<InfoJson> details = new ArrayList<>();
            List<TableCell> cells = row.getCells();
            for (int index = 0; index < Math.min(cells.size(), columns.size()); index++) {
                TableCell cell = cells.get(index);
                if (!cell.isSecret() && cell.getText().length() > LONG_TEXT) {
                    details.add(new InfoJson(columns.get(index), List.of(cell.getText()), null, null));
                }
            }
            row.getDetails().forEach(detail -> details.add(InfoJson.of(detail, icons)));
            return new TableRowJson(row.getId(), cells.stream().map(TableCellJson::of).toList(),
                    details.isEmpty() ? null : details);
        }

    }

    record TableCellJson(String type, String text) {

        static TableCellJson of(TableCell cell) {
            return new TableCellJson(cell.isSecret() ? "secret" : "text", cell.getText());
        }

    }

    /**
     * A piece of information, as an item of a card or a detail of a row. Values that are not
     * code are the rule, so only the ones that are say so. A piece of information without any
     * value is shown with a dash, which is not code.
     */
    record InfoJson(String label, List<String> values, @Nullable String icon, @Nullable Boolean code) {

        static final String NO_VALUE = "—";

        static InfoJson of(InfoItem item, ItemIcons icons) {
            boolean known = !item.getValues().isEmpty();
            return new InfoJson(item.getLabel(), known ? item.getValues() : List.of(NO_VALUE), icons.find(item.getIcon()),
                    (known && item.isCode()) ? true : null);
        }

    }

    /**
     * Finds the icon to show for a card or an item: the one it names, when there is one by
     * that name.
     */
    @FunctionalInterface
    interface ItemIcons {

        @Nullable String find(@Nullable String icon);

    }

    record CardJson(String id, String title, @Nullable String description, @Nullable String icon, @Nullable String documentation,
            @Nullable String propertiesPrefix, @Nullable StatusJson status, List<CardItemJson> items) {

        static CardJson of(Card card, String contextPath, ItemIcons icons) {
            return new CardJson(card.getId(), card.getTitle(), card.getDescription(), icons.find(card.getIcon()),
                    card.getDocumentation(), card.getPropertiesPrefix(), StatusJson.of(card.getStatus()),
                    card.getItems().stream().map(item -> CardItemJson.of(item, contextPath, icons)).toList());
        }

    }

    record StatusJson(String text, String level) {

        static @Nullable StatusJson of(@Nullable Status status) {
            return (status != null) ? new StatusJson(status.text(), status.level().name().toLowerCase(Locale.ROOT)) : null;
        }

    }

    record CardItemJson(String type, String label, @Nullable List<String> values, @Nullable String url, @Nullable String icon,
            @Nullable Boolean code) {

        static CardItemJson of(CardItem item, String contextPath, ItemIcons icons) {
            return switch (item) {
                case InfoItem infoItem -> {
                    InfoJson info = InfoJson.of(infoItem, icons);
                    yield new CardItemJson("info", info.label(), info.values(), null, info.icon(), info.code());
                }
                // A path of the application starts from its context path, which only a request
                // tells. A link item only takes a path that starts with a slash or a URL.
                case LinkItem linkItem -> new CardItemJson("link", linkItem.getLabel(), null,
                        linkItem.getUrl().startsWith("/") ? contextPath + linkItem.getUrl() : linkItem.getUrl(), null, null);
                case LogItem logItem -> new CardItemJson("log", logItem.getLabel(), null, null, null, null);
            };
        }

    }

    record ErrorResponseJson(ErrorJson error) {}

    record ErrorJson(int status, String message) {}

}
