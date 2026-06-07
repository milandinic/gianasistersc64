# EnterYourNameScreen — in-game letter picker

Date: 2026-06-07

## Problem

The previous Android fix (commit `93da78e`) assumed the soft keyboard covers
"the bottom half" of the screen and moved the name line up to virtual y=155
(just above the 320÷2=160 midpoint). It did not work: the name is **still
hidden under the keyboard**.

**Root cause:** the app is locked to **landscape**
(`android/AndroidManifest.xml`: `android:screenOrientation="landscape"`). In
landscape, the Android soft keyboard covers far more than half the screen —
typically 60–75% of the height. The whole 480×320 virtual screen is stretched
across the full physical height (`SpriteBatch` ortho `0,0,480,320`), so the
keyboard's top edge lands well *above* virtual-Y 160. A name at y=155 is under
it. Any "move it up by N pixels" fix is a guess against an unknown,
device-dependent keyboard height — fragile by construction.

The prior fix's `submitted` latch *did* help: the screen no longer auto-skips
("it no longer just continues" — confirmed better by the user). Only the
visibility problem remains.

## Decision

**Remove the native soft keyboard entirely.** Draw an in-game, tappable letter
grid (A–Z, 0–9, plus `DEL` and `SPC`) using the existing `yellowFont12`. With no
native keyboard, nothing can ever overlap the name. The layout is fully
controlled by us, deterministic, identical on desktop and Android, and the
selection math is unit-testable.

This was "considered and rejected" in the prior design — but that rejection
rested on the (wrong) assumption that nudging text upward would suffice.

## Layout (virtual 480×320, top-anchored)

```
  DONE                          SKIP        y≈305   text buttons, top corners
      NAME: MILAN_                          y≈275   always visible (top)

  A B C D E F G H I J                       grid: 10 columns × 4 rows
  K L M N O P Q R S T                       evenly spaced, ~210 down to ~40
  U V W X Y Z 0 1 2 3
  4 5 6 7 8 9 DEL SPC                        last row has 8 cells (2 empty slots)
```

The 38 cells are: `A`–`Z` (26), `0`–`9` (10), then two action cells `DEL`
(backspace) and `SPC` (space). Laid out left-to-right, top-to-bottom in a
10-wide grid: 3 full rows of 10, then a 4th row of 8 (the last two column slots
are empty). The space cell renders as `SPC`; backspace as `DEL`.

## Behavior

- **`show()`**: pre-fill the name with the player's last name (existing
  `getMyBest()` logic). Do **not** call `setOnscreenKeyboardVisible(true)` — the
  keyboard is never raised.
- **Selection model: direct tap.** Each cell is its own touch target. Tapping a
  letter/digit appends it (subject to `MAX_NAME = 12`); `DEL` removes the last
  char; `SPC` appends a space. Because the grid only offers valid characters,
  the old A–Z/0–9/space filtering is implicit.
- **No press highlight.** The name line updating is the only feedback (user's
  choice — keep it simple).
- **Grid only — no hardware keyboard.** Remove the `keyTyped`/`keyDown`
  `InputAdapter` and `setInputProcessor`. Selection is uniform on both platforms
  (click on desktop, tap on Android). DONE/SKIP are tappable cells too; there is
  no hardware Enter/Esc path any more.
- **DONE** (top-left) → `confirm()` (save score, go to `HighScoreScreen`).
  **SKIP** (top-right) → `skip()` (go without saving).
- Keep the `submitted` latch (leave/submit at most once) and the `touchConsumed`
  latch (one tap fires at most one action).

## Architecture

Split the screen into two units:

1. **`NameGridLayout`** (new, in `core/.../screens/`): a pure, libGDX-free
   value object that owns the cell list and geometry. Given the virtual screen
   constants it computes each cell's rectangle, and exposes:
   - `cellCount()`, `cellLabel(i)`, `cellRect(i)` (or x/y/w/h) for rendering;
   - `hitTest(float vx, float vy)` → the index of the cell containing virtual
     point (vx, vy), or `-1`;
   - `DONE_RECT` and `SKIP_RECT` as two named regions (kept separate from the
     character cells, since they sit in a different visual band and trigger
     actions, not appends). The renderer reads the same rects, so draw and
     hit-test share one source of geometry and can't drift.

   No `Gdx.*`, no `SpriteBatch` — just floats and chars. **This is the
   unit-tested part** (the most off-by-one-prone code).

2. **`EnterYourNameScreen`** (rewritten): owns the `StringBuilder name`, the
   `submitted`/`touchConsumed`/`blinkTime` state, the `SpriteBatch`, and the
   screen lifecycle. `render()` draws DONE/SKIP, the name line with blinking
   cursor, and every grid cell via `NameGridLayout`. `processTouches()` converts
   each active pointer's screen X/Y to virtual coords (existing transform) and
   calls `NameGridLayout.hitTest(...)`; the returned cell drives
   append/delete/space/done/skip.

## Data flow

touch (screen px) → virtual (vx, vy) via existing
`x = getX()/graphics.width*480`, `y = 320 - getY()/graphics.height*320`
→ `NameGridLayout.hitTest(vx, vy)` → cell index → mutate `name` /
`confirm()` / `skip()`. Render reads `name` + `NameGridLayout` geometry.

## Error / edge handling

- Append no-ops when `name.length() == MAX_NAME`.
- `DEL` no-ops on an empty name.
- `confirm()` with an empty/whitespace name skips the save (existing behavior:
  only save when trimmed name is non-empty), then proceeds to `HighScoreScreen`.
- Multi-pointer: keep scanning pointers 0 and 1 (a stray second touch shouldn't
  block a real tap), but `touchConsumed` still gates to one action per press.

## Testing

- **Unit test `NameGridLayout`** (JUnit 4, under `desktop/test`, no libGDX
  backend needed since it's pure): cell count is 38 (26+10+DEL+SPC); `hitTest`
  on each cell's center returns that cell's index; `hitTest` outside the grid
  returns -1; DONE/SKIP regions hit-test correctly; adjacent cells don't overlap
  (a point in cell N never resolves to N±1).
- Screen UI itself stays untested (consistent with every other screen).
- Manual verification: desktop run (click cells, DONE/SKIP, cursor blink) and an
  Android device check (name fully visible, no keyboard, taps land on the right
  letters).

## Out of scope

- No cursor/confirm arcade picker (rejected: too many taps per letter on touch).
- No press-highlight animation.
- No new artwork — all text via `yellowFont12`.
- No change to `HighScoreScreen`, score submission, or the Supabase backend.
- `data/scoresprites.png` is already unused (removed in the prior fix); no change.
