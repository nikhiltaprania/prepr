// Admin — User management. An AG Grid (Community) of all users with inline editing (username, email,
// role), a create form, search, and per-row delete. Admin-only.

import { html, mount, on } from "../core/dom.js";
import { isAdmin } from "../core/store.js";
import { fmtDate } from "../core/format.js";
import { createDataGrid, whenAgGridReady, dateFilterParams } from "../ui/grid.js";
import { toast } from "../ui/toast.js";
import * as users from "../api/users.js";

// Delete-button cell renderer (AG Grid component — version-safe).
class DeleteRenderer {
    init(p) {
        this.eGui = document.createElement("button");
        this.eGui.className = "btn btn--danger btn--sm";
        this.eGui.textContent = "Delete";
        this.eGui.onclick = () => p.onDelete(p.data);
    }
    getGui() { return this.eGui; }
    refresh() { return false; }
}

export default function adminView(outlet) {
    let rows = [], gridApi = null;

    if (!isAdmin()) {
        mount(outlet, html`<div class="page"><h1 class="page__title">User management</h1><p class="muted">Administrators only.</p></div>`);
        return () => {};
    }

    function render() {
        mount(outlet, html`
            <div class="page">
                <h1 class="page__title">User management</h1>
                <form class="toolbar-form" id="create-user">
                    <input name="username" placeholder="Username" required/>
                    <input name="email" type="email" placeholder="Email" required/>
                    <button class="btn btn--primary btn--sm" type="submit">Add user</button>
                </form>
                <div class="history__head">
                    <span class="muted">${rows.length} user${rows.length === 1 ? "" : "s"}</span>
                    <input id="user-search" class="grid-search" type="search" placeholder="Search users…"/>
                </div>
                <div id="users-grid" class="data-grid"></div>
            </div>`);
        buildGrid();
    }

    function buildGrid() {
        const container = document.getElementById("users-grid");
        if (!container) return;
        whenAgGridReady().then((ready) => {
            const live = document.getElementById("users-grid");
            if (!ready || live !== container) { container.innerHTML = "<p class='muted'>Grid unavailable.</p>"; return; }
            gridApi = createDataGrid(container, {
                rowData: rows,
                columnDefs: [
                    { headerName: "Username", field: "username", editable: true },
                    { headerName: "Email", field: "email", editable: true, minWidth: 200 },
                    {
                        headerName: "Role", field: "role", editable: true, maxWidth: 140,
                        cellEditor: "agSelectCellEditor", cellEditorParams: { values: ["USER", "ADMIN"] },
                    },
                    { headerName: "Created", colId: "created", maxWidth: 180, editable: false, valueGetter: (p) => p.data.createdAt, valueFormatter: (p) => fmtDate(p.value), filter: "agDateColumnFilter", filterParams: dateFilterParams },
                    { headerName: "", colId: "actions", maxWidth: 120, editable: false, sortable: false, filter: false, cellRenderer: DeleteRenderer, cellRendererParams: { onDelete } },
                ],
                gridOptions: { onCellValueChanged: onCellChanged, stopEditingWhenCellsLoseFocus: true },
            });
        });
    }

    async function load() {
        try { rows = await users.list(); }
        catch (err) { toast(err?.message || "Couldn't load users", "error"); rows = []; }
        render();
    }

    async function onCellChanged(e) {
        try {
            if (e.colDef.field === "role") {
                await users.updateRole(e.data.id, e.newValue);
            } else {
                // Full update preserving the row's other fields (the service replaces all fields).
                await users.update(e.data.id, {
                    username: e.data.username, email: e.data.email,
                    targetRole: e.data.targetRole || null, experienceYears: e.data.experienceYears ?? null,
                    currentCompany: e.data.currentCompany || null, targetCompany: e.data.targetCompany || null,
                    bio: e.data.bio || null,
                });
            }
            toast("Saved", "success");
        } catch (err) {
            toast(err?.message || "Update failed", "error");
            load();   // revert to server state
        }
    }

    async function onDelete(user) {
        if (!confirm(`Delete ${user.username}? This cannot be undone.`)) return;
        try { await users.remove(user.id); await load(); toast("User deleted", "success"); }
        catch (err) { toast(err?.message || "Delete failed", "error"); }
    }

    render();
    load();

    const offCreate = on(outlet, "submit", "#create-user", async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            await users.create({ username: fd.get("username"), email: fd.get("email") });
            await load();
            toast("User created", "success");
        } catch (err) { toast(err?.message || "Couldn't create user", "error"); }
    });
    const offSearch = on(outlet, "input", "#user-search", (_e, t) => {
        if (gridApi) gridApi.setGridOption("quickFilterText", t.value);
    });

    return () => {
        offCreate(); offSearch();
        if (gridApi) { try { gridApi.destroy(); } catch { /* ignore */ } gridApi = null; }
    };
}
