package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.mdinic.game.giana.service.Score;
import com.mdinic.game.giana.service.ScoreStore;

public class ScoreStoreTest {

    @Test
    public void scoreList_roundTrips() {
        List<Score> in = new ArrayList<Score>();
        in.add(new Score("Maria", 1630, 2));
        in.add(new Score("Milan", 1530, 1));

        String s = ScoreStore.scoresToJson(in);
        List<Score> out = ScoreStore.scoresFromJson(s);

        assertEquals(2, out.size());
        assertEquals("Maria", out.get(0).getName());
        assertEquals(1630, out.get(0).getScore());
        assertEquals(2, out.get(0).getLevel());
        assertEquals("Milan", out.get(1).getName());
        assertEquals(1530, out.get(1).getScore());
    }

    @Test
    public void scoresFromJson_nullOrBlank_returnsEmpty() {
        assertEquals(0, ScoreStore.scoresFromJson(null).size());
        assertEquals(0, ScoreStore.scoresFromJson("").size());
    }

    @Test
    public void outbox_roundTrips() {
        java.util.List<com.mdinic.game.giana.service.PendingSubmit> in =
            new java.util.ArrayList<com.mdinic.game.giana.service.PendingSubmit>();
        in.add(new com.mdinic.game.giana.service.PendingSubmit("Giana", 1260, 3, 1700000000000L, "abc123"));
        in.add(new com.mdinic.game.giana.service.PendingSubmit("Anna", 999, 1, 1700000001111L, "def456"));

        String s = com.mdinic.game.giana.service.ScoreStore.outboxToJson(in);
        java.util.List<com.mdinic.game.giana.service.PendingSubmit> out =
            com.mdinic.game.giana.service.ScoreStore.outboxFromJson(s);

        assertEquals(2, out.size());
        assertEquals("Giana", out.get(0).name);
        assertEquals(1260, out.get(0).score);
        assertEquals(3, out.get(0).level);
        assertEquals(1700000000000L, out.get(0).ts);
        assertEquals("abc123", out.get(0).sig);
        assertEquals("Anna", out.get(1).name);
    }

    @Test
    public void outboxFromJson_nullOrBlank_returnsEmpty() {
        assertEquals(0, com.mdinic.game.giana.service.ScoreStore.outboxFromJson(null).size());
        assertEquals(0, com.mdinic.game.giana.service.ScoreStore.outboxFromJson("").size());
    }
}
