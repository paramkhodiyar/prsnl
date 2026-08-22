# prsnl — Roadmap

> Each phase must end with a working, buildable app — not a broken
> in-progress state. Do not start phase N+1 until phase N's checklist is
> genuinely done (tests passing, manually verified on device where
> relevant).

## Phase 0 — Freeze the foundation (no app code yet)
- [ ] Finalize `01_PRODUCT.md` through `09_TESTING.md` — read them all,
      adjust anything that doesn't match your actual intent before coding.
- [ ] Decide: Compose Canvas vs. custom View/SurfaceView for the drawing
      surface (small throwaway spike, timebox it, pick one).
- [ ] Decide: serialization format for page element data (protobuf vs
      JSON vs other) — timebox a decision, document it in
      `03_DATA_MODEL.md` §5.
- [ ] Set up Android project skeleton matching the module map in
      `02_ARCHITECTURE.md` §2 — empty modules with correct dependency
      wiring, no features yet.
- [ ] Set up CI running an empty test suite (proves the pipeline works).

**Exit criteria:** project builds, module dependency graph matches the doc,
CI is green.

## Phase 1 — Document model + persistence (no drawing yet)
- [ ] Implement `Notebook`/`Page`/`Element` variants per `03_DATA_MODEL.md`.
- [ ] Implement Room entities + file storage repository.
- [ ] Unit tests: full serialization round-trip for every element type.
- [ ] Basic Home screen: create/list/delete notebooks (ugly UI is fine).

**Exit criteria:** can create a notebook with hand-constructed test pages
(no UI drawing yet), close and reopen the app, data persists correctly.

## Phase 2 — Raw stylus input on a bare canvas
- [ ] Bare test screen: full-screen drawing surface, nothing else.
- [ ] Implement input pipeline per `04_DRAWING_ENGINE.md` §1–3 and
      `05_STYLUS.md`.
- [ ] Manual test checklist from `04_DRAWING_ENGINE.md` §9 — this is the
      most important checkpoint in the whole project. Do not proceed until
      writing genuinely feels good on target hardware.
- [ ] Palm rejection verified on real device.

**Exit criteria:** you can write on the bare canvas and it feels close to
native. If it doesn't, fix this before building anything else — every
later phase depends on this foundation.

## Phase 3 — Persistence-backed drawing + undo/redo + eraser
- [ ] Wire committed strokes into the Phase 1 document model via Commands.
- [ ] Implement undo/redo per `04_DRAWING_ENGINE.md` §6.
- [ ] Implement both eraser modes.
- [ ] Autosave (debounced background persistence).
- [ ] Test checklist from `04_DRAWING_ENGINE.md` §9 (full list).

**Exit criteria:** draw on a real page, close app, reopen, everything is
exactly as left; undo/redo works reliably across a long session.

## Phase 4 — Shapes + hold-to-recognize + selection/transform
- [ ] Manual shape insertion (toolbar).
- [ ] Hold-to-recognize per `06_SHAPE_ENGINE.md`.
- [ ] Selection tool: move/resize/rotate/recolor/delete.
- [ ] Test checklist from `06_SHAPE_ENGINE.md` §7.

**Exit criteria:** rough hand-drawn shapes reliably convert; editing a
converted shape feels natural; undo restores the original raw stroke.

## Phase 5 — Paper types, templates, theming, real UI
- [ ] Ruled/grid/dotted backgrounds, light/dark mode per `08_UI_SYSTEM.md`.
- [ ] Real toolbar, Notebook Detail screen with page thumbnails.
- [ ] Text boxes, image insertion.
- [ ] Test checklist from `08_UI_SYSTEM.md` §7.

**Exit criteria:** app looks and feels like a real product, not a test
harness — but still built on the same solid engine from Phases 2–4.

## Phase 6 — PDF import, annotation, export
- [ ] Export pipeline per `07_PDF_ENGINE.md` §1.A (build first).
- [ ] Import + annotation pipeline per §1.B.
- [ ] Test checklist from `07_PDF_ENGINE.md` §5.

**Exit criteria:** export a real notebook to PDF and open it in a
third-party viewer correctly; import a real-world PDF and annotate it.

## Phase 7 — Xiaomi-specific spike (isolated, optional)
- [ ] Investigate whether Tier 2 Focus Pen Pro gestures (per
      `05_STYLUS.md` §1) are accessible via any public API.
- [ ] If yes: build as an isolated, optional integration that degrades
      gracefully. If no: document the finding and stop — don't force it.

**Exit criteria:** a documented yes/no answer, and if yes, a working
opt-in integration that never breaks Tier 1 behavior.

## Phase 8 — Polish + performance pass
- [ ] Profile drawing performance on large pages (spatial indexing if
      needed, per `04_DRAWING_ENGINE.md` §7).
- [ ] Profile PDF export/import performance on large documents.
- [ ] Cold-start time, thumbnail generation performance.
- [ ] Full manual device testing checklist (`09_TESTING.md` §4).

## Explicitly deferred beyond this roadmap (see `01_PRODUCT.md` §3)
Handwriting-to-text, search, cloud sync, collaboration, math/LaTeX,
parametric graphs, monetization. Do not pull these forward without
updating `01_PRODUCT.md` first.
