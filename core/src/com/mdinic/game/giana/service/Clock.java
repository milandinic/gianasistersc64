package com.mdinic.game.giana.service;

/**
 * Supplies the current wall-clock time in epoch milliseconds. Production wires
 * this to {@link System#currentTimeMillis()}; tests inject a fixed value so the
 * UTC-day comparison in {@link SupabaseHighScoreService#goodForHighScores(int)}
 * is deterministic. Kept as an interface (not a lambda target by convention —
 * this codebase uses anonymous classes), mirroring {@link EnvLookup}.
 */
public interface Clock {
    /** @return the current time in epoch milliseconds. */
    long now();
}
