// Single HTTP choke point. Every API call goes through here so auth headers, error
// normalization, and 401 handling exist in exactly one place.

import { getToken, clearSession } from "./store.js";

const BASE = "/api";

export class ApiError extends Error {
    constructor(status, message, body) {
        super(message);
        this.status = status;
        this.body = body;
    }
}

function safeJson(text) {
    try { return JSON.parse(text); } catch { return text; }
}

async function request(method, path, { body, auth = true, multipart = false } = {}) {
    const headers = {};
    if (auth) {
        const token = getToken();
        if (token) headers["Authorization"] = "Bearer " + token;
    }

    let payload;
    if (multipart) {
        payload = body;                                  // FormData — browser sets the boundary
    } else if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        payload = JSON.stringify(body);
    }

    let res;
    try {
        res = await fetch(BASE + path, { method, headers, body: payload });
    } catch {
        // fetch rejects on network failure / CORS / offline — give a human message.
        throw new ApiError(0, "Network error — please check your connection and try again.");
    }

    // A 401 on an *authenticated* call means the token is invalid/expired → drop session and
    // bounce to login. A 401 on a public call (login/register) is a normal error (bad credentials)
    // and must fall through so the backend message surfaces to the user.
    if (res.status === 401 && auth) {
        clearSession();
        if (location.hash !== "#/login") location.hash = "#/login";
        throw new ApiError(401, "Session expired");
    }

    const text = await res.text();
    const data = text ? safeJson(text) : null;

    if (!res.ok) {
        const message = (data && data.message) || res.statusText || "Request failed";
        throw new ApiError(res.status, message, data);
    }
    return data;
}

export const api = {
    get: (path, opts) => request("GET", path, opts),
    post: (path, body, opts) => request("POST", path, { ...opts, body }),
    put: (path, body, opts) => request("PUT", path, { ...opts, body }),
    del: (path, opts) => request("DELETE", path, opts),
    upload: (path, formData, opts) => request("POST", path, { ...opts, body: formData, multipart: true }),
};

/**
 * Poll `fn` until `until(result)` is true (or timeout). Used for async report scoring
 * and document processing. Always store the returned stop intent in a view teardown if long-lived.
 */
export async function poll(fn, { intervalMs = 2000, timeoutMs = 60000, until }) {
    const start = Date.now();
    // eslint-disable-next-line no-constant-condition
    while (true) {
        const result = await fn();
        if (until(result) || Date.now() - start > timeoutMs) return result;
        await new Promise((r) => setTimeout(r, intervalMs));
    }
}
