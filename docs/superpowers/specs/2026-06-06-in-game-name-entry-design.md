# In-game name entry on the high-score screen

## Problem

When the player dies with no lives left and their score qualifies, the game
shows `EnterYourNameScreen` (pen icon = type your name, fast-forward icon =
skip). Clicking the pen is supposed to let the player type their name, but
nothing happens — the screen jumps straight to the high-score list.

### Root cause (verified by runtime log)

`EnterYourNameScreen.processKeys()` calls `Gdx.input.getTextInput(listener, ...)`.
On the **LWJGL3 desktop backend this method is not implemented**: it immediately
invokes `listener.canceled()`. Instrumented log from a real run:

```
[EnterName] touched x0=40 y0=34 leftBtn=true rightBtn=false processKeys=true
[EnterName] PEN clicked -> requesting text input
[EnterName] text input canceled        <-- fired with no chance to type
```

So the hit test and code path are fine; the platform call is the failure.

### Why not just upgrade the libGDX API

LWJGL3 is built on GLFW, and GLFW and AWT/Swing cannot coexist on the same
thread, so neither the legacy `getTextInput(...)` nor the newer
`openTextInputField(NativeInputConfiguration)` (PR #7004) is implemented on the
LWJGL3 desktop backend. **No single built-in libGDX call gives working text
input on both LWJGL3 desktop and Android.** The libGDX-recommended cross-platform
answer is a custom in-game text field.

## Goal

Replace the broken native dialog with an in-game text field rendered by the
game's own bitmap font and driven by libGDX keyboard events, working identically
on desktop and Android. No platform-specific code, no `core`/launcher/service
API changes.

## Scope

One file changes meaningfully: `core/.../screens/EnterYourNameScreen.java`.

## Behavior

1. The screen still draws the existing "CONGRATULATION / YOU CAN TYPE YOUR NAME /
   IN THE HALL OF FAME / YOUR SCORE" text plus the pen sprite (left) and
   fast-forward sprite (right).
2. **Tapping the pen** enters *edit mode* (instead of calling `getTextInput`):
   - Pre-fill the name buffer with the player's last name
     (`getHighScoreService().getMyBest().getName()`, or empty if none).
   - Install an `InputProcessor` (`InputAdapter`) handling `keyTyped`/`keyDown`.
   - On Android, call `Gdx.input.setOnscreenKeyboardVisible(true)` so the soft
     keyboard appears. (No-op effect on desktop.)
3. **While editing**, render the buffer with a blinking cursor, e.g.
   `NAME: ABC_`, using `renderer.yellowFont12`.
   - `keyTyped(char)` appends the char if it is in the whitelist
     **uppercase A–Z, 0–9, space** (lowercase letters are upper-cased), capped at
     **12 characters**.
   - **Backspace** removes the last char.
   - **Enter** confirms; **Escape** cancels.
4. **Confirm** (Enter key, or tapping the pen again while editing):
   - Hide the on-screen keyboard.
   - If the trimmed buffer is non-empty: build
     `new Score(name, oldMap.score, oldMap.level + 1)` and call
     `saveHighScore(score)`.
   - Go to `HighScoreScreen`.
5. **Cancel** (Escape, or empty confirm): hide keyboard, go to
   `HighScoreScreen` (same destination as skip).
6. **Tapping fast-forward** still skips directly to `HighScoreScreen`.

## Edge cases

- Remove the existing `processKeys`-set-to-false-and-never-reset latch (a latent
  second bug that would have dead-locked the pen after one click).
- Debounce the opening tap so the same touch that enters edit mode does not also
  register as a confirm tap.
- Clear the input processor (`Gdx.input.setInputProcessor(null)`) and hide the
  on-screen keyboard in `hide()` so nothing leaks into the next screen.
- Length cap (12) keeps the name within the `%-22s` hall-of-fame format.

## Testing / verification

This is GLFW/keyboard UI; the honest verification is a manual run on desktop:
die with 0 lives at a qualifying score, click the pen, type a name, press Enter,
and confirm the name appears in the hall of fame. Temporary debug logging is kept
during verification and removed afterward. The existing `MapTest` smoke test is
unaffected (it does not load screens).
