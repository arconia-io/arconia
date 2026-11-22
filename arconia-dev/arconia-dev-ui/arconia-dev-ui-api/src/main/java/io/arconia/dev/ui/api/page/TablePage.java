package io.arconia.dev.ui.api.page;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A page of the Dev UI that shows a table, for what comes in large numbers, such as the
 * properties of an application. The developer can filter the rows by the text they contain.
 */
@Incubating
public final class TablePage implements DevUiPage {

    private static final String DEFAULT_ICON = "table";

    private final String id;

    private final String title;

    private final String icon;

    private final int order;

    @Nullable
    private final DevUiSection section;

    private final List<String> columns;

    @Nullable
    private final String filterColumn;

    private final Supplier<List<TableRow>> rows;

    private TablePage(Builder builder) {
        this.id = Names.validatePageId(builder.id);
        Assert.hasText(builder.title, "title cannot be null or empty");
        Assert.notEmpty(builder.columns, "columns cannot be null or empty");
        builder.columns.forEach(column -> Assert.hasText(column, "columns cannot contain null or empty elements"));
        Assert.isTrue(new HashSet<>(builder.columns).size() == builder.columns.size(), "columns cannot contain the same title twice");
        Assert.isTrue(builder.filterColumn == null || builder.columns.contains(builder.filterColumn),
                () -> "filterColumn '%s' is not one of the columns".formatted(builder.filterColumn));
        Assert.notNull(builder.rows, "rows cannot be null");
        this.title = builder.title;
        this.icon = Names.validate(builder.icon, "icon");
        this.order = builder.order;
        this.section = builder.section;
        this.columns = List.copyOf(builder.columns);
        this.filterColumn = builder.filterColumn;
        this.rows = builder.rows;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    @Nullable
    public DevUiSection getSection() {
        return section;
    }

    /**
     * The titles of the columns of the table.
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * The column whose values the developer can choose from to see the matching rows only,
     * if any.
     */
    @Nullable
    public String getFilterColumn() {
        return filterColumn;
    }

    /**
     * Supplies the rows of the table, called whenever the Dev UI needs them.
     */
    public Supplier<List<TableRow>> getRows() {
        return rows;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TablePage}, with a generic icon, the order zero, and no column to filter
     * by, by default.
     */
    public static final class Builder {

        @Nullable
        private String id;
        @Nullable
        private String title;
        private String icon = DEFAULT_ICON;
        private int order;
        @Nullable
        private DevUiSection section;
        @Nullable
        private List<String> columns;
        @Nullable
        private String filterColumn;
        @Nullable
        private Supplier<List<TableRow>> rows;

        private Builder() {}

        /**
         * The identifier of the page, unique among the pages of the Dev UI and used in its
         * URL. It can only contain lowercase letters, digits, and single hyphens between them. It
         * cannot be {@code overview}, which is the identifier of the page the Dev UI opens on.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The title of the page, shown in the navigation.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * The icon of the page, shown next to its title in the navigation. It is the name of
         * an icon of the <a href="https://lucide.dev/icons">Lucide</a> icon set, such as
         * {@code database}.
         */
        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * The position of the page in the navigation, or among the pages of its section when
         * it has one. Pages with a lower value come first, and pages with the same value are
         * sorted by title.
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * The section of the navigation the page is listed in, if any. A page without a
         * section is listed on its own.
         */
        public Builder section(@Nullable DevUiSection section) {
            this.section = section;
            return this;
        }

        /**
         * The titles of the columns of the table.
         * Put what a row is about first: the first column is given the most room and is always
         * shown in full. Rows are sorted as text, and a text too long for its cell is shown in
         * full with the details of its row.
         */
        public Builder columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        /**
         * The titles of the columns of the table, as for {@link #columns(List)}.
         */
        public Builder columns(String... columns) {
            Assert.notNull(columns, "columns cannot be null");
            return columns(Arrays.asList(columns));
        }

        /**
         * The column whose values the developer can choose from to see the matching rows
         * only, if any. It suits a column with a few values that many rows share, such as a
         * status. The developer can always filter the rows by the text they contain.
         */
        public Builder filterColumn(@Nullable String filterColumn) {
            this.filterColumn = filterColumn;
            return this;
        }

        /**
         * Supplies the rows of the table, in the order they are shown. The supplier is called
         * whenever the Dev UI needs the rows and never when the application starts, so it can
         * read the state of the application. That can be several times while someone looks at
         * the page, so the supplier should be quick and change nothing in the application.
         * <p>
         * The supplier runs on the thread of the request that needs the rows, and two requests
         * can call it at the same time, so it must be safe to call from several threads. When
         * it fails, or returns {@code null}, the failure is logged and shown in place of the
         * page, and the application is not affected.
         */
        public Builder rows(Supplier<List<TableRow>> rows) {
            this.rows = rows;
            return this;
        }

        public TablePage build() {
            return new TablePage(this);
        }

    }

}
