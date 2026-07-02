// Home — the hub. Greeting + Start CTA, a progress snapshot, a "continue" card, interview history,
// and a coaching nudge. First run (no interviews) shows a focused empty state.

import { html, mount, on } from "../core/dom.js";
import { getUserId } from "../core/store.js";
import { fmtDate } from "../core/format.js";
import { statCard, card, skeleton, button, statusPill, statusVariant, humanize, emptyState } from "../ui/components.js";
import { createDataGrid, agGridAvailable, whenAgGridReady, dateFilterParams } from "../ui/grid.js";
import { openNewInterviewModal } from "./newInterview.js";
import * as interviews from "../api/interviews.js";
import * as growth from "../api/growth.js";
import * as users from "../api/users.js";

// AG Grid cell renderer component (version-safe) for the status badge.
class StatusBadgeRenderer {
    init(params) {
        this.eGui = document.createElement("span");
        if (params.value) {
            this.eGui.className = "badge badge--" + statusVariant(params.value);
            this.eGui.textContent = humanize(params.value);
        }
    }
    getGui() { return this.eGui; }
    refresh() { return false; }
}

export default function homeView(outlet) {
    let user = null, list = [], skills = [], loading = true;
    let gridApi = null;

    async function load() {
        const uid = getUserId();
        const [u, l, s] = await Promise.allSettled([users.get(uid), interviews.listByUser(uid), growth.skills(uid)]);
        user = u.status === "fulfilled" ? u.value : null;
        list = l.status === "fulfilled" ? l.value : [];
        skills = s.status === "fulfilled" ? s.value : [];
        loading = false;
        render();
    }

    /* derived */
    const completed = () => list.filter((i) => i.status === "COMPLETED");
    const avgScore = () => {
        const scored = completed().filter((i) => i.overallScore != null);
        return scored.length ? (scored.reduce((a, i) => a + i.overallScore, 0) / scored.length).toFixed(1) : "—";
    };
    const bySkill = (pick) => {
        if (!skills.length) return "—";
        const best = skills.reduce((acc, s) => pick(s.averageScore ?? 0, acc.averageScore ?? 0) ? s : acc);
        return humanize(best.skillCategory);
    };

    const NEUTRAL = { dir: "neutral", label: "—" };
    const dateOf = (i) => new Date(i.completedAt || i.startedAt).getTime();

    // Average-score trend: split scored interviews chronologically and compare the two halves.
    const scoreTrend = () => {
        const scored = completed().filter((i) => i.overallScore != null).sort((a, b) => dateOf(a) - dateOf(b));
        if (scored.length < 2) return NEUTRAL;
        const mid = Math.floor(scored.length / 2);
        const mean = (arr) => arr.reduce((a, x) => a + x.overallScore, 0) / arr.length;
        const delta = mean(scored.slice(mid)) - mean(scored.slice(0, mid));
        if (Math.abs(delta) < 0.05) return { dir: "neutral", label: "steady" };
        return { dir: delta > 0 ? "up" : "down", label: `${delta > 0 ? "+" : ""}${delta.toFixed(1)} vs earlier` };
    };

    // Completed-count trend: last 30 days vs the prior 30 days (needs a real prior baseline).
    const completedTrend = () => {
        const done = completed().filter((i) => i.completedAt || i.startedAt);
        const now = Date.now(), day = 86400000;
        const recent = done.filter((i) => now - dateOf(i) <= 30 * day).length;
        const prior = done.filter((i) => { const t = now - dateOf(i); return t > 30 * day && t <= 60 * day; }).length;
        if (prior === 0) return NEUTRAL;          // no baseline → neutral, never fabricate
        const delta = recent - prior;
        if (delta === 0) return { dir: "neutral", label: "steady" };
        return { dir: delta > 0 ? "up" : "down", label: `${delta > 0 ? "+" : ""}${delta} vs prior 30d` };
    };

    function hero() {
        return html`
            <div class="hero">
                <div>
                    <h1>${user ? `Welcome back, ${user.username}` : "Welcome"}</h1>
                    <p class="muted">${user?.targetRole ? `Preparing for ${user.targetRole}` : "Ready to practice?"}</p>
                </div>
                ${button({ label: "Start interview", action: "start" })}
            </div>`;
    }

    function snapshot() {
        return html`
            <div class="stats">
                ${statCard({ label: "Interviews completed", value: String(completed().length), trend: completedTrend() })}
                ${statCard({ label: "Average score", value: avgScore(), trend: scoreTrend() })}
                ${statCard({ label: "Strongest skill", value: bySkill((v, b) => v > b), hint: "by avg score" })}
                ${statCard({ label: "Needs work", value: bySkill((v, b) => v < b), hint: "focus area" })}
            </div>`;
    }

    function continueCard() {
        const inProgress = list.find((i) => i.status === "IN_PROGRESS");
        if (inProgress) {
            return card(html`
                <div class="between">
                    <div><strong>Resume your interview</strong><div class="muted">${humanize(inProgress.type)} · ${humanize(inProgress.difficulty)}</div></div>
                    ${button({ label: "Resume", href: `#/interviews/${inProgress.id}/room` })}
                </div>`);
        }
        const last = completed()[0];
        if (last) {
            return card(html`
                <div class="between">
                    <div><strong>Review your last report</strong><div class="muted">${humanize(last.type)} · score ${last.overallScore != null ? last.overallScore.toFixed(1) : "—"}</div></div>
                    ${button({ label: "View report", href: `#/interviews/${last.id}`, variant: "secondary" })}
                </div>`);
        }
        return "";
    }

    function nudge() {
        if (!skills.length) return "";
        const weakest = bySkill((v, b) => v < b);
        return card(html`
            <div class="between">
                <div><div class="muted">Focus area</div><strong>${weakest}</strong></div>
                ${button({ label: "Practice", action: "start", variant: "secondary" })}
            </div>`);
    }

    function history() {
        return html`
            <div class="history__head">
                <h3>Interview history</h3>
                <input id="history-search" class="grid-search" type="search" placeholder="Search interviews…"/>
            </div>
            <div id="history-grid" class="data-grid"></div>`;
    }

    // Build the AG Grid after the container is in the DOM. AG Grid loads async (decoupled from app
    // boot): show the simple table immediately, then upgrade to the grid once the library is ready.
    function buildGrid() {
        const container = document.getElementById("history-grid");
        if (!container) return;
        if (agGridAvailable()) { mountGrid(container); return; }

        container.innerHTML = String(fallbackTable());     // instant
        whenAgGridReady().then((ready) => {
            const live = document.getElementById("history-grid");
            if (ready && live === container) { container.innerHTML = ""; mountGrid(container); }
        });
    }

    function mountGrid(container) {
        gridApi = createDataGrid(container, {
            columnDefs: [
                { headerName: "Type", field: "type", valueFormatter: (p) => humanize(p.value) },
                { headerName: "Difficulty", field: "difficulty", valueFormatter: (p) => humanize(p.value) },
                { headerName: "Status", field: "status", maxWidth: 170, cellRenderer: StatusBadgeRenderer },
                { headerName: "Score", field: "overallScore", maxWidth: 120, valueFormatter: (p) => (p.value != null ? p.value.toFixed(1) : "—") },
                {
                    headerName: "Date", colId: "date", maxWidth: 180,
                    valueGetter: (p) => p.data.completedAt || p.data.startedAt,
                    valueFormatter: (p) => fmtDate(p.value),
                    filter: "agDateColumnFilter", filterParams: dateFilterParams,
                },
            ],
            rowData: list,
            onRowClicked: (row) => { location.hash = `#/interviews/${row.id}`; },
        });
    }

    function fallbackTable() {
        return html`
            <div class="table">
                <div class="table__head"><span>Type</span><span>Difficulty</span><span>Status</span><span>Score</span><span>Date</span></div>
                ${list.map((i) => html`
                    <a class="table__row" href="#/interviews/${i.id}">
                        <span>${humanize(i.type)}</span><span>${humanize(i.difficulty)}</span>
                        <span>${statusPill(i.status)}</span>
                        <span>${i.overallScore != null ? i.overallScore.toFixed(1) : "—"}</span>
                        <span class="muted">${fmtDate(i.completedAt || i.startedAt)}</span>
                    </a>`)}
            </div>`;
    }

    function render() {
        if (loading) { mount(outlet, html`<div class="page">${skeleton(8)}</div>`); return; }

        if (!list.length) {
            mount(outlet, html`
                <div class="page">
                    ${hero()}
                    ${emptyState({
                        title: "Start your first interview",
                        body: "Practice a realistic AI interview, get scored feedback, and track your progress. Tip: complete your profile in Settings for tailored questions.",
                        actionLabel: "Start interview",
                        action: "start",
                    })}
                </div>`);
            return;
        }

        mount(outlet, html`
            <div class="page">
                ${hero()}
                ${snapshot()}
                <div class="home__cards">${continueCard()}${nudge()}</div>
                ${history()}
            </div>`);
        buildGrid();
    }

    render();
    load();

    const offStart = on(outlet, "click", '[data-action="start"]', () => openNewInterviewModal());
    const offSearch = on(outlet, "input", "#history-search", (_e, t) => {
        if (gridApi) gridApi.setGridOption("quickFilterText", t.value);
    });
    return () => {
        offStart(); offSearch();
        if (gridApi) { try { gridApi.destroy(); } catch { /* ignore */ } gridApi = null; }
    };
}
