package com.mdinic.game.giana.service;

import java.util.Properties;

/** Immutable Supabase connection config, loaded from a properties source. */
public class SupabaseConfig {

    public final String url;
    public final String anonKey;
    public final String functionsUrl;
    public final String secret;

    public SupabaseConfig(String url, String anonKey, String functionsUrl, String secret) {
        this.url = url == null ? "" : url.trim();
        this.anonKey = anonKey == null ? "" : anonKey.trim();
        this.functionsUrl = functionsUrl == null ? "" : functionsUrl.trim();
        this.secret = secret == null ? "" : secret.trim();
    }

    /** True only when every value is present and non-blank. */
    public boolean isConfigured() {
        return !url.isEmpty() && !anonKey.isEmpty() && !functionsUrl.isEmpty() && !secret.isEmpty();
    }

    public static SupabaseConfig fromProperties(Properties p) {
        if (p == null) {
            return new SupabaseConfig("", "", "", "");
        }
        return new SupabaseConfig(p.getProperty("supabase.url"), p.getProperty("supabase.anonKey"),
                p.getProperty("supabase.functionsUrl"), p.getProperty("score.secret"));
    }
}
