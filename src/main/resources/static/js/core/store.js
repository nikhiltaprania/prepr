// Minimal app state: persisted session + a tiny pub/sub for the few cross-view signals.
// No framework. Most state stays local to a view; this is only for what must be shared.

const SESSION_KEY = "prepr.session";

function load() {
    try { return JSON.parse(localStorage.getItem(SESSION_KEY)) || null; }
    catch { return null; }
}

let session = load();                 // { token, userId, role } | null
const subscribers = new Map();        // key -> Set<fn>

function emit(key, value) {
    subscribers.get(key)?.forEach((fn) => fn(value));
}

/* Session ----------------------------------------------------------------- */
export function getSession() { return session; }
export function getToken() { return session?.token || null; }
export function getUserId() { return session?.userId || null; }
export function getDisplayName() { return session?.name || null; }
export function isAuthed() { return !!(session && session.token); }
export function isAdmin() { return session?.role === "ROLE_ADMIN"; }

export function setSession(next) {
    session = next;
    localStorage.setItem(SESSION_KEY, JSON.stringify(next));
    emit("session", next);
}

/** Merge a partial update into the current session (e.g. a renamed profile) and notify subscribers. */
export function updateSession(partial) {
    if (!session) return;
    setSession({ ...session, ...partial });
}

/** Decode a JWT payload (no verification — the server verifies on every API call). */
function decodeJwt(token) {
    try {
        const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        return JSON.parse(decodeURIComponent(escape(atob(part))));
    } catch { return {}; }
}

/** Build a session from a raw JWT — used for the OAuth2 redirect, which carries only the token. */
export function setSessionFromToken(token) {
    const claims = decodeJwt(token);
    setSession({ token, userId: claims.sub, role: claims.role, name: claims.name || claims.email });
}

export function clearSession() {
    session = null;
    localStorage.removeItem(SESSION_KEY);
    emit("session", null);
}

/* Active assistant conversation (no list endpoint — we keep one active id) ---- */
const CONVO_KEY = "prepr.conversation";
export function getActiveConversation() { return localStorage.getItem(CONVO_KEY) || null; }
export function setActiveConversation(id) { localStorage.setItem(CONVO_KEY, id); }
export function clearActiveConversation() { localStorage.removeItem(CONVO_KEY); }

/* Generic pub/sub (sparingly used) ---------------------------------------- */
export function subscribe(key, fn) {
    if (!subscribers.has(key)) subscribers.set(key, new Set());
    subscribers.get(key).add(fn);
    return () => subscribers.get(key)?.delete(fn);
}
