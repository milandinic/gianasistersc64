package com.mdinic.game.giana.service;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * The property keys whose values are blank — empty when {@link #isConfigured()}.
     * Used to log exactly which config values are missing at startup.
     */
    public List<String> missingKeys() {
        List<String> missing = new ArrayList<String>();
        if (url.isEmpty()) {
            missing.add("supabase.url");
        }
        if (anonKey.isEmpty()) {
            missing.add("supabase.anonKey");
        }
        if (functionsUrl.isEmpty()) {
            missing.add("supabase.functionsUrl");
        }
        if (secret.isEmpty()) {
            missing.add("score.secret");
        }
        return missing;
    }

    public static SupabaseConfig fromProperties(Properties p) {
        return fromSources(p, new EnvLookup() {
            public String get(String name) {
                return null;
            }
        });
    }

    /**
     * Resolves each value as: environment variable (if non-blank) wins over the
     * file property. {@code fileProps} may be {@code null} (no file present), in
     * which case only the environment is consulted.
     */
    public static SupabaseConfig fromSources(Properties fileProps, EnvLookup env) {
        return new SupabaseConfig(
                pick(env.get("GIANA_SUPABASE_URL"), prop(fileProps, "supabase.url")),
                pick(env.get("GIANA_SUPABASE_ANON_KEY"), prop(fileProps, "supabase.anonKey")),
                pick(env.get("GIANA_SUPABASE_FUNCTIONS_URL"), prop(fileProps, "supabase.functionsUrl")),
                pick(env.get("GIANA_SCORE_SECRET"), prop(fileProps, "score.secret")));
    }

    private static String prop(Properties p, String key) {
        return p == null ? null : p.getProperty(key);
    }

    /** Env value if present and non-blank, otherwise the file value. */
    private static String pick(String envValue, String fileValue) {
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        return fileValue;
    }
}
