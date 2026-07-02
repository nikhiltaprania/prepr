// Skill tracking + coaching endpoints (both user-scoped). Coaching is wired in Milestone 5.

import { api } from "../core/api.js";

export function skills(userId) { return api.get(`/user-skills/user/${userId}`); }

// The persisted active coaching plan (no generation; 404 if none yet).
export function plan(userId) { return api.get(`/coaching/users/${userId}/plan`); }

// Generate a fresh, data-grounded plan and replace the active one (LLM call — on demand).
export function generatePlan(userId) { return api.post(`/coaching/users/${userId}/plan`); }
