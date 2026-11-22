import { html, nothing } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer, gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/select';
import '@vaadin/text-field';
import './arconia-info-list.js';
import { FILTER_PARAMETER, iconPath } from './paths.js';

/**
 * What the choice of every group has as its value. The select takes an item without a value
 * for one whose value is its label, so the choice needs a value of its own, one that no group
 * can have as its name.
 */
const ANY_GROUP = '\u0000';

/**
 * How the columns share the width of the table: most of it goes to the first one, which says
 * what a row is about, and to the second one, which usually holds its value.
 */
const COLUMN_GROWTH = [4, 3];

/**
 * Shows the table of a page. The developer can sort its rows by a column, filter them by the
 * text they contain and by the values of a column, open a row to see its details, and reveal
 * the cells that hold a secret.
 */
class ArconiaTablePage extends ArconiaElement {

    static properties = {
        columns: { attribute: false },
        rows: { attribute: false },
        // The position of the column whose values the rows can be filtered by, if any.
        filterColumn: { attribute: false },
        // The text the rows must contain to be shown, and the group they must belong to.
        filter: { state: true },
        group: { state: true },
        // The identifiers of the rows whose details are shown, and the secret cells whose text
        // is shown. The rows themselves are replaced each time the page is loaded again.
        opened: { state: true },
        revealed: { state: true },
    };

    constructor() {
        super();
        this.columns = [];
        this.rows = [];
        // The filter comes with the address of the page, so that a filtered table can be linked
        // to, bookmarked and reloaded.
        this.filter = new URLSearchParams(window.location.search).get(FILTER_PARAMETER) ?? '';
        this.group = ANY_GROUP;
        this.opened = new Set();
        this.revealed = new Set();
        this.shownRows = [];
        this.groupItems = [];
    }

    /**
     * The rows that pass the filters. The text of a secret cell is not searched, so that typing
     * doesn't give away what it holds.
     */
    willUpdate(changes) {
        if (changes.has('rows')) {
            // What each cell is sorted by: its text, whatever its case. The text of a secret
            // cell is left out, so that the order of the rows doesn't give away what it holds.
            for (const row of this.rows) {
                row.sort = row.cells.map((cell) => (cell.type === 'text' ? cell.text.toLowerCase() : ''));
            }
        }
        if (changes.has('rows') || changes.has('columns')) {
            const groups = this.filterColumn == null ? [] : [...new Set(this.rows.map((row) => this.groupOf(row)).filter(Boolean))];
            this.groupItems = groups.length > 1
                ? [{ label: `Any ${this.columns[this.filterColumn].toLowerCase()}`, value: ANY_GROUP }, ...groups.map((group) => ({ label: group, value: group }))]
                : [];
            // A group that the rows no longer have cannot be chosen, so it cannot stay chosen.
            if (!this.groupItems.some((item) => item.value === this.group)) {
                this.group = ANY_GROUP;
            }
        }
        if (changes.has('rows') || changes.has('filter') || changes.has('group')) {
            const filter = this.filter.trim().toLowerCase();
            this.shownRows = this.rows.filter((row) => (this.group === ANY_GROUP || this.groupOf(row) === this.group)
                && (!filter || row.cells.some((cell) => cell.type === 'text' && cell.text.toLowerCase().includes(filter))));
        }
    }

    /**
     * Activating a row opens its details, and activating it again closes them. The table
     * activates a row on a click and on the Space key, but not when the click is on a button
     * in the row or in the details themselves.
     */
    toggle(row) {
        if (!row?.details) {
            return;
        }
        const opened = new Set(this.opened);
        if (!opened.delete(row.id)) {
            opened.add(row.id);
        }
        this.opened = opened;
    }

    /**
     * The Enter key opens and closes a row too, as the Space key does. The table itself would
     * use it to move into a cell. A button in a cell keeps the key for itself.
     */
    toggleOnEnter(event) {
        if (event.key !== 'Enter' || event.composedPath().some((element) => element.localName === 'vaadin-button')) {
            return;
        }
        const context = event.currentTarget.getEventContext(event);
        if (context.section === 'body' && context.item) {
            event.stopPropagation();
            this.toggle(context.item);
        }
    }

    toggleSecret(key) {
        const revealed = new Set(this.revealed);
        if (!revealed.delete(key)) {
            revealed.add(key);
        }
        this.revealed = revealed;
    }

