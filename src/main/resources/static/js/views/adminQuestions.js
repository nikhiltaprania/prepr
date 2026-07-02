// Admin — Question bank. An AG Grid (Community) of reusable interview questions organized by stack
// (type), difficulty, and category. Admins view/search/filter/create/edit/delete, and AI-generate a
// batch. These questions seed interview plans (backend), so the bank feeds real interviews.

import { html, mount, on, el } from "../core/dom.js";
import { isAdmin } from "../core/store.js";
import { fmtDate } from "../core/format.js";
import { humanize } from "../ui/components.js";
import { createDataGrid, whenAgGridReady, dateFilterParams } from "../ui/grid.js";
import { toast } from "../ui/toast.js";
import * as questions from "../api/questions.js";

const TYPES = ["JAVA_BACKEND", "SPRING_BOOT", "MICROSERVICES", "SYSTEM_DESIGN", "BEHAVIORAL", "HR", "CUSTOM"];
const DIFFICULTIES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

class SourceRenderer {
    init(p) {
        this.eGui = document.createElement("span");
        const ai = p.value === true || p.value === "true";
        this.eGui.className = "badge badge--" + (ai ? "info" : "neutral");
        this.eGui.textContent = ai ? "AI" : "Manual";
    }
    getGui() { return this.eGui; }
    refresh() { return false; }
}

export default function adminQuestionsView(outlet) {
    let rows = [], gridApi = null;
    // Shared settings, configured ONCE and used by both the manual-add and AI-generate actions.
    let cfg = { type: "JAVA_BACKEND", difficulty: "INTERMEDIATE", category: "" };

    if (!isAdmin()) {
        mount(outlet, html`<div class="page"><h1 class="page__title">Question bank</h1><p class="muted">Administrators only.</p></div>`);
        return () => {};
    }

    const options = (values, selected) => values.map((v) => html`<option value="${v}" ${v === selected ? "selected" : ""}>${humanize(v)}</option>`);
    const captureCfg = () => { cfg = { type: el("#q-type").value, difficulty: el("#q-difficulty").value, category: el("#q-category").value }; };

    const onDelete = async (q) => {
        if (!confirm("Delete this question?")) return;
        try { await questions.remove(q.id); await load(); toast("Deleted", "success"); }
        catch (err) { toast(err?.message || "Delete failed", "error"); }
    };

    class DeleteRenderer {
        init(p) { this.eGui = document.createElement("button"); this.eGui.className = "btn btn--danger btn--sm"; this.eGui.textContent = "Delete"; this.eGui.onclick = () => onDelete(p.data); }
        getGui() { return this.eGui; }
        refresh() { return false; }
    }

    function render() {
        mount(outlet, html`
            <div class="page">
                <h1 class="page__title">Question bank</h1>

                <div class="qbank-bar">
                    <div class="toolbar-form">
                        <span class="toolbar-form__label">Settings</span>
                        <select id="q-type">${options(TYPES, cfg.type)}</select>
                        <select id="q-difficulty">${options(DIFFICULTIES, cfg.difficulty)}</select>
                        <input id="q-category" placeholder="Category (optional)" value="${cfg.category}"/>
                    </div>
                    <div class="toolbar-form">
                        <input id="q-text" placeholder="New question text…" style="flex:1; min-width:240px"/>
                        <button class="btn btn--primary btn--sm" data-action="add-q" type="button">Add question</button>
                        <span class="toolbar-form__label">or</span>
                        <input id="q-count" type="number" min="1" max="15" value="5" style="min-width:80px"/>
                        <button class="btn btn--secondary btn--sm" id="gen-btn" data-action="gen-q" type="button">AI generate</button>
                    </div>
                </div>

                <div class="history__head">
                    <span class="muted">${rows.length} question${rows.length === 1 ? "" : "s"}</span>
                    <input id="q-search" class="grid-search" type="search" placeholder="Search questions…"/>
                </div>
                <div id="q-grid" class="data-grid"></div>
            </div>`);
        buildGrid();
    }

    function buildGrid() {
        const container = document.getElementById("q-grid");
        if (!container) return;
        whenAgGridReady().then((ready) => {
            const live = document.getElementById("q-grid");
            if (!ready || live !== container) { container.innerHTML = "<p class='muted'>Grid unavailable.</p>"; return; }
            gridApi = createDataGrid(container, {
                rowData: rows,
                columnDefs: [
                    { headerName: "Type", field: "type", minWidth: 130, maxWidth: 150, editable: true, cellEditor: "agSelectCellEditor", cellEditorParams: { values: TYPES }, valueFormatter: (p) => humanize(p.value) },
                    { headerName: "Difficulty", field: "difficulty", minWidth: 120, maxWidth: 140, editable: true, cellEditor: "agSelectCellEditor", cellEditorParams: { values: DIFFICULTIES }, valueFormatter: (p) => humanize(p.value) },
                    { headerName: "Category", field: "category", minWidth: 110, maxWidth: 150, editable: true },
                    { headerName: "Question", field: "question", minWidth: 220, editable: true, tooltipField: "question" },
                    { headerName: "Source", field: "aiGenerated", minWidth: 95, maxWidth: 110, editable: false, cellRenderer: SourceRenderer },
                    { headerName: "Created", colId: "created", minWidth: 120, maxWidth: 150, editable: false, valueGetter: (p) => p.data.createdAt, valueFormatter: (p) => fmtDate(p.value), filter: "agDateColumnFilter", filterParams: dateFilterParams },
                    { headerName: "", colId: "actions", minWidth: 90, maxWidth: 100, editable: false, sortable: false, filter: false, cellRenderer: DeleteRenderer },
                ],
                gridOptions: { onCellValueChanged: onCellChanged, stopEditingWhenCellsLoseFocus: true },
            });
        });
    }

    async function load() {
        try { rows = await questions.list(); }
        catch (err) { toast(err?.message || "Couldn't load questions", "error"); rows = []; }
        render();
    }

    async function onCellChanged(e) {
        try {
            await questions.update(e.data.id, {
                type: e.data.type, difficulty: e.data.difficulty,
                category: e.data.category || null, question: e.data.question,
            });
            toast("Saved", "success");
        } catch (err) { toast(err?.message || "Update failed", "error"); load(); }
    }

    render();
    load();

    const offAdd = on(outlet, "click", '[data-action="add-q"]', async () => {
        captureCfg();
        const text = el("#q-text").value.trim();
        if (!text) { toast("Enter a question.", "error"); return; }
        try {
            await questions.create({ ...cfg, category: cfg.category || null, question: text });
            await load();   // re-render preserves cfg
            toast("Question added", "success");
        } catch (err) { toast(err?.message || "Couldn't add question", "error"); }
    });

    const offGen = on(outlet, "click", '[data-action="gen-q"]', async () => {
        captureCfg();
        const count = Number(el("#q-count").value) || 5;
        const btn = el("#gen-btn");
        if (btn) { btn.disabled = true; btn.textContent = "Generating…"; }
        try {
            await questions.generate({ ...cfg, category: cfg.category || null, count });
            await load();
            toast(`Generated ${count} question${count === 1 ? "" : "s"}`, "success");
        } catch (err) {
            toast(err?.message || "Generation failed", "error");
            if (btn) { btn.disabled = false; btn.textContent = "AI generate"; }
        }
    });

    const offSearch = on(outlet, "input", "#q-search", (_e, t) => {
        if (gridApi) gridApi.setGridOption("quickFilterText", t.value);
    });

    return () => {
        offAdd(); offGen(); offSearch();
        if (gridApi) { try { gridApi.destroy(); } catch { /* ignore */ } gridApi = null; }
    };
}
