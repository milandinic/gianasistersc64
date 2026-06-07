# EnterYourNameScreen — Android-friendly layout

Date: 2026-06-07

## Problem

On Android, the name-entry screen (`EnterYourNameScreen`) is a poor experience:

1. **The soft keyboard covers the name being typed.** The name line is drawn at
   y=100 in a virtual 480×320 screen; the Android soft keyboard slides up over
   the bottom ~half and hides it, so the user can't see what they type.
2. **Tapping the screen unexpectedly "finishes" entry.** The pen touch target
   (bottom-left, `x < 70`) calls `confirm()` and the fast-forward target
   (bottom-right) calls `skip()`. On a phone the keyboard occupies the bottom
   corners where those targets live, so tapping a keyboard key near a corner
   fires confirm/skip — the entry ends before the user is done.

Desktop is unaffected: the LWJGL3 backend shows no soft keyboard, so nothing
overlaps and the bottom-corner targets are reachable with the mouse.

**Root cause:** the layout assumes a full-height screen, but on Android the soft
keyboard owns the bottom ~half — colliding with both the name display and the
interactive touch targets.

## Decision

Keep the Android soft keyboard, but move everything the user must see or tap
into the **top half** of the screen, above where the keyboard sits. Replace the
pen/fast-forward icons with explicit text buttons.

## Layout (virtual 480×320, top-anchored)

```
 DONE                                    SKIP      y ~290  text buttons, top corners
        CONGRATULATION!                            y 250
   YOU CAN TYPE YOUR NAME                          y 230
     IN THE HALL OF FAME                           y 210
        YOUR SCORE 0001234                         y 185
   NAME: MILAN_                                    y 155   always visible
 ───────────────────────────────────────────
            (Android soft keyboard)                bottom half
```

## Behavior

- **Auto-start editing** in `show()`: enter editing mode, pre-fill the name with
  the player's last name (existing `getMyBest()` logic), and raise the keyboard.
  The pen / "tap to start typing" step is removed.
- **Two touch targets**, both rendered as text with the existing `yellowFont12`
  (no new art), hit-tested only in the top band (`y` above ~270 in virtual
  coords):
  - **DONE** (top-left) → `confirm()` — save the score, go to `HighScoreScreen`.
  - **SKIP** (top-right) → `skip()` — go to `HighScoreScreen` without saving.
- Keep keyboard paths unchanged: typing appends (filtered to A–Z, 0–9, space,
  `MAX_NAME` cap), backspace deletes, hardware **Enter** = DONE, **ESC/BACK** =
  SKIP.
- A tap that is not on a button does nothing (no accidental finish).
- Keep the `submitted` latch so the screen leaves / submits at most once.

## Implementation notes

- Remove the `scoresprites.png` texture load and the `left`/`right`
  `TextureRegion` fields (and their disposal in `hide()`); they're no longer
  used.
- In `processTouches()`, compute `y` for *both* pointers (currently only `y0` is
  computed). Each button tests its own pointer's `x` and `y`. The top-band `y`
  requirement is what prevents bottom-of-screen keyboard taps from matching.
- Keep the multi-pointer scan (pointers 0 and 1): on Android a keyboard touch
  and a button touch can be down at the same time.
- Update the class Javadoc to describe the new top-anchored layout and
  auto-edit-on-show behavior (the cross-platform custom-text-field rationale
  stays — `Gdx.input.getTextInput` is still unusable on LWJGL3).

## Testing

This screen is pure libGDX UI (touch coordinates, soft keyboard, `setScreen`),
which the existing suite does not unit-test — consistent with every other screen
(only `GameMap` / codec / config logic has unit tests). No new automated test.
Verification is a desktop build/run for the top-anchored layout and font
buttons; the Android keyboard-overlap fix is confirmed on a device.

## Out of scope

- No arcade-style on-screen letter picker (considered and rejected; we keep the
  native soft keyboard).
- No new artwork; buttons are text.
- No change to desktop behavior beyond the shared layout (Enter/typing still
  work; the DONE/SKIP text is also mouse-clickable at the top).
