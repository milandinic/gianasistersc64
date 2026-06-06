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
}
