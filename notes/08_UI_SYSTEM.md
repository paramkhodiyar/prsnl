# prsnl — UI System

> Scope: Compose screens, navigation, theming, and toolbar/UX structure.
> Lives in `ui`, the only module allowed to depend on everything else (see
> `02_ARCHITECTURE.md` §3). Built AFTER the drawing engine works on a bare
> test screen — see `01_PRODUCT.md` §4 and `11_ROADMAP.md`.

## 1. Screen map (v1)

```
Home (Notebook Library)
 ├── Notebook grid/list, folders/subjects
 ├── New notebook creation
 └── → Notebook Detail

Notebook Detail (Page thumbnails)
 ├── Page grid, reorder, add/delete page
 └── → Page Editor

Page Editor (the core screen)
 ├── Canvas (drawing surface — see 04_DRAWING_ENGINE.md)
 ├── Top bar: back, page nav, undo/redo, export, more menu
 ├── Bottom/side toolbar: tool selection, color, width, shape picker
 └── Zoom/pan controls (gesture-driven, minimal chrome)

Settings (minimal for v1)
 ├── Light/dark mode override (system/light/dark)
 └── Default paper type
```

## 2. Theming

- Full light mode + dark mode, each with independently tuned colors for:
  paper background, ruled/grid line color, default ink colors (dark mode
  should not just literally invert — pick sensible dark-paper-friendly
  defaults).
- Theme is a first-class Compose `MaterialTheme` (or custom design tokens)
  — never hardcode colors inside drawing/toolbar composables.
- `Background.colorLight` / `colorDark` (see `03_DATA_MODEL.md`) are set
  per-page so a page's background renders correctly regardless of current
  app theme, independent of system theme if the user wants per-notebook
  overrides (stretch, not required v1).

## 3. Toolbar structure (keep minimal in v1)

```
[ Pen ] [ Highlighter ] [ Eraser ] [ Shape ] [ Select ] [ Text ] [ Image ]
   │
   └── tap-and-hold or secondary panel: color picker, width slider
```
- Avoid deep nested menus for core tools — one tap to switch tool.
- Shape tool: tapping opens a small picker (rectangle/ellipse/line/arrow);
  hold-to-recognize (see `06_SHAPE_ENGINE.md`) works regardless of which
  tool is active as long as it's Pen — it's a gesture on top of normal
  drawing, not a separate mode users must switch into.

## 4. Interaction rules

- All destructive actions (delete page, delete notebook) require
  confirmation.
- Undo/redo always visible/accessible, not buried in a menu.
- Zoom/pan must not conflict with drawing gestures — v1 approach: two-finger
  gesture = pan/zoom, single stylus/finger = draw (standard convention,
  matches most Android drawing apps).

## 5. Performance/UX rules

- Toolbar and chrome must never cause the canvas to redraw/recompose
  unnecessarily — keep drawing surface state isolated from Compose
  recomposition scope where possible (this matters more than it sounds;
  profile it in Phase 2).
- Page thumbnails (Notebook Detail screen) are generated async and cached,
  never regenerated synchronously on every screen visit.

## 6. Explicit non-goals for v1

- Custom template marketplace / browsing.
- Onboarding tutorial flows (nice to have, not blocking v1).
- Advanced settings (backup/restore UI, export presets) beyond what's
  listed above.

## 7. Test checklist

- [ ] Switching tools does not interrupt an in-progress stroke.
- [ ] Dark mode / light mode toggle updates all screens consistently,
      including in-progress page editor without requiring a re-open.
- [ ] Undo/redo buttons correctly disable when stack is empty.
- [ ] Two-finger pan/zoom never triggers accidental strokes.
- [ ] Page thumbnail list stays smooth-scrolling with 50+ pages.
