import { html, nothing } from 'lit';
import '@vaadin/icon';
import { ArconiaElement } from './arconia-element.js';
import { iconPath } from './paths.js';

/**
 * Shows pieces of information, each with its label: the information of a card, or the details
 * of a row of a table.
 * <p>
 * A label can have an icon before it. When only some of the labels have one, the others keep
 * its space, so that the labels stay aligned. Values that are code are in a fixed-width font,
 * and several values of one piece of information are laid out side by side.
 */
class ArconiaInfoList extends ArconiaElement {

    static properties = {
        items: { attribute: false },
    };

    render() {
        const items = this.items ?? [];
        const icons = items.some((item) => item.icon);
        return html`
            <dl>
                ${items.map((item) => html`
                    <dt>${this.renderIcon(item, icons)}${item.label}</dt>
                    <dd class=${item.code ? 'code' : nothing}>${this.renderValues(item.values)}</dd>
                `)}
            </dl>
        `;
    }

    renderIcon(item, icons) {
        if (item.icon) {
            return html`<vaadin-icon src=${iconPath(item.icon)} aria-hidden="true"></vaadin-icon>`;
        }
        return icons ? html`<span class="no-icon"></span>` : nothing;
    }

    /**
     * Several values wrap as whole values, so that a value such as a port mapping is never
     * split over two lines.
     */
    renderValues(values) {
        if (values.length === 1) {
            return values[0];
        }
        return html`<span class="values">${values.map((value) => html`<span>${value}</span>`)}</span>`;
    }

}

customElements.define('arconia-info-list', ArconiaInfoList);
