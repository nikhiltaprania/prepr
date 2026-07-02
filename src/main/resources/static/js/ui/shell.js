// The persistent app frame: sidebar (nav) + topbar (start-interview, theme toggle, avatar) + #outlet
// + footer. Rendered once at boot; the router only swaps #outlet and toggles the active nav item.
// The avatar shows the first letter of the logged-in user's name and updates live on profile rename.

import { html, raw, mount, on } from "../core/dom.js";
import { getDisplayName, clearSession, isAdmin, subscribe } from "../core/store.js";
import { openNewInterviewModal } from "../views/newInterview.js";

const icon = {
    home: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 11l9-8 9 8M5 10v10h14V10"/></svg>'),
    growth: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19V5M4 19h16M8 16l3-4 3 2 4-6"/></svg>'),
    assistant: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 5h16v11H8l-4 4z"/></svg>'),
    users: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 19v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 19v-1a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>'),
    questions: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16v13H8l-4 4zM9 9h6M9 12.5h4"/></svg>'),
    moon: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>'),
    sun: raw('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M19 5l-1.5 1.5M6.5 17.5L5 19"/></svg>'),
};

function navItem(name, path, label, ic) {
    return html`<a class="nav-item" data-nav="${name}" href="#${path}">${ic}<span>${label}</span></a>`;
}

function navItems() {
    return html`
        ${navItem("home", "/", "Home", icon.home)}
        ${navItem("growth", "/growth", "Growth", icon.growth)}
        ${navItem("assistant", "/assistant", "Assistant", icon.assistant)}
        ${isAdmin() ? navItem("admin-users", "/admin", "User management", icon.users) : ""}
        ${isAdmin() ? navItem("admin-questions", "/admin/questions", "Question bank", icon.questions) : ""}`;
}

function initial() { const n = getDisplayName(); return n ? n.trim().charAt(0).toUpperCase() : "U"; }
function themeIcon() { return document.documentElement.dataset.theme === "dark" ? icon.sun : icon.moon; }

function menuItems() {
    return html`
        <a class="menu__item" href="#/settings">Settings</a>
        <button class="menu__item" data-action="logout" type="button">Log out</button>`;
}

export function renderShell(appRoot) {
    mount(appRoot, html`
        <aside id="sidebar" class="sidebar">
            <div class="brand"><span class="brand__dot"></span><span class="brand__name">Prepr</span></div>
            <nav class="nav" id="nav" aria-label="Primary">${navItems()}</nav>
        </aside>

        <header id="topbar" class="topbar">
            <div class="topbar__spacer"></div>
            <button class="btn btn--primary btn--sm" data-action="start-interview">Start interview</button>
            <button class="icon-btn" id="theme-toggle" data-action="toggle-theme" aria-label="Toggle theme" title="Toggle theme">${themeIcon()}</button>
            <button class="avatar avatar--btn" id="user-initial" data-action="toggle-menu"
                    aria-label="Account menu" aria-haspopup="true" title="Account">${initial()}</button>
            <div class="menu" id="user-menu" hidden>${menuItems()}</div>
        </header>

        <div class="main-wrap">
            <main id="outlet" class="outlet" tabindex="-1"></main>
            <footer class="footer">
                <span>© ${new Date().getFullYear()} Prepr · Designed by Nikhil Kumar</span>
                <nav class="footer__links" aria-label="Social">
                    <a href="#" rel="noopener">Portfolio</a>
                    <a href="#" rel="noopener">LinkedIn</a>
                    <a href="#" rel="noopener">Instagram</a>
                </nav>
            </footer>
        </div>
    `);

    wireShell(appRoot);

    // Keep the avatar initial + nav (admin items) in sync with the session (login, logout, role change).
    subscribe("session", () => {
        const i = document.getElementById("user-initial");
        if (i) i.textContent = initial();
        const nav = document.getElementById("nav");
        if (nav) mount(nav, navItems());
    });
}

function wireShell(appRoot) {
    on(appRoot, "click", "[data-action]", (_e, target) => {
        const action = target.dataset.action;
        if (action === "toggle-menu") {
            document.getElementById("user-menu").toggleAttribute("hidden");
        } else if (action === "logout") {
            clearSession();
            location.hash = "#/login";
        } else if (action === "start-interview") {
            openNewInterviewModal();
        } else if (action === "toggle-theme") {
            const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
            document.documentElement.dataset.theme = next;
            localStorage.setItem("prepr.theme", next);
            const btn = document.getElementById("theme-toggle");
            if (btn) mount(btn, themeIcon());
        }
    });

    document.addEventListener("click", (e) => {
        if (!e.target.closest("#user-menu") && !e.target.closest('[data-action="toggle-menu"]')) {
            document.getElementById("user-menu")?.setAttribute("hidden", "");
        }
    });
}

export function setActiveNav(name) {
    document.querySelectorAll(".nav-item").forEach((a) => {
        const active = a.dataset.nav === name;
        a.classList.toggle("is-active", active);
        if (active) a.setAttribute("aria-current", "page");
        else a.removeAttribute("aria-current");
    });
}
