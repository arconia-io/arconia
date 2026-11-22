import { html, nothing } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import '@vaadin/badge';
import './arconia-cards-page.js';

/**
 * Shows the overview of the application, the page the Dev UI opens on: what identifies the
 * application and the facts worth seeing at a glance, set apart from the rest, followed by
 * the details as cards.
 */
class ArconiaOverviewPage extends ArconiaElement {

    static properties = {
        page: { attribute: false },
    };

    render() {
        const application = this.page.application;
        return html`
            <section class="summary" aria-label="Summary">
                <div class="identity">
                    <h2>
                        ${application.name}
                        ${application.version ? html`<vaadin-badge>${application.version}</vaadin-badge>` : nothing}
                    </h2>
                    <p>Running in dev mode since ${application.startedAt}</p>
                </div>
                <dl>
                    ${application.facts.map((fact) => html`<div><dt>${fact.label}</dt><dd>${fact.value}</dd></div>`)}
                </dl>
            </section>
            <arconia-cards-page page-id=${this.page.id} .cards=${this.page.cards}></arconia-cards-page>
        `;
    }

}

customElements.define('arconia-overview-page', ArconiaOverviewPage);
