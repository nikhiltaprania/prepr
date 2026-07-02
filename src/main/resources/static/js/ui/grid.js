// Thin wrapper over AG Grid Community (loaded via CDN as the global `agGrid`). Centralizes our grid
// defaults: sorting, filtering, quick search, pagination, column resizing, theming, responsive sizing.
// (Row grouping is AG Grid Enterprise-only and intentionally not used.)

export function agGridAvailable() { return typeof window.agGrid !== "undefined"; }

/** Resolves true once the async-loaded AG Grid global is ready (or false after timeout). */
export function whenAgGridReady(timeoutMs = 8000) {
    return new Promise((resolve) => {
        if (agGridAvailable()) return resolve(true);
        const start = Date.now();
        const timer = setInterval(() => {
            if (agGridAvailable() || Date.now() - start > timeoutMs) {
                clearInterval(timer);
                resolve(agGridAvailable());
            }
        }, 100);
    });
}

export function gridThemeClass() {
    return document.documentElement.dataset.theme === "dark" ? "ag-theme-quartz-dark" : "ag-theme-quartz";
}

/**
 * Community date filter params for a column whose value is an ISO date string. Use:
 *   { ..., filter: "agDateColumnFilter", filterParams: dateFilterParams }
 */
export const dateFilterParams = {
    comparator: (filterMidnight, cellValue) => {
        if (!cellValue) return -1;
        const d = new Date(cellValue);
        if (isNaN(d)) return -1;
        const cellMidnight = new Date(d.getFullYear(), d.getMonth(), d.getDate());
        if (cellMidnight < filterMidnight) return -1;
        if (cellMidnight > filterMidnight) return 1;
        return 0;
    },
};

/**
 * Create a grid in `container`. Returns the AG Grid api (use `api.setGridOption('quickFilterText', v)`
 * to drive an external search box). Caller destroys it on teardown.
 */
export function createDataGrid(container, { columnDefs, rowData, onRowClicked, pageSize = 10, gridOptions = {} }) {
    container.classList.add(gridThemeClass());
    return window.agGrid.createGrid(container, {
        columnDefs,
        rowData,
        // No `flex` here: flex columns fight manual resizing. fitGridWidth gives an even default
        // distribution on load while keeping every column independently resizable afterwards.
        defaultColDef: { sortable: true, filter: true, resizable: true, minWidth: 120 },
        pagination: true,
        paginationPageSize: pageSize,
        paginationPageSizeSelector: [10, 20, 50],
        domLayout: "autoHeight",
        autoSizeStrategy: { type: "fitGridWidth", defaultMinWidth: 120 },
        suppressCellFocus: true,
        animateRows: true,
        onRowClicked: onRowClicked ? (e) => onRowClicked(e.data) : undefined,
        ...gridOptions,   // per-screen overrides (e.g. editable cells, onCellValueChanged)
    });
}
