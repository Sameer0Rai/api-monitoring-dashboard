package com.monitor.model;

/**
 * Only USER exists today - the whole point of a real enum (backed by a Spring Security
 * authority, see CustomUserDetails) instead of a boolean "isAdmin" flag is that adding
 * ADMIN later is a one-line addition here plus a @PreAuthorize annotation, not a schema
 * migration and a rewrite of the authorization model.
 */
public enum Role {
    USER
}
