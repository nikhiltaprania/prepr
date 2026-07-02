// Hash router (~50 lines). Maps `#/path` patterns to view functions, guards auth,
// toggles the layout, and runs the previous view's teardown before rendering the next.

import { isAuthed } from "./store.js";
import { setActiveNav } from "../ui/shell.js";

const routes = [];          // { regex, keys, view, nav, public, focus }
let teardown = null;        // cleanup fn returned by the active view

function compile(route) {
    const keys = [];
    const pattern = route.path.replace(/:[^/]+/g, (m) => { keys.push(m.slice(1)); return "([^/]+)"; });
    return { ...route, regex: new RegExp("^" + pattern + "$"), keys };
}

export function register(list) { list.forEach((r) => routes.push(compile(r))); }

const outlet = () => document.getElementById("outlet");

async function resolve() {
    const path = location.hash.replace(/^#/, "") || "/";

    let match = null;
    const params = {};
    for (const route of routes) {
        const m = route.regex.exec(path);
        if (m) { match = route; route.keys.forEach((k, i) => (params[k] = decodeURIComponent(m[i + 1]))); break; }
    }
    if (!match) { location.hash = "#/"; return; }
    if (!match.public && !isAuthed()) { location.hash = "#/login"; return; }

    if (teardown) { try { teardown(); } catch { /* ignore */ } teardown = null; }

    document.body.dataset.layout = match.public ? "auth" : match.focus ? "focus" : "app";
    setActiveNav(match.nav || null);
    window.scrollTo(0, 0);

    try {
        const result = await match.view(outlet(), params);
        teardown = typeof result === "function" ? result : null;
    } catch (err) {
        // A view crashing must not white-screen the app.
        console.error("View render failed:", err);
        outlet().innerHTML =
            '<div class="page"><div class="empty"><div class="empty__title">Something went wrong</div>' +
            '<p class="empty__body muted">This page failed to load. <a href="#/">Go home</a></p></div></div>';
    }
}

export function start() {
    window.addEventListener("hashchange", resolve);
    return resolve();
}

export function navigate(hash) { location.hash = hash; }
