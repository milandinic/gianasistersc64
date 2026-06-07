package com.mdinic.game.giana;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.mdinic.game.giana.TreatBox.TreatType;
import com.mdinic.game.giana.screens.GianaSistersScreen;

/**
 * Not an assertion test: it analyzes every level via the real GameMap loader and
 * prints a per-level cumulative score ceiling used to set the server-side high-score
 * check in supabase/. The ceiling is ceiling[L] = perfectRun[L] + bonusBudget[L] +
 * repeatBudget[L]:
 *   - perfectRun: collectables + boss + full 990 time bonus, cumulated (score carries).
 *   - bonusBudget: for each level with a MAGICWATER bonus door, the measured max
 *     collectable score of its bonus map (bonuslevels.png, index level % 5); no time
 *     bonus or boss because bonus levels exit via the ride-up door, not LevelOverScreen.
 *   - repeatBudget: a generous upper bound for death+repeat re-grinding =
 *     maxLivesLostSoFar (3 start + earned, one life per 100 diamonds) * richest
 *     single-level re-grindable score so far (collectables only).
 * Output includes a ready-to-paste TS array and markdown rows.
 * Run: gradlew desktop:test --tests "*MaxScorePerLevelTest" --info
 */
public class MaxScorePerLevelTest {

    // Scoring rules (from the game code):
    private static final int DIAMOND = 25; // Diamond, DIAMOND treat box
    private static final int TREAT = 100; // BALL treat box when Giana can grow
    private static final int MONSTER = 50; // killable ground monster, bee
    private static final int SPIDER = 10000; // boss
    private static final int MAX_TIME_BONUS = 99 * 10; // finish at time=99 -> +990

    private HeadlessApplication app;

    @Before
    public void setUp() {
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
    }

    @After
    public void tearDown() {
        if (app != null) {
            app.exit();
            app = null;
        }
    }

    /** Collectable-only score for a map: diamonds, boxes, killable ground
     *  monsters, bees. Excludes the time bonus (awarded once on a clean clear,
     *  not on a re-grind) and the boss (not meaningfully re-grindable). Used for
     *  both bonus-map score and the per-level re-grindable amount. */
    private int collectableScore(GameMap map) {
        int diamonds = map.diamonds.size;
        int ballBoxes = 0;
        int diamondBoxes = 0;
        for (TreatBox b : map.treatBoxes) {
            if (!b.active) {
                continue; // TREAT_BOX_USED: worth nothing
            }
            if (b.type == TreatType.BALL) {
                ballBoxes++;
            } else {
                diamondBoxes++;
            }
        }
        int killableGround = 0;
        for (GroundMonster m : map.groundMonsters) {
            if (m.type.canBeKilled) {
                killableGround++;
            }
        }
        int bees = map.bees.size;
        return diamonds * DIAMOND
                + diamondBoxes * DIAMOND
                + ballBoxes * TREAT
                + killableGround * MONSTER
                + bees * MONSTER;
    }

    /** Number of collectable diamonds on a map (diamonds + every active box,
     *  since each box yields one diamond via collectDiamound). Used to track
     *  earned lives (one per 100 diamonds). */
    private int collectableDiamonds(GameMap map) {
        int boxes = 0;
        for (TreatBox b : map.treatBoxes) {
            if (b.active) {
                boxes++;
            }
        }
        return map.diamonds.size + boxes;
    }

    @Test
    public void printMaxScorePerLevel() {
        int cumulative = 0;
        int cumulativeDiamonds = 0;
        int richestReGrindable = 0;
        int[] ceiling = new int[GianaSistersScreen.LEVEL_COUNT + 1]; // index 0 unused

        System.out.println("LEVEL | perfectRun | bonus | repeatBudget | cumulativeCeiling | breakdown");
        for (int level = 1; level <= GianaSistersScreen.LEVEL_COUNT; level++) {
            GameMap map = new GameMap(level, null);

            // --- perfect-run score for this level (collectables + boss + time) ---
            boolean boss = map.boss != null;
            int perLevel = collectableScore(map)
                    + (boss ? SPIDER : 0)
                    + MAX_TIME_BONUS;

            // --- measured bonus-level score, if this level has a bonus door ---
            int bonus = 0;
            if (map.bonusLevelDoor != null) {
                try {
                    GameMap bonusMap = new GameMap(level % 5, null, true);
                    bonus = collectableScore(bonusMap); // no time bonus, no boss
                } catch (RuntimeException e) {
                    // Defensive: a bonus index that fails to load contributes 0.
                    bonus = 0;
                }
            }

            // --- repeat budget: maxLivesLost * richest re-grindable level so far ---
            int reGrindable = collectableScore(map); // excludes time + boss
            if (reGrindable > richestReGrindable) {
                richestReGrindable = reGrindable;
            }
            cumulativeDiamonds += collectableDiamonds(map);
            int maxLives = 3 + (cumulativeDiamonds / 100);
            int repeatBudget = maxLives * richestReGrindable;

            cumulative += perLevel + bonus;
            ceiling[level] = cumulative + repeatBudget;

            System.out.println(String.format(
                    "%5d | %10d | %5d | %12d | %17d | bonusDoor=%b boss=%b maxLives=%d",
                    level, perLevel, bonus, repeatBudget, ceiling[level],
                    map.bonusLevelDoor != null, boss, maxLives));
        }

        // --- copy-paste output for downstream files ---
        System.out.println();
        System.out.println("=== Paste into supabase/functions/submit-score/index.ts CUMULATIVE_MAX ===");
        StringBuilder ts = new StringBuilder("  0, // level 0 (unused)\n  ");
        for (int level = 1; level <= GianaSistersScreen.LEVEL_COUNT; level++) {
            ts.append(ceiling[level]);
            if (level < GianaSistersScreen.LEVEL_COUNT) {
                ts.append(", ");
            }
            if (level % 10 == 0 && level < GianaSistersScreen.LEVEL_COUNT) {
                ts.append("\n  ");
            }
        }
        System.out.println(ts.toString());

        System.out.println();
        System.out.println("=== Markdown table rows (Lvl | Cumulative ceiling) ===");
        for (int level = 1; level <= GianaSistersScreen.LEVEL_COUNT; level++) {
            System.out.println(String.format("| %d | %,d |", level, ceiling[level]));
        }
        System.out.println("ALL-LEVELS CEILING = " + ceiling[GianaSistersScreen.LEVEL_COUNT]);
    }
}
