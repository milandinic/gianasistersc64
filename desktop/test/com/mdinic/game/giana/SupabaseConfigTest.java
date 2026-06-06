package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

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
}