    render() {
        const rows = this.shownRows;
        return html`
            <div class="toolbar">
                <vaadin-text-field placeholder="Filter" accessible-name="Filter the rows of the table" clear-button-visible
                        .value=${this.filter} @value-changed=${(event) => this.setFilter(event.detail.value)}>
                    <vaadin-icon slot="prefix" src=${iconPath('search')}></vaadin-icon>
                </vaadin-text-field>
                ${this.groupItems.length ? html`
                    <vaadin-select accessible-name=${this.columns[this.filterColumn]} .value=${this.group}
                            .items=${this.groupItems}
                            @value-changed=${(event) => { this.group = event.detail.value; }}>
                    </vaadin-select>
                ` : nothing}
                <span class="count">${rows.length === this.rows.length ? `${rows.length} rows` : `${rows.length} of ${this.rows.length} rows`}</span>
            </div>
            <vaadin-grid .items=${rows} item-id-path="id" .detailsOpenedItems=${rows.filter((row) => this.opened.has(row.id))}
                    @cell-activate=${(event) => this.toggle(event.detail.model.item)}
                    @keydown=${{ handleEvent: (event) => this.toggleOnEnter(event), capture: true }}
                    ${gridRowDetailsRenderer((row) => this.renderDetails(row), [])}>
                <span slot="empty-state">${this.rows.length ? 'No rows match the filter.' : 'There is nothing to show in this table.'}</span>
                <vaadin-grid-column width="2.75rem" flex-grow="0" frozen
                        ${columnBodyRenderer((row) => this.renderToggle(row), [this.opened])}></vaadin-grid-column>
                ${this.columns.map((column, index) => html`
                    <vaadin-grid-sort-column header=${column} path="sort.${index}" resizable flex-grow=${COLUMN_GROWTH[index] ?? 1}
                            ${columnBodyRenderer((row) => this.renderCell(row, index), [this.revealed])}></vaadin-grid-sort-column>
                `)}
            </vaadin-grid>
        `;
    }

    /**
     * What a row has in the column the page says the rows can be filtered by.
     */
    groupOf(row) {
        const cell = row.cells[this.filterColumn];
        return cell?.type === 'text' ? cell.text : undefined;
    }

    /**
     * Filters the rows by a text, which is also kept in the address of the page, in place of
     * the one it had.
     */
    setFilter(filter) {
        if (filter === this.filter) {
            return;
        }
        this.filter = filter;
        const url = new URL(window.location.href);
        if (filter) {
            url.searchParams.set(FILTER_PARAMETER, filter);
        } else {
            url.searchParams.delete(FILTER_PARAMETER);
        }
        window.history.replaceState(window.history.state, '', url);
    }

    renderToggle(row) {
        if (!row.details) {
            return nothing;
        }
        const opened = this.opened.has(row.id);
        return html`<vaadin-icon class="toggle" src=${iconPath(opened ? 'chevron-down' : 'chevron-right')}
                aria-label=${opened ? 'Details shown' : 'Details hidden'}></vaadin-icon>`;
    }

    renderCell(row, index) {
        const cell = row.cells[index];
        if (!cell) {
            return nothing;
        }
        // Only a cell known to be plain text is shown as it is. Any other kind is hidden like a
        // secret, so that a kind this page doesn't know yet is never shown by mistake.
        if (cell.type === 'text') {
            if (index === 0) {
                // The first column says what the row is about, and is always shown in full. A
                // long name continues on the next line, preferably after one of its dots.
                const parts = cell.text.split('.');
                return html`<span class="name">${parts.map((part, position) => (position ? html`.<wbr>${part}` : part))}</span>`;
            }
            return html`<span title=${cell.text}>${cell.text}</span>`;
        }
        const key = `${row.id}\n${index}`;
        const revealed = this.revealed.has(key);
        return html`
            <span class="secret">
                ${revealed ? html`<span>${cell.text}</span>` : html`<span aria-hidden="true">••••••••</span><span class="visually-hidden">Hidden value</span>`}
                <vaadin-button theme="tertiary icon" aria-label=${revealed ? 'Hide the value' : 'Show the value'}
                        title=${revealed ? 'Hide the value' : 'Show the value'} @click=${() => this.toggleSecret(key)}>
                    <vaadin-icon src=${iconPath(revealed ? 'eye-off' : 'eye')}></vaadin-icon>
                </vaadin-button>
            </span>
        `;
    }

    /**
     * What is shown below a row when it is opened, as the server decides: the text of the cells
     * too long to be read in the table, and the details the row comes with.
     */
    renderDetails(row) {
        return html`<arconia-info-list class="details" .items=${row.details ?? []}></arconia-info-list>`;
    }

}

customElements.define('arconia-table-page', ArconiaTablePage);
