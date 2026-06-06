package com.mdinic.game.giana.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.Charset;

/** Pure, side-effect-free helpers for encoding/signing scores. No Gdx.net here. */
public final class ScoreCodec {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ScoreCodec() {
    }

    public static String signingString(String name, int score, int level, long ts) {
        return name + "|" + score + "|" + level + "|" + ts;
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
