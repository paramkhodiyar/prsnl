# prsnl — Product Specification

> This file is the source of truth for WHAT we are building. If a feature isn't
> in this doc, it isn't in v1. AI agents must not add features not listed here
> without the doc being updated first.

## 1. One-line pitch

A beautiful, fast, Android-first notebook app for stylus users (Xiaomi Focus
Pen Pro target hardware) that combines handwriting, smart shapes, graphs, and
PDF annotation — built as an object-based document, not pixel screenshots.

## 2. Target user

- Student or professional using an Android tablet + active stylus.
- Primary hardware target: Xiaomi Pad (7/8 family) + Focus Pen Pro.
- Secondary: any Android tablet with a standard stylus (S Pen, generic).
- NOT targeting: phones, non-stylus use, iOS, desktop.

## 3. v1 scope (what ships in the first real release)

### In scope
- Notebooks → Pages, organized in folders/subjects.
- Page backgrounds: blank, ruled (lines), grid, dotted — light + dark mode,
  each independently themeable.
- Core tools: pen, highlighter, eraser (stroke-eraser + pixel-eraser modes).
- Pressure-sensitive strokes (variable width from pressure).
- Undo/redo (command-based, not snapshot-based).
- Pan + zoom on infinite/scrollable page canvas.
- Shape tool: manual shape insertion (rectangle, ellipse, line, arrow).
- Hold-to-recognize: draw a rough shape, hold pen still ~300–500ms at stroke
  end → auto-converts to clean shape.
- Selection tool: select stroke/shape/text/image, move, resize, rotate,
  delete, recolor.
- Local persistence (Room + file storage) — documents survive app restarts,
  fully offline.
- PDF export of a notebook/page (vector-based, not rasterized where possible).
- PDF import + annotate on top of PDF pages.
- Basic text boxes (typed text, not handwriting recognition).
- Basic image insertion (from gallery).

### Explicitly OUT of scope for v1 (do not build, do not scaffold)
- Handwriting-to-text recognition.
- Search inside handwritten content.
- Cloud sync / multi-device / accounts.
- Collaboration / sharing / real-time multi-user.
- Math equation rendering / LaTeX.
- Smart/parametric graphs (x², sin(x), plotting functions).
- Xiaomi-proprietary pen gestures (double-press, slide, rotation) beyond
  what standard Android `MotionEvent` exposes.
- Tilt-based shading effects.
- Custom template marketplace.
- Any monetization/billing.

These are Phase 5+ candidates (see `11_ROADMAP.md`) and must not be
half-implemented "just in case" during earlier phases.

## 4. Non-negotiable product principles

1. **Document = objects, not pixels.** A page is a list of typed elements
   (strokes, shapes, text, images, PDF refs), never a bitmap/screenshot.
   This is the single decision that determines whether the app can ever
   support undo, resize, PDF re-export, or zoom without quality loss.
2. **Drawing engine ships before pretty UI.** No home screen animations,
   no fancy toolbars, until raw stylus input feels correct on a blank
   canvas screen.
3. **Every module has one job.** Drawing engine does not know PDF exists.
   PDF engine does not know about shape recognition. See `02_ARCHITECTURE.md`.
4. **No AI-driven architecture changes without updating the docs first.**
   If an implementation reveals the architecture is wrong, that's a
   docs-update conversation, not a silent rewrite.

## 5. Success criteria for v1

- Writing on a Focus Pen Pro feels close to native (no visible lag, pressure
  affects stroke width, no palm-triggered marks).
- Hold-to-recognize correctly converts rough circle/rectangle/line/arrow at
  least 90% of the time in casual testing.
- A 20-page notebook with mixed strokes/shapes/PDF exports to a correct PDF
  in under 5 seconds on target hardware.
- App cold-starts and opens last notebook in under 2 seconds.
- Zero data loss on force-close (autosave/durable writes).

## 6. Naming / branding notes

- App name: **prsnl**
- Positioning: "a personal notebook, not a Goodnotes clone" — lean into the
  Xiaomi Focus Pen Pro niche rather than trying to support every tablet.
