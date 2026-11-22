import { html, nothing } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import { UNREACHABLE, WAITING_HEADING } from './api.js';

/**
 * A message shown in place of content: nothing to show, something not found, or a failure.
 * A plain message has no panel and no mark, for what is only shown for a moment. A waiting
 * message says that the application cannot be reached and is asked again. A not-found message
 * says that there is no page at the address, whether the address cannot be one of a page or
 * the application has no page there.
 * <p>
 * A message with a heading stands in for a whole page, so its heading is the one of the page.
 */
class ArconiaMessage extends ArconiaElement {

    static properties = {
        heading: { type: String },
        text: { type: String },
        error: { type: Boolean, reflect: true },
        plain: { type: Boolean, reflect: true },
        waiting: { type: Boolean },
        notFound: { type: Boolean, attribute: 'not-found' },
    };

    render() {
        if (this.waiting) {
            return html`<h1>${WAITING_HEADING}</h1><p>${UNREACHABLE} Trying again...</p>`;
        }
        if (this.notFound) {
            return html`<h1>Page not found</h1><p>There is no page at this address.</p>`;
        }
        return html`
            ${this.heading ? html`<h1>${this.heading}</h1>` : nothing}
            <p>${this.text}</p>
        `;
    }

}

customElements.define('arconia-message', ArconiaMessage);
