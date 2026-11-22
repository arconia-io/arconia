import { html, nothing } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import { Router } from '@vaadin/router';
import '@vaadin/app-layout';
import '@vaadin/app-layout/vaadin-drawer-toggle.js';
import '@vaadin/badge';
import '@vaadin/button';
import '@vaadin/icon';
import { Notification } from '@vaadin/notification';
import '@vaadin/side-nav';
import '@vaadin/side-nav/vaadin-side-nav-item.js';
import { getJson, post, reloadedAfterRestart, RETRY_DELAY } from './api.js';
import { colorScheme, nextColorScheme } from './color-scheme.js';
import { applicationLogs, applicationName, applicationRestart, applicationVersion, basePath, iconPath, pagePath } from './paths.js';
import './arconia-console.js';
import './arconia-message.js';
import './arconia-page.js';

/**
 * Where the browser remembers the sections of the navigation that the developer closed.
 */
const NAVIGATION_KEY = 'arconia-dev-ui-navigation';

function readCollapsedSections() {
    try {
        return new Set(JSON.parse(localStorage.getItem(NAVIGATION_KEY)) ?? []);
    } catch {
        return new Set();
    }
}

/**
 * How long to wait, in milliseconds, for a restarted application to come back before telling
 * the developer.
 */
const RESTART_TIMEOUT = 60000;

/**
 * How long, in milliseconds, what the Dev UI has to tell the developer stays on screen.
 */
const NOTICE_DURATION = 6000;

/**
 * How the button in the top bar shows each choice of color scheme. It shows the current
 * choice, and a click moves to the next one.
 */
const COLOR_SCHEMES = {
    system: { icon: 'monitor', label: 'Color scheme: system. Click to use the light one.' },
    light: { icon: 'sun', label: 'Color scheme: light. Click to use the dark one.' },
    dark: { icon: 'moon', label: 'Color scheme: dark. Click to follow the system.' },
};

function message(heading, text) {
    const element = document.createElement('arconia-message');
    element.heading = heading;
    element.text = text;
    return element;
}

/**
 * The root element of the Dev UI: the top bar, the navigation, and the area where the pages
 * are shown.
 */
class ArconiaDevUi extends ArconiaElement {

    static properties = {
        // What the navigation lists, as the server gives it: pages, and sections with their pages.
        navigation: { state: true },
        failure: { state: true },
        // Whether the pages could not be loaded because the application cannot be reached.
        waiting: { state: true },
        location: { state: true },
        colorScheme: { state: true },
        // Whether a restart of the application was asked for and is not over yet.
        restarting: { state: true },
        // The sections of the navigation that the developer closed.
        collapsedSections: { state: true },
    };

    constructor() {
        super();
        this.navigation = [];
        this.colorScheme = colorScheme();
        this.collapsedSections = readCollapsedSections();
        this.updateLocation = (event) => {
            this.location = event.detail.location;
        };
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('vaadin-router-location-changed', this.updateLocation);
    }

    disconnectedCallback() {
        window.removeEventListener('vaadin-router-location-changed', this.updateLocation);
        super.disconnectedCallback();
    }

    async firstUpdated() {
        if (reloadedAfterRestart()) {
            this.tell('The application was restarted.');
        }
        if (!await this.loadPages()) {
            return;
        }
        const router = new Router(this.renderRoot.querySelector('main'), { baseUrl: `${basePath}/` });
        router.setRoutes([
            { path: '/', action: (context, commands) => this.home(commands) },
            { path: '/pages/:id', component: 'arconia-page' },
            { path: '(.*)', action: () => message('Page not found', 'There is no page at this address.') },
        ]);
    }

    /**
     * Loads the list of the pages, which the navigation is built from. When
     * the application cannot be reached, as while it is being restarted, it is asked again
     * until it answers. Any other failure is shown to the developer, and the list is not
     * loaded.
     */
    async loadPages() {
        for (;;) {
            try {
                this.navigation = (await getJson('pages')).navigation;
                this.failure = undefined;
                this.waiting = false;
                // The area where the pages are shown takes the place of the failure.
                await this.updateComplete;
                return true;
            } catch (error) {
                this.failure = error.message;
                this.waiting = !!error.unreachable;
                if (!error.unreachable) {
                    return false;
                }
                await new Promise((resolve) => { setTimeout(resolve, RETRY_DELAY); });
            }
        }
    }

