// Interview endpoints: lifecycle, the conversational turn loop, questions and answers.

import { api } from "../core/api.js";

export function create(body) { return api.post("/interviews", body); }
export function start(id) { return api.post(`/interviews/${id}/start`); }
export function get(id) { return api.get(`/interviews/${id}`); }
export function listByUser(userId) { return api.get(`/interviews/user/${userId}`); }

// Conversational interview channel (voice/text). respond("") asks the AI to open the interview.
export function respond(id, text) { return api.post(`/voice/interviews/${id}/respond`, { text }); }
export function transcript(id) { return api.get(`/voice/interviews/${id}/transcript`); }

// Question plan + recorded answers (used by the report breakdown).
export function generatePlan(id, count) { return api.post(`/interview-questions/plan/${id}?count=${count}`); }
export function questions(id) { return api.get(`/interview-questions/interview/${id}`); }
export function answers(id) { return api.get(`/interview-answers/interview/${id}`); }
