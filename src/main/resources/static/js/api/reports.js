// Interview report endpoints. The report is generated automatically on completion (server-side),
// but `generate` lets the UI (re)trigger it if it isn't ready yet.

import { api } from "../core/api.js";

export function get(id) { return api.get(`/interview-reports/interview/${id}`); }
export function generate(id) { return api.post(`/interview-reports/generate/${id}`); }
