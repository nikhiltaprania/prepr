// User profile endpoints. (Resume upload/analysis + admin list arrive with Milestone 7.)

import { api } from "../core/api.js";

export function get(id) {
    return api.get(`/users/${id}`);
}

export function update(id, body) {
    return api.put(`/users/${id}`, body);
}

/* Resume (candidate profile attachment) */
export function resume(userId) {
    return api.get(`/resumes/users/${userId}`);
}

export function uploadResume(userId, file) {
    const fd = new FormData();
    fd.append("file", file);
    return api.upload(`/resumes/users/${userId}`, fd);
}

/* Admin user management */
export function list() {
    return api.get("/users");
}

export function create(body) {
    return api.post("/users", body);
}

export function updateRole(id, role) {
    return api.put(`/users/${id}/role`, { role });
}

export function remove(id) {
    return api.del(`/users/${id}`);
}
