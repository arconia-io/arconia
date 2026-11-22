import { html, nothing } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import '@vaadin/badge';
import '@vaadin/button';
import '@vaadin/card';
import '@vaadin/dialog';
import '@vaadin/icon';
import { iconPath, logPath, propertiesPath } from './paths.js';
import './arconia-info-list.js';
import './arconia-log-view.js';
import './arconia-message.js';

/**
 * The levels of a status that have a color of their own, and how they are said.
 */
const STATUS_LEVELS = { success: 'OK', warning: 'warning', error: 'error' };

/**
 * Shows the cards of a page.
 */
class ArconiaCardsPage extends ArconiaElement {

    static properties = {
        // The identifier of the page the cards belong to.
        pageId: { type: String, attribute: 'page-id' },
        cards: { attribute: false },
        // The log shown in the dialog, if any: its card and the label of its item.
        log: { state: true },
    };

    render() {
        if (!this.cards?.length) {
            return html`<arconia-message text="There is nothing to show on this page."></arconia-message>`;
        }
        return html`
            ${this.cards.map((card) => this.renderCard(card))}
            ${this.renderLogDialog()}
        `;
    }

    /**
     * The log is followed while the dialog is open, and no longer once it is closed.
     */
    renderLogDialog() {
        const close = () => { this.log = undefined; };
        return html`
            <vaadin-dialog header-title=${this.log ? `${this.log.card.title}: ${this.log.item}` : ''}
                    .opened=${!!this.log} resizable draggable
                    width="min(72rem, 90vw)" height="75vh"
                    @opened-changed=${(event) => { if (!event.detail.value) close(); }}>
                <vaadin-button slot="header-content" theme="tertiary icon" aria-label="Close" @click=${close}>
                    <vaadin-icon src=${iconPath('x')}></vaadin-icon>
                </vaadin-button>
                ${this.log ? html`
                    <arconia-log-view url=${logPath(this.pageId, this.log.card.id, this.log.item)}></arconia-log-view>
                ` : nothing}
            </vaadin-dialog>
        `;
    }

    /**
     * The header of a card has its icon, its title and its status. Its description follows, as
     * wide as the card, then its information, and its links, in a row. The footer of the card,
     * below a line, has what every card has in the same place: what there is to read about it
     * at the start, as icons, and its logs at the end, as buttons.
     */
    renderCard(card) {
        const information = card.items.filter((item) => item.type === 'info');
        const links = card.items.filter((item) => item.type === 'link');
        const logs = card.items.filter((item) => item.type === 'log');
        return html`
            <vaadin-card card-title=${card.title}>
                ${card.icon ? html`<vaadin-icon slot="header-prefix" src=${iconPath(card.icon)} aria-hidden="true"></vaadin-icon>` : nothing}
                ${card.status ? this.renderStatus(card.status) : nothing}
                ${card.description ? html`<p class="description">${card.description}</p>` : nothing}
                ${information.length ? html`<arconia-info-list .items=${information}></arconia-info-list>` : nothing}
                ${links.length ? html`<div class="links">${links.map((item) => this.renderLink(item))}</div>` : nothing}
                ${card.documentation || card.propertiesPrefix || logs.length ? html`
                    <div class="footer">
                        ${card.documentation ? html`
                            <a class="about" href=${card.documentation} target="_blank" rel="noopener noreferrer"
                                    title="Documentation" aria-label="Documentation of ${card.title} (opens in a new tab)">
                                <vaadin-icon src=${iconPath('book-open')} aria-hidden="true"></vaadin-icon>
                            </a>
                        ` : nothing}
                        ${card.propertiesPrefix ? html`
                            <a class="about" href=${propertiesPath(card.propertiesPrefix)}
                                    title="Properties" aria-label="Properties of ${card.title}">
                                <vaadin-icon src=${iconPath('sliders-horizontal')} aria-hidden="true"></vaadin-icon>
                            </a>
                        ` : nothing}
                        <div class="logs">${logs.map((item) => this.renderLog(card, item))}</div>
                    </div>
                ` : nothing}
            </vaadin-card>
        `;
    }

    /**
     * The status of a card, at the end of its header, as a badge. Its level is one of the
     * variants of the badge, except for the neutral one, which is the neutral accent color of
     * the theme. What the color says is also said in words for assistive technologies, since
     * the text of a status, such as "Exited", doesn't always tell whether it is a problem.
     */
    renderStatus(status) {
        const level = STATUS_LEVELS[status.level];
        return html`
            <vaadin-badge slot="header-suffix" class=${level ? nothing : 'aura-accent-neutral'} theme=${level ? status.level : nothing}>
                ${status.text}${level ? html`<span class="visually-hidden">, ${level}</span>` : nothing}
            </vaadin-badge>
        `;
    }

    renderLink(item) {
        return html`
            <a href=${item.url} target="_blank" rel="noopener noreferrer">
                <vaadin-icon src=${iconPath('external-link')} aria-hidden="true"></vaadin-icon>
                ${item.label}
                <span class="visually-hidden">(opens in a new tab)</span>
            </a>
        `;
    }

    renderLog(card, item) {
        return html`
            <vaadin-button @click=${() => { this.log = { card, item: item.label }; }}>
                <vaadin-icon slot="prefix" src=${iconPath('scroll-text')}></vaadin-icon>
                ${item.label}
            </vaadin-button>
        `;
    }

}

customElements.define('arconia-cards-page', ArconiaCardsPage);
