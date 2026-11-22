package io.arconia.dev.ui.api.page;

import java.util.Objects;

import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * An item of a {@link Card} that opens a log and follows it. Within a page, the identifier
 * of the card together with the label of the item identifies the log.
 */
@Incubating
public final class LogItem implements CardItem {

    private final String label;

    private final LogSource source;

    private LogItem(String label, LogSource source) {
        Assert.hasText(label, "label cannot be null or empty");
        Assert.notNull(source, () -> "source of the log '%s' cannot be null".formatted(label));
        this.label = label;
        this.source = source;
    }

    /**
     * Creates an item that opens the given log.
     *
     * @param label the label of the item (e.g. {@code Logs})
     * @param source the log to follow
     */
    public static LogItem of(String label, LogSource source) {
        return new LogItem(label, source);
    }

    /**
     * The label of the item (e.g. {@code Logs}).
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * The log to follow.
     */
    public LogSource getSource() {
        return source;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LogItem item && this.label.equals(item.label) && this.source.equals(item.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, source);
    }

    @Override
    public String toString() {
        return "LogItem[label=%s]".formatted(label);
    }

}
