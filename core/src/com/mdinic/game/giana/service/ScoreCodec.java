package com.mdinic.game.giana.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;

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
}
