package com.mdinic.game.giana.screens;

/**
 * Pure, libGDX-free geometry for the in-game name-entry letter picker.
 *
 * Owns the cell list (A-Z, 0-9, plus DEL and SPC) and their rectangles in
 * virtual 480x320 coordinates, plus the two action regions DONE and SKIP. The
 * renderer and the touch hit-test both read this single source of geometry, so
 * what is drawn and what is tappable can never drift apart.
 *
 * No {@code Gdx.*}, no {@code SpriteBatch} - just floats and chars - so this is
 * the unit-tested part (the most off-by-one-prone code).
 */
public final class NameGridLayout {

    /** Backspace cell label. */
    public static final String DEL = "DEL";
    /** Space cell label. */
    public static final String SPC = "SPC";

    private static final int COLUMNS = 10;
    private static final int ROWS = 4;

    /** Virtual-screen extent the grid is laid out against. */
    private static final float SCREEN_WIDTH = 480;

    /** Grid horizontal margins and vertical span (virtual coords). */
    private static final float GRID_LEFT = 30;
    private static final float GRID_RIGHT = SCREEN_WIDTH - 30;
    private static final float GRID_TOP_Y = 210; // y of the top row's center band
    private static final float GRID_BOTTOM_Y = 40; // y of the bottom row's center band

    private final String[] labels;
    private final Rect[] cells;

    public NameGridLayout() {
        labels = buildLabels();
        cells = new Rect[labels.length];

        float colStep = (GRID_RIGHT - GRID_LEFT) / COLUMNS;
        float rowStep = (GRID_TOP_Y - GRID_BOTTOM_Y) / (ROWS - 1);
        // Cells fill their slot with a small gap so adjacent cells never overlap.
        float cellW = colStep * 0.9f;
        float cellH = rowStep * 0.6f;

        for (int i = 0; i < labels.length; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float cx = GRID_LEFT + colStep * (col + 0.5f);
            float cy = GRID_TOP_Y - rowStep * row;
            cells[i] = new Rect(cx - cellW / 2, cy - cellH / 2, cellW, cellH);
        }
    }

    private static String[] buildLabels() {
        String[] out = new String[26 + 10 + 2];
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            out[i++] = String.valueOf(c);
        }
        for (char c = '0'; c <= '9'; c++) {
            out[i++] = String.valueOf(c);
        }
        out[i++] = DEL;
        out[i++] = SPC;
        return out;
    }

    public int cellCount() {
        return cells.length;
    }

    public String cellLabel(int i) {
        return labels[i];
    }

    public Rect cellRect(int i) {
        return cells[i];
    }

    /** True if cell {@code i} is the backspace action. */
    public boolean isDel(int i) {
        return DEL.equals(labels[i]);
    }

    /** True if cell {@code i} is the space action. */
    public boolean isSpc(int i) {
        return SPC.equals(labels[i]);
    }

    /**
     * @return index of the character cell containing virtual point (vx, vy), or
     *         -1 if the point is outside every cell.
     */
    public int hitTest(float vx, float vy) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i].contains(vx, vy)) {
                return i;
            }
        }
        return -1;
    }

    /** DONE action region (top-left). */
    public Rect doneRect() {
        return new Rect(15, 290, 90, 25);
    }

    /** SKIP action region (top-right). */
    public Rect skipRect() {
        return new Rect(SCREEN_WIDTH - 80, 290, 70, 25);
    }

    public boolean hitDone(float vx, float vy) {
        return doneRect().contains(vx, vy);
    }

    public boolean hitSkip(float vx, float vy) {
        return skipRect().contains(vx, vy);
    }

    /** Simple axis-aligned rectangle in virtual coords (origin bottom-left). */
    public static final class Rect {
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        public Rect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}
