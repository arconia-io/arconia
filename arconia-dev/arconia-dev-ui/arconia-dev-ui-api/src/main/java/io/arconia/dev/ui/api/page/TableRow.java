package io.arconia.dev.ui.api.page;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A row of a {@link TablePage}: one cell for each column of the table and, optionally, details
 * shown below the row when the developer opens it.
 */
@Incubating
public final class TableRow {

    private final String id;

    private final List<TableCell> cells;

    private final List<InfoItem> details;

    private TableRow(Builder builder) {
        Assert.hasText(builder.id, "id cannot be null or empty");
        Assert.notEmpty(builder.cells, () -> "cells of the row '%s' cannot be null or empty".formatted(builder.id));
        Assert.noNullElements(builder.cells, () -> "cells of the row '%s' cannot contain null elements".formatted(builder.id));
        Assert.notNull(builder.details, "details cannot be null");
        Assert.noNullElements(builder.details, () -> "details of the row '%s' cannot contain null elements".formatted(builder.id));
        this.id = builder.id;
        this.cells = List.copyOf(builder.cells);
        this.details = List.copyOf(builder.details);
    }

    /**
     * The identifier of the row, which must be unique among the rows of its table. It is what the row is
     * about, such as the name of a property.
     */
    public String getId() {
        return id;
    }

    /**
     * The cells of the row, one for each column of the table and in the same order.
     */
    public List<TableCell> getCells() {
        return cells;
    }

    /**
     * What is shown below the row when the developer opens it. A row without details can only
     * be opened when one of its cells holds a text too long to be shown in full.
     */
    public List<InfoItem> getDetails() {
        return details;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TableRow}, with no details by default.
     */
    public static final class Builder {

        @Nullable
        private String id;
        @Nullable
        private List<? extends TableCell> cells;
        private List<? extends InfoItem> details = List.of();

        private Builder() {}

        /**
         * The identifier of the row, which must be unique among the rows of its table. It is what the row
         * is about, such as the name of a property.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The cells of the row, one for each column of the table and in the same order.
         */
        public Builder cells(List<? extends TableCell> cells) {
            this.cells = cells;
            return this;
        }

        /**
         * The texts of the cells of the row, one for each column of the table and in the same
         * order, for a row that has no other kind of cell.
         */
        public Builder cells(String... texts) {
            Assert.notNull(texts, "texts cannot be null");
            Assert.noNullElements(texts, "texts cannot contain null elements");
            this.cells = Arrays.stream(texts).map(TableCell::text).toList();
            return this;
        }

        /**
         * What is shown below the row when the developer opens it, each piece of information
         * with its label.
         */
        public Builder details(List<? extends InfoItem> details) {
            this.details = details;
            return this;
        }

        public TableRow build() {
            return new TableRow(this);
        }

    }

}
