// Admin question-bank endpoints. The bank seeds interview plans, so these power a curated library
// that flows into real interviews (not an isolated feature).

import { api } from "../core/api.js";

export function list() { return api.get("/admin/questions"); }
export function create(body) { return api.post("/admin/questions", body); }
export function update(id, body) { return api.put(`/admin/questions/${id}`, body); }
export function remove(id) { return api.del(`/admin/questions/${id}`); }

export function generate({ type, difficulty, category, count }) {
    const q = new URLSearchParams({ type, difficulty, count: String(count) });
    if (category) q.set("category", category);
    return api.post(`/admin/questions/generate?${q.toString()}`);
}
