package io.arconia.dev.ui.api.page;

import io.arconia.core.annotations.Incubating;

/**
 * An item of a {@link Card}.
 */
@Incubating
public sealed interface CardItem permits InfoItem, LinkItem, LogItem {

    /**
     * The label of the item.
     */
    String getLabel();

}
