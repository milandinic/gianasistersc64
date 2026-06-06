package com.mdinic.game.giana.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Pure, side-effect-free helpers for encoding/signing scores. No Gdx.net here. */
public final class ScoreCodec {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String PATH = "/rest/v1/scores?select=name,score,level";

    private ScoreCodec() {
    }

    public static String signingString(String name, int score, int level, long ts) {
        return name + "|" + score + "|" + level + "|" + ts;
    }

    public static String allTimeUrl(String baseUrl) {
        return baseUrl + PATH + "&order=score.desc&limit=5";
    }

    public static String todaysUrl(String baseUrl, long nowMillis) {
        return baseUrl + PATH + "&created_at=gte." + utcMidnightIso(nowMillis) + "&order=score.desc&limit=5";
    }

    /** UTC midnight of the day containing nowMillis, as ISO-8601 'yyyy-MM-ddT00:00:00Z'. */
    public static String utcMidnightIso(long nowMillis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(nowMillis);
        int y = c.get(Calendar.YEAR);
        int mo = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02dT00:00:00Z", y, mo, d);
    }

    public static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(UTF8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(UTF8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC failure", e);
        }
    }

    public static List<Score> parseScores(String json) {
        List<Score> out = new ArrayList<Score>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        JsonValue root = new JsonReader().parse(json);
        if (root == null) {
            return out;
        }
        for (JsonValue v = root.child; v != null; v = v.next) {
            out.add(new Score(v.getString("name", ""), v.getInt("score", 0), v.getInt("level", 0)));
        }
        return out;
    }

    /** Compact JSON body for the submit-score Edge Function. Numbers stay unquoted. */
    public static String submitBody(PendingSubmit ps) {
        return "{\"name\":" + jsonQuote(ps.name) + ",\"score\":" + ps.score + ",\"level\":" + ps.level
                + ",\"ts\":" + ps.ts + ",\"sig\":" + jsonQuote(ps.sig) + "}";
    }

    static String jsonQuote(String raw) {
        String s = raw == null ? "" : raw;
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
