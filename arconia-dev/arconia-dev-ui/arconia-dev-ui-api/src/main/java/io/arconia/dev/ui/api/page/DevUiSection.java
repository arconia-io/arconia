package io.arconia.dev.ui.api.page;

import io.arconia.core.annotations.Incubating;

/**
 * A section of the navigation of the Dev UI, which gathers the pages about one subject under
 * a title. A page says which section it belongs to, and a page without a section is listed on
 * its own.
 * <p>
 * The sections are the ones defined here.
 */
@Incubating
public final class DevUiSection {

    public static final DevUiSection CONFIGURATION = new DevUiSection("configuration", "Configuration", "settings", -100);

    private final String id;

    private final String title;

    private final String icon;

    private final int order;

    private DevUiSection(String id, String title, String icon, int order) {
        Names.validate(id, "id");
        Names.validate(icon, "icon");
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.order = order;
    }

    /**
     * The identifier of the section, unique among the sections of the Dev UI.
     */
    public String getId() {
        return id;
    }

    /**
     * The title of the section, shown in the navigation.
     */
    public String getTitle() {
        return title;
    }

    /**
     * The icon of the section, shown next to its title in the navigation. It is the name of
     * an icon of the <a href="https://lucide.dev/icons">Lucide</a> icon set.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * The position of the section in the navigation, among the pages without a section and
     * the other sections. The ones with a lower value come first.
     */
    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return title;
    }

}
