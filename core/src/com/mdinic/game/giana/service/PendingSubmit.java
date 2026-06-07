package com.mdinic.game.giana.service;

/** A score submission, already HMAC-signed, that may be queued offline. */
public class PendingSubmit {

    public String name;
    public int score;
    public int level;
    public long ts;
    public String sig;

    /** Required no-arg constructor for libGDX Json deserialization. */
    public PendingSubmit() {
    }

    public PendingSubmit(String name, int score, int level, long ts, String sig) {
        this.name = name;
        this.score = score;
        this.level = level;
        this.ts = ts;
        this.sig = sig;
    }
}