    /**
     * Restarts the application. Once it is back, any request finds out and the page is loaded
     * again, so the Dev UI keeps asking until then. When the application doesn't come back,
     * the developer is told after a while.
     */
    async restart() {
        this.restarting = true;
        try {
            await post('actions/restart');
        } catch (error) {
            this.restarting = false;
            this.tell(`The application could not be restarted. ${error.message}`);
            return;
        }
        const deadline = Date.now() + RESTART_TIMEOUT;
        const check = async () => {
            try {
                await getJson('pages');
            } catch { /* The application is not back yet. */ }
            if (Date.now() < deadline) {
                setTimeout(check, RETRY_DELAY);
            } else {
                this.restarting = false;
                this.tell('The application has not come back yet. Have a look at its logs.');
            }
        };
        setTimeout(check, RETRY_DELAY);
    }

    renderPageItem(page, inSection = false) {
        return html`
            <vaadin-side-nav-item slot=${inSection ? 'children' : nothing} path=${pagePath(page.id)}>
                <vaadin-icon slot="prefix" src=${iconPath(page.icon)}></vaadin-icon>
                ${page.title}
            </vaadin-side-nav-item>
        `;
    }

    /**
     * A section is open until the developer closes it, which the browser remembers. Opening
     * a page of a closed section opens it again.
     */
    rememberSection(id, expanded) {
        if (expanded === !this.collapsedSections.has(id)) {
            return;
        }
        const collapsed = new Set(this.collapsedSections);
        if (expanded) {
            collapsed.delete(id);
        } else {
            collapsed.add(id);
        }
        this.collapsedSections = collapsed;
        try {
            localStorage.setItem(NAVIGATION_KEY, JSON.stringify([...collapsed]));
        } catch { /* The choice lasts until the page is closed. */ }
    }

    tell(notice) {
        Notification.show(notice, { position: 'bottom-center', duration: NOTICE_DURATION });
    }

    /**
     * The Dev UI opens on the overview of the application.
     */
    home(commands) {
        return commands.redirect('/pages/overview');
    }

    render() {
        return html`
            <vaadin-app-layout>
                <vaadin-drawer-toggle slot="navbar" theme="tertiary"></vaadin-drawer-toggle>
                <a slot="navbar" class="arconia-brand" href="${basePath}/" aria-label="Arconia Dev UI home">
                    <span class="arconia-brand-logo" role="img" aria-label="Arconia"></span>
                    <span class="arconia-brand-product">Dev UI</span>
                </a>
                <span slot="navbar" class="application" title="The application this Dev UI belongs to">
                    <span class="name">${applicationName}</span>
                    ${applicationVersion ? html`<vaadin-badge class="aura-accent-neutral">${applicationVersion}</vaadin-badge>` : nothing}
                </span>
                ${applicationRestart ? html`
                    <vaadin-button slot="navbar" class="restart" theme="tertiary icon" ?disabled=${this.restarting}
                            aria-label="Restart the application" title="Restart the application" @click=${() => this.restart()}>
                        <vaadin-icon class=${this.restarting ? 'spinning' : ''} src=${iconPath('rotate-cw')}></vaadin-icon>
                    </vaadin-button>
                ` : nothing}
                <vaadin-button slot="navbar" class="color-scheme" theme="tertiary icon"
                        aria-label=${COLOR_SCHEMES[this.colorScheme].label} title=${COLOR_SCHEMES[this.colorScheme].label}
                        @click=${() => { this.colorScheme = nextColorScheme(); }}>
                    <vaadin-icon src=${iconPath(COLOR_SCHEMES[this.colorScheme].icon)}></vaadin-icon>
                </vaadin-button>
                <vaadin-side-nav slot="drawer" .location=${this.location}>
                    ${this.navigation.map((entry) => entry.section ? html`
                        <vaadin-side-nav-item ?expanded=${!this.collapsedSections.has(entry.section.id)}
                                ?holds-current=${entry.pages.some((page) => pagePath(page.id) === this.location?.pathname)}
                                @expanded-changed=${(event) => this.rememberSection(entry.section.id, event.detail.value)}>
                            <vaadin-icon slot="prefix" src=${iconPath(entry.section.icon)}></vaadin-icon>
                            ${entry.section.title}
                            ${entry.pages.map((page) => this.renderPageItem(page, true))}
                        </vaadin-side-nav-item>
                    ` : this.renderPageItem(entry.page))}
                </vaadin-side-nav>
                ${this.failure
                    ? this.waiting
                        ? html`<arconia-message waiting></arconia-message>`
                        : html`<arconia-message error heading="The Dev UI cannot be loaded" text=${this.failure}></arconia-message>`
                    : html`<main></main>`}
                ${applicationLogs ? html`<arconia-console></arconia-console>` : nothing}
            </vaadin-app-layout>
        `;
    }

}

customElements.define('arconia-dev-ui', ArconiaDevUi);
