import { html } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import { getJson, RETRY_DELAY } from './api.js';
import './arconia-cards-page.js';
import './arconia-message.js';
import './arconia-overview-page.js';
import './arconia-table-page.js';

/**
 * The title the server gives the Dev UI, which names the application it belongs to.
 */
const BASE_TITLE = document.title;

/**
 * Shows one page of the Dev UI, loading its content from the API.
 */
class ArconiaPage extends ArconiaElement {

    static properties = {
        // Set by the router, with the identifier of the page as a parameter.
        location: { attribute: false },
        page: { state: true },
        failure: { state: true },
        // Whether the page could not be loaded because the application cannot be reached.
        waiting: { state: true },
    };

    willUpdate(changes) {
        if (changes.has('location')) {
            this.load(this.location.params.id);
        }
        if (changes.has('page')) {
            // The tab of the browser says which page it shows, so that several tabs of the
            // Dev UI, and the entries of the history, can be told apart.
            document.title = this.page ? `${this.page.title} · ${BASE_TITLE}` : BASE_TITLE;
        }
    }

    /**
     * The content of a page is a picture of the application at the time it was loaded. It is
     * loaded again when the developer comes back to the Dev UI from another tab or window, such
     * as the IDE next to it, where they may have changed or restarted the application.
     * Reloading the page in the browser does the same at any time.
     */
    connectedCallback() {
        super.connectedCallback();
        this.onFocus = () => {
            if (this.location) {
                this.load(this.location.params.id, { keepContent: true });
            }
        };
        window.addEventListener('focus', this.onFocus);
    }

    disconnectedCallback() {
        clearTimeout(this.retry);
        window.removeEventListener('focus', this.onFocus);
        // The page that takes its place, such as the one for an unknown address, may have no
        // title of its own.
        document.title = BASE_TITLE;
        super.disconnectedCallback();
    }

    /**
     * Loads the content of a page. When the page is already shown, its content can be kept
     * on screen until the new one has arrived, so that it doesn't flicker.
     */
    async load(id, { keepContent = false } = {}) {
        clearTimeout(this.retry);
        // Only the answer to the latest request counts, whatever order the answers come in.
        const request = this.request = (this.request ?? 0) + 1;
        if (!keepContent) {
            this.page = undefined;
            this.failure = undefined;
            this.waiting = false;
        }
        try {
            const page = await getJson(`pages/${encodeURIComponent(id)}`);
            if (request === this.request) {
                this.page = page;
                this.failure = undefined;
                this.waiting = false;
            }
        } catch (error) {
            // A page that is no longer shown has nothing to report and nothing to wait for.
            if (this.isConnected && request === this.request) {
                this.failure = error.message;
                this.waiting = !!error.unreachable;
                if (error.unreachable) {
                    // The application is most likely being restarted and will be back soon.
                    this.retry = setTimeout(() => this.load(id, { keepContent: true }), RETRY_DELAY);
                }
            }
        }
    }

    render() {
        if (this.waiting) {
            return html`<arconia-message waiting></arconia-message>`;
        }
        if (this.failure) {
            return html`<arconia-message error heading="This page cannot be shown" text=${this.failure}></arconia-message>`;
        }
        if (!this.page) {
            return html`<arconia-message plain text="Loading..."></arconia-message>`;
        }
        return html`
            <h1>${this.page.title}</h1>
            ${this.renderContent(this.page)}
        `;
    }

    renderContent(page) {
        switch (page.type) {
            case 'overview':
                return html`<arconia-overview-page .page=${page}></arconia-overview-page>`;
            case 'table':
                return html`<arconia-table-page .columns=${page.table.columns} .filterColumn=${page.table.filterColumn} .rows=${page.table.rows}></arconia-table-page>`;
            case 'cards':
                return html`<arconia-cards-page page-id=${page.id} style="--columns: ${page.columns}" .cards=${page.cards}></arconia-cards-page>`;
            default:
                return html`<arconia-message text="This version of the Dev UI cannot show this kind of page."></arconia-message>`;
        }
    }

}

customElements.define('arconia-page', ArconiaPage);
