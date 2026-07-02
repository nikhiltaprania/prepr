// Reusable presentational components: pure functions (props -> Html). No side effects.
// Add components here as screens need them (statCard, scoreRing, chatBubble… arrive in later milestones).

import { html, raw } from "../core/dom.js";

export function button({ label, variant = "primary", size = "md", action = "", href = null, type = "button" }) {
    const cls = `btn btn--${variant} btn--${size}`;
    return href != null
        ? html`<a class="${cls}" href="${href}">${label}</a>`
        : html`<button class="${cls}" type="${type}" data-action="${action}">${label}</button>`;
}

export function card(content, { title = null } = {}) {
    return html`<section class="card">${title ? html`<h3 class="card__title">${title}</h3>` : ""}${content}</section>`;
}

export function emptyState({ title, body = "", actionLabel = null, action = "" }) {
    return html`
        <div class="empty">
            <div class="empty__title">${title}</div>
            ${body ? html`<p class="empty__body muted">${body}</p>` : ""}
            ${actionLabel ? button({ label: actionLabel, action }) : ""}
        </div>`;
}

export function skeleton(lines = 3) {
    return html`<div class="skeleton">${Array.from({ length: lines }).map(() => html`<div class="skeleton__line"></div>`)}</div>`;
}

/** Labelled text/email/password/number input. Value is escaped by the html`` template. */
export function field({ name, label, type = "text", value = "", placeholder = "", required = false, autocomplete = "off" }) {
    return html`
        <label class="field">
            <span>${label}</span>
            <input name="${name}" type="${type}" value="${value ?? ""}" placeholder="${placeholder}"
                   autocomplete="${autocomplete}" ${required ? raw("required") : ""}/>
        </label>`;
}

/** Labelled multi-line input. */
export function textArea({ name, label, value = "", rows = 3 }) {
    return html`
        <label class="field">
            <span>${label}</span>
            <textarea name="${name}" rows="${rows}">${value ?? ""}</textarea>
        </label>`;
}

/** Inline form error / notice banner. */
export function alert(message, variant = "error") {
    return message ? html`<div class="alert alert--${variant}" role="alert">${message}</div>` : "";
}

/** Turn an enum-ish token (JAVA_BACKEND) into a label (Java Backend). */
export function humanize(s) {
    return String(s || "").toLowerCase().split(/[_\s]+/).filter(Boolean)
        .map((w) => w[0].toUpperCase() + w.slice(1)).join(" ");
}

export function pill(label, variant = "neutral") {
    return html`<span class="pill pill--${variant}">${label}</span>`;
}

export function statusPill(status) {
    return pill(humanize(status), statusVariant(status));
}

/** Variant for any interview/document status. */
export function statusVariant(status) {
    switch (status) {
        case "COMPLETED": case "PROCESSED": return "good";
        case "IN_PROGRESS": return "info";
        case "PROCESSING": return "warn";
        case "FAILED": return "bad";
        default: return "neutral"; // CREATED, etc.
    }
}


/** Circular score gauge (0–10). Color ramps good/warn/bad; pairs the number with color (a11y). */
export function scoreRing(score) {
    const v = score == null ? 0 : Math.max(0, Math.min(10, score));
    const r = 42, circ = 2 * Math.PI * r, offset = circ * (1 - v / 10);
    const color = v >= 8 ? "var(--good)" : v >= 5 ? "var(--warn)" : "var(--bad)";
    return html`
        <div class="score-ring">
            <svg viewBox="0 0 100 100" width="96" height="96" aria-hidden="true">
                <circle cx="50" cy="50" r="${r}" fill="none" stroke="var(--surface-2)" stroke-width="8"/>
                <circle cx="50" cy="50" r="${r}" fill="none" stroke="${color}" stroke-width="8" stroke-linecap="round"
                        stroke-dasharray="${circ}" stroke-dashoffset="${offset}" transform="rotate(-90 50 50)"/>
            </svg>
            <div class="score-ring__label">${score == null ? "—" : v.toFixed(1)}<span>/10</span></div>
        </div>`;
}

export function skillBar({ label, score, count = 0 }) {
    const v = score == null ? 0 : Math.max(0, Math.min(10, score));
    const variant = v >= 8 ? "good" : v >= 5 ? "warn" : "bad";
    return html`
        <div class="skill">
            <div class="skill__top">
                <span>${label}</span>
                <span class="muted">${score == null ? "—" : v.toFixed(1)} · ${count} assessment${count === 1 ? "" : "s"}</span>
            </div>
            <div class="skill__track"><div class="skill__fill skill__fill--${variant}" style="width:${v * 10}%"></div></div>
        </div>`;
}

/** Minimal score trend sparkline (0–10 scale). Returns "" for empty input. */
export function miniTrendChart(values, { width = 480, height = 80 } = {}) {
    if (!values || !values.length) return "";
    const n = values.length;
    const pts = values.map((val, i) => {
        const x = n > 1 ? (i / (n - 1)) * width : width / 2;
        const y = height - (Math.max(0, Math.min(10, val)) / 10) * height;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(" ");
    return html`
        <svg class="trend" viewBox="0 0 ${width} ${height}" width="100%" height="${height}" preserveAspectRatio="none" aria-hidden="true">
            <polyline fill="none" stroke="var(--accent)" stroke-width="2" points="${pts}"/>
        </svg>`;
}

/**
 * KPI card. Optional `trend` = { dir: "up" | "down" | "neutral", label }. Up renders green with an
 * up-arrow, down red with a down-arrow, neutral muted (use for insufficient history — never fabricate).
 */
export function statCard({ label, value, hint = "", trend = null }) {
    const arrow = { up: "▲", down: "▼", neutral: "–" };
    return html`
        <div class="stat">
            <div class="stat__label">${label}</div>
            <div class="stat__value">${value}</div>
            ${trend
                ? html`<div class="stat__trend stat__trend--${trend.dir}"><span aria-hidden="true">${arrow[trend.dir] || "–"}</span> ${trend.label}</div>`
                : hint ? html`<div class="stat__hint muted">${hint}</div>` : ""}
        </div>`;
}

export function chatBubble({ who, text, label }) {
    const name = label || (who === "ai" ? "Interviewer" : "You");
    return html`<div class="bubble bubble--${who}"><div class="bubble__who">${name}</div><div class="bubble__text">${text}</div></div>`;
}

/** Source citation shown under a knowledge-grounded answer. */
export function citationChip({ fileName, chunkNumber }) {
    return html`<span class="cite">${fileName || "source"}${chunkNumber != null ? ` · chunk ${chunkNumber}` : ""}</span>`;
}

export function typingBubble() {
    return html`<div class="bubble bubble--ai"><div class="bubble__who">Interviewer</div><div class="typing"><span></span><span></span><span></span></div></div>`;
}
