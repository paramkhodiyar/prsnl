# prsnl — Stylus Input

> Scope: reading input correctly off Android's `MotionEvent` API, with
> Xiaomi Focus Pen Pro as the primary target device and generic Android
> styluses as the fallback baseline.

## 1. Two tiers of support (do not conflate them)

### Tier 1 — Standard Android stylus input (build this, fully, v1)
Available via public `MotionEvent` APIs on any Android device with a
supported active stylus:
- x / y coordinates
- pressure (`MotionEvent.getPressure()`)
- tool type discrimination (`TOOL_TYPE_STYLUS` vs `TOOL_TYPE_FINGER` vs
  `TOOL_TYPE_ERASER`)
- orientation / tilt axes where the device reports them
- hover events (stylus near but not touching screen) where supported

This tier is what the entire drawing engine is built against. It works on
Focus Pen Pro AND any other decent Android stylus.

### Tier 2 — Xiaomi-proprietary pen gestures (spike only, not core path)
Focus Pen Pro markets additional interactions (double-press, slide,
rotation gestures, haptic feedback) that are documented as part of Xiaomi's
own Mi Canvas experience. Whether these are exposed through a public
third-party API is NOT assumed — this must be verified with a small,
isolated spike (see Phase 6 in `11_ROADMAP.md`) before any product feature
depends on it. If no public API exists, Tier 2 features are dropped
entirely rather than half-built.

**Rule:** no core `drawing` module code may depend on Tier 2 APIs. If a
Tier 2 integration is built, it lives in an isolated, optional module that
degrades gracefully to Tier 1 behavior when unavailable.

## 2. Palm rejection strategy (v1)

- Use `MotionEvent.getToolType()` to distinguish stylus vs. finger contact
  where possible — many devices report palm touches as `TOOL_TYPE_FINGER`
  or with a distinct pointer ID while a stylus is active.
- While a stylus pointer is actively down/hovering, ignore concurrent
  finger-type pointer events on the drawing surface (standard "stylus
  priority" pattern).
- Do NOT attempt custom palm-shape heuristics in v1 (contact-size-based
  guessing) — this is a rabbit hole; rely on tool-type discrimination first
  and revisit only if real-device testing shows it's insufficient.

## 3. Pressure normalization

- Raw pressure range varies by device/driver. On first use of a new
  device, sample a normalization curve (or use a fixed reasonable default
  range, e.g. clamp raw values to an expected min/max and linearly map to
  0.0–1.0).
- Store normalized 0.0–1.0 pressure in `StrokePoint` (see
  `03_DATA_MODEL.md`), never raw device units — keeps rendering logic
  device-independent.

## 4. Input event handling rules

- Handle `ACTION_DOWN`, `ACTION_MOVE` (may contain **historical points** via
  `MotionEvent.getHistoricalX/Y/Pressure` — use these, don't discard them,
  they're free extra resolution between frames), `ACTION_UP`,
  `ACTION_CANCEL`.
- Always handle `ACTION_CANCEL` explicitly (e.g. app loses focus mid-stroke)
  — finalize or discard the in-progress stroke cleanly, never leave the
  engine in a stuck "pointer down" state.
- Hover events (`ACTION_HOVER_MOVE`) are used for cursor/preview feedback
  only in v1 (e.g. showing brush size preview), not for drawing.

## 5. Device testing matrix

| Device | Tier 1 required | Tier 2 explored |
|---|---|---|
| Xiaomi Pad 7/8 + Focus Pen Pro | Yes | Yes (spike) |
| Any Android tablet + generic active stylus | Yes | No |
| Finger-only (no stylus) | Basic draw works, no pressure | N/A |

## 6. Explicit non-goals for v1

- Tilt-based shading/brush angle effects.
- Any device-specific driver-level integration outside public Android APIs.
