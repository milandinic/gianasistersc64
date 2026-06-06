package com.mdinic.game.giana.service;

/**
 * Looks up a configuration value by environment-variable name. Production wires
 * this to {@link System#getenv(String)}; tests inject a fake. Kept as an
 * interface (not a lambda target by convention — this codebase uses anonymous
 * classes) so {@link SupabaseConfig#fromSources} stays unit-testable without
 * real process environment variables.
 */
public interface EnvLookup {
    /** @return the value for {@code name}, or {@code null} if unset. */
    String get(String name);
}
