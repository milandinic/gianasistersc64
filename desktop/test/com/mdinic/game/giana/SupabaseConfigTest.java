package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import com.mdinic.game.giana.service.EnvLookup;
import com.mdinic.game.giana.service.SupabaseConfig;

public class SupabaseConfigTest {

    @Test
    public void fromProperties_allKeysPresent_isConfigured() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://proj.supabase.co");
        p.setProperty("supabase.anonKey", "anon123");
        p.setProperty("supabase.functionsUrl", "https://proj.functions.supabase.co");
        p.setProperty("score.secret", "s3cr3t");

        SupabaseConfig c = SupabaseConfig.fromProperties(p);

        assertTrue(c.isConfigured());
        assertEquals("https://proj.supabase.co", c.url);
        assertEquals("anon123", c.anonKey);
        assertEquals("https://proj.functions.supabase.co", c.functionsUrl);
        assertEquals("s3cr3t", c.secret);
    }

    @Test
    public void fromProperties_missingKey_notConfigured() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://proj.supabase.co");
        // anonKey, functionsUrl, secret missing

        assertFalse(SupabaseConfig.fromProperties(p).isConfigured());
    }

    @Test
    public void fromProperties_blankValue_notConfigured() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://proj.supabase.co");
        p.setProperty("supabase.anonKey", "   ");
        p.setProperty("supabase.functionsUrl", "https://proj.functions.supabase.co");
        p.setProperty("score.secret", "s3cr3t");

        assertFalse(SupabaseConfig.fromProperties(p).isConfigured());
    }

    @Test
    public void fromProperties_null_notConfigured() {
        assertFalse(SupabaseConfig.fromProperties(null).isConfigured());
    }

    /** Small fake environment for tests: returns a value for a known name, else null. */
    private static EnvLookup env(final String name, final String value) {
        return new EnvLookup() {
            public String get(String n) {
                return name.equals(n) ? value : null;
            }
        };
    }

    private static Properties fullProps() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://file.supabase.co");
        p.setProperty("supabase.anonKey", "fileAnon");
        p.setProperty("supabase.functionsUrl", "https://file.functions.supabase.co");
        p.setProperty("score.secret", "fileSecret");
        return p;
    }

    @Test
    public void fromSources_envOverridesFileValue() {
        SupabaseConfig c = SupabaseConfig.fromSources(fullProps(),
                env("GIANA_SUPABASE_URL", "https://env.supabase.co"));

        assertEquals("https://env.supabase.co", c.url); // env wins
        assertEquals("fileAnon", c.anonKey);            // others fall back to file
        assertTrue(c.isConfigured());
    }

    @Test
    public void fromSources_blankEnvFallsBackToFile() {
        SupabaseConfig c = SupabaseConfig.fromSources(fullProps(),
                env("GIANA_SUPABASE_URL", "   ")); // blank == absent

        assertEquals("https://file.supabase.co", c.url);
        assertTrue(c.isConfigured());
    }

    @Test
    public void fromSources_envOnlyWithNullProps_isConfigured() {
        final Properties none = null;
        EnvLookup all = new EnvLookup() {
            public String get(String n) {
                if ("GIANA_SUPABASE_URL".equals(n)) return "https://env.supabase.co";
                if ("GIANA_SUPABASE_ANON_KEY".equals(n)) return "envAnon";
                if ("GIANA_SUPABASE_FUNCTIONS_URL".equals(n)) return "https://env.functions.supabase.co";
                if ("GIANA_SCORE_SECRET".equals(n)) return "envSecret";
                return null;
            }
        };

        SupabaseConfig c = SupabaseConfig.fromSources(none, all);

        assertTrue(c.isConfigured());
        assertEquals("https://env.supabase.co", c.url);
        assertEquals("envSecret", c.secret);
    }

    @Test
    public void fromSources_neitherSource_notConfigured() {
        EnvLookup empty = new EnvLookup() {
            public String get(String n) {
                return null;
            }
        };

        assertFalse(SupabaseConfig.fromSources(null, empty).isConfigured());
    }
}
