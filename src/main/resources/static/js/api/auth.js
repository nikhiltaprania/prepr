// Auth endpoints (public — no Bearer token). Returns AuthResponse { accessToken, userId, role, ... }.

import { api } from "../core/api.js";

export function register(body) {
    return api.post("/auth/register", body, { auth: false });
}

export function login(body) {
    return api.post("/auth/login", body, { auth: false });
}
