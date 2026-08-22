# prsnl — Drawing Engine

> Scope: everything that turns pointer input into rendered ink and manages
> on-canvas editing (selection/move/resize). Lives in the `drawing` module.
> Depends on `document` + `core` only (see `02_ARCHITECTURE.md` §3).

## 1. Pipeline overview

```
MotionEvent (from stylus/finger)
     │
     ▼
Input Filter          — discard palm-rejection candidates, route by tool type
     │
     ▼
Point Sampler          — collects raw points + pressure + timestamp
     │
     ▼
Smoothing/Prediction    — reduces jitter, optional input-latency compensation
     │
     ▼
Live Stroke Renderer    — draws the in-progress stroke immediately (this
                           frame, no waiting on commit) for perceived latency
     │
     ▼
Stroke Commit           — on pointer-up, finalize StrokePoint list, create
                           Stroke element, issue AddElement command
     │
     ▼
Shape Recognizer (conditional, see 06_SHAPE_ENGINE.md)
     │
     ▼
Document Model update → Repository (async persist)
```

## 2. Latency budget (this is the whole ballgame)

Target: input-to-pixel latency indistinguishable from native drawing apps.

- Live stroke segment must be drawn on the SAME frame the input arrives —
  do not batch multiple pointer events before rendering.
- Use `View.OnTouchListener`/raw pointer APIs, not a higher-level
  gesture-detection abstraction that adds a frame of buffering.
- If using Compose Canvas, verify in the Phase 2 spike that pointer input
  latency is acceptable; if not, fall back to a `SurfaceView`/custom `View`
  purely for the drawing surface, with Compose used only for the
  surrounding chrome (toolbars, etc).
- Persistence must never block the render thread — always async, debounced.

## 3. Live vs. committed strokes

- While the pointer is down: render from an in-memory "active stroke"
  buffer directly, bypassing the full Document Model diffing.
- On pointer-up: convert active stroke buffer into a `Stroke` element,
  issue `AddElement` command, THEN let normal document-driven rendering
  take over.
- This two-path rendering (fast live path + document-driven committed path)
  is intentional and should not be "simplified" into a single path — that
  simplification is what reintroduces lag.

## 4. Eraser modes (v1: two modes, keep separate code paths)

- **Stroke eraser**: touching any point of a `Stroke` deletes the whole
  stroke (issue `DeleteElement`).
- **Pixel eraser**: splits a `Stroke` into multiple `Stroke`s at the erased
  segment (issue `DeleteElement` + `AddElement` for the remaining pieces,
  wrapped as a single undo-able compound command).

## 5. Selection & transform

- Selection is hit-testing against `Element.boundingBox`, refined against
  actual geometry for strokes (bounding box first pass, then per-point
  distance check for precision).
- Move/resize/rotate operate on a temporary transform matrix during the
  gesture; only on gesture-end is a `MoveElement`/`ResizeElement` command
  issued — do not spam the undo stack with per-frame updates.
- Multi-select: group of elements moved together; issue one compound
  command per gesture, not one per element.

## 6. Undo/Redo

- Implemented purely via the `Command` stack defined in `03_DATA_MODEL.md`
  §4. No page-snapshot-based undo (too memory-heavy, breaks at scale).
- Undo stack is per-page, cleared when a page is closed (v1 — no
  cross-session undo history required).
- Every command must implement both `apply()` and `invert()`— write the
  inverse at the same time you write the command, not later.

## 7. Rendering / performance rules

- Cull elements outside the visible viewport before drawing (do not
  iterate every element on every frame for large pages).
- Cache stroke geometry as a `Path` object once committed; do not
  re-tessellate from raw points every frame.
- For pages with very high element counts, consider spatial indexing
  (quad-tree) — flag this as a Phase 3+ optimization, not v1-required
  unless profiling shows it's needed.

## 8. Explicit non-goals for this module

- No handwriting recognition here (belongs to a future module, not `drawing`).
- No PDF awareness here — `drawing` renders onto whatever `Page.background`
  says, and doesn't care if that background came from a PDF (that's `pdf`'s
  job to prepare, see `07_PDF_ENGINE.md`).

## 9. Test checklist (see `09_TESTING.md` for full detail)

- [ ] Slow deliberate stroke produces smooth curve, no jaggies.
- [ ] Fast flick stroke doesn't drop points / produce gaps.
- [ ] Pressure changes visibly affect stroke width.
- [ ] Palm touch while writing does not create a stray stroke.
- [ ] Undo after 50 rapid strokes correctly removes them one at a time.
- [ ] Eraser (both modes) behaves correctly on overlapping strokes.
- [ ] Zoom in/out keeps strokes crisp (no bitmap blur).
- [ ] App handles pointer-up mid-gesture (e.g. app backgrounded) without
      losing or corrupting the in-progress stroke.
