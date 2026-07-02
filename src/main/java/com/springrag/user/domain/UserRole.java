package com.springrag.user.domain;

/**
 * Coarse-grained authorization role. Stored on the user and emitted as the JWT {@code role} claim.
 * Mapped to a Spring Security authority named exactly {@code ROLE_<name>} so {@code hasRole('ADMIN')}
 * works. Add new roles here (e.g. COACH, RECRUITER) as the product grows — no other change needed.
 */
public enum UserRole {

    /** Default candidate role: owns and accesses only their own resources. */
    USER,

    /** Administrative role: may access any user's resources and admin-only endpoints. */
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
