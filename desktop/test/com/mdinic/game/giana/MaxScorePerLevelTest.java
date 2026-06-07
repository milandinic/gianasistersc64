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
 * prints the maximum obtainable score per level and the running cumulative max
 * (score carries across levels). Output is used to set the server-side per-level
 * score ceiling in supabase/. Run: gradlew desktop:test --tests "*MaxScorePerLevelTest" --info
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

    @Test
    public void printMaxScorePerLevel() {
        int cumulative = 0;
        System.out.println("LEVEL | perLevelMax | cumulativeMax | breakdown");
        for (int level = 1; level <= GianaSistersScreen.LEVEL_COUNT; level++) {
            GameMap map = new GameMap(level, null);

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
            boolean boss = map.boss != null;

            int perLevel = diamonds * DIAMOND
                    + diamondBoxes * DIAMOND
                    + ballBoxes * TREAT
                    + killableGround * MONSTER
                    + bees * MONSTER
                    + (boss ? SPIDER : 0)
                    + MAX_TIME_BONUS;

            cumulative += perLevel;

            System.out.println(String.format(
                    "%5d | %11d | %13d | diamonds=%d diamondBox=%d ballBox=%d ground=%d bees=%d boss=%b",
                    level, perLevel, cumulative, diamonds, diamondBoxes, ballBoxes, killableGround, bees, boss));
        }
        System.out.println("TOTAL CUMULATIVE MAX = " + cumulative);
    }
}
