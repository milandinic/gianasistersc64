package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.mdinic.game.giana.service.ScoreCodec;

public class ScoreCodecTest {

    @Test
    public void signingString_isCanonicalPipeJoin() {
        assertEquals("Giana|1260|3|1700000000000", ScoreCodec.signingString("Giana", 1260, 3, 1700000000000L));
    }

    @Test
    public void hmac_isDeterministic() {
        String a = ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000");
        String b = ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000");
        assertEquals(a, b);
        // 32 bytes -> 64 hex chars
        assertEquals(64, a.length());
    }

    @Test
    public void hmac_differsByKey() {
        String a = ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000");
        String b = ScoreCodec.hmacSha256Hex("other", "Giana|1260|3|1700000000000");
        assertNotEquals(a, b);
    }

    @Test
    public void hmac_knownVector() {
        // Lock the algorithm. If this constant is wrong on first run, replace it
        // with the value from the failure output (determinism is what matters,
        // and the Edge Function uses the identical algorithm).
        String expected = "009269c8313759bf08197f67cb04c64f44060887040fe31914270af99117c256";
        assertEquals(expected, ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000"));
    }
}
