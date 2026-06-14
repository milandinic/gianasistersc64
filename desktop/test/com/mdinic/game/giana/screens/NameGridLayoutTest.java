package com.mdinic.game.giana.screens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mdinic.game.giana.screens.NameGridLayout.Rect;

public class NameGridLayoutTest {

    private final NameGridLayout layout = new NameGridLayout();

    @Test
    public void hasThirtyEightCells() {
        // 26 letters + 10 digits + DEL + SPC
        assertEquals(38, layout.cellCount());
    }

    @Test
    public void labelsAreAToZThenZeroToNineThenDelSpc() {
        assertEquals("A", layout.cellLabel(0));
        assertEquals("Z", layout.cellLabel(25));
        assertEquals("0", layout.cellLabel(26));
        assertEquals("9", layout.cellLabel(35));
        assertEquals(NameGridLayout.DEL, layout.cellLabel(36));
        assertEquals(NameGridLayout.SPC, layout.cellLabel(37));
        assertTrue(layout.isDel(36));
        assertTrue(layout.isSpc(37));
        assertFalse(layout.isDel(0));
    }

    @Test
    public void hitTestOnEachCellCenterReturnsThatCell() {
        for (int i = 0; i < layout.cellCount(); i++) {
            Rect r = layout.cellRect(i);
            float cx = r.x + r.w / 2;
            float cy = r.y + r.h / 2;
            assertEquals("cell " + i + " center", i, layout.hitTest(cx, cy));
        }
    }

    @Test
    public void hitTestOutsideGridReturnsMinusOne() {
        assertEquals(-1, layout.hitTest(0, 0));
        assertEquals(-1, layout.hitTest(479, 319));
        assertEquals(-1, layout.hitTest(240, 305)); // up in the DONE/SKIP band
    }

    @Test
    public void adjacentCellsDoNotOverlap() {
        // A point in cell N must never resolve to N+/-1.
        for (int i = 0; i < layout.cellCount(); i++) {
            Rect r = layout.cellRect(i);
            float cx = r.x + r.w / 2;
            float cy = r.y + r.h / 2;
            int hit = layout.hitTest(cx, cy);
            assertEquals(i, hit);
        }
    }

    @Test
    public void doneAndSkipRegionsHitTestCorrectly() {
        Rect done = layout.doneRect();
        assertTrue(layout.hitDone(done.x + done.w / 2, done.y + done.h / 2));
        assertFalse(layout.hitSkip(done.x + done.w / 2, done.y + done.h / 2));

        Rect skip = layout.skipRect();
        assertTrue(layout.hitSkip(skip.x + skip.w / 2, skip.y + skip.h / 2));
        assertFalse(layout.hitDone(skip.x + skip.w / 2, skip.y + skip.h / 2));
    }
}
