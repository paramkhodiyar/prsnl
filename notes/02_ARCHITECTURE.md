# prsnl — Architecture

> This architecture is FROZEN once Phase 0 is complete (see `11_ROADMAP.md`).
> Any AI agent working on this codebase must read this file first. Changing
> module boundaries or the tech stack requires updating this doc explicitly —
> never do it silently mid-feature.

## 1. Tech stack (decided, do not relitigate per-feature)

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Native Android, best stylus API access |
| UI | Jetpack Compose | Modern, but drawing surface is NOT Compose-native (see below) |
| Drawing surface | Custom `View` + `Canvas` (or Compose `Canvas` with raw pointer input, TBD in Phase 2 spike) | Stylus latency requires low-level control; Compose gesture APIs alone are not enough |
| Local DB | Room (SQLite) | Metadata: notebooks, pages, tags |
| Large assets | Flat files on disk (app-private storage) | Stroke data, images, PDFs — not blobbed into SQLite |
| PDF render | PdfRenderer (Android) for import view; custom vector PDF writer for export | Avoid rasterizing our own vector strokes on export |
| DI | Hilt (or manual DI if team prefers less magic) | Keep modules decoupled |
| Async | Kotlin Coroutines + Flow | Standard Android practice |
| Min SDK | Target Xiaomi Pad Android version; verify before Phase 0 closes | Stylus APIs vary by API level |

**Explicitly rejected:** Flutter, React Native, any cross-platform framework.
Stylus input (pressure, tilt, hover, tool-type discrimination, palm
rejection) is an Android-native input problem — see `05_STYLUS.md`.

## 2. Top-level module map

```
prsnl/
├── app/            — Application entry, DI wiring, navigation
├── core/           — Shared utilities, base classes, no Android UI deps
├── document/        — Document model + persistence (Notebook/Page/Element)
├── drawing/         — Ink engine: stroke capture, rendering, shape engine
├── pdf/             — PDF import/export/annotation
├── ui/              — Compose screens, ViewModels, theming
└── storage/         — Room DB + file storage implementation (backs document/)
```

## 3. Dependency direction (STRICT — enforce in build.gradle module deps)

```
        ui
         │
    ┌────┴────┐
    │         │
 drawing    document ──── storage
    │         │
    └────┬────┘
         │
        core
```

- `drawing` depends on `document` (to read/write elements) and `core`.
- `drawing` NEVER depends on `pdf` or `ui`.
- `pdf` depends on `document` and `core`. `pdf` NEVER depends on `drawing`
  internals — it consumes the document model only.
- `document` depends on `storage` and `core` only.
- `ui` is the only module allowed to depend on everything else.
- `core` depends on nothing in this project.

**Rule for AI agents:** if a change requires adding a new cross-module
dependency (e.g. `drawing` importing from `pdf`), STOP and flag it. That is
an architecture violation, not a normal code change.

## 4. Runtime component diagram

```
Stylus/Finger
     │
     ▼
Input Layer (MotionEvent capture, filtering)
     │
     ▼
Stroke Engine (raw points → smoothed stroke)
     │
     ├──► Shape Recognizer (on hold-still trigger)
     │
     ▼
Document Model (in-memory page state)
     │
     ├──► Renderer (Canvas draw of current page)
     │
     └──► Repository ──► Room (metadata) + Files (stroke/asset data)
                              │
                              ▼
                         PDF Engine (reads Document Model, never the reverse)
```

## 5. Threading model

- Input capture and rendering: main thread (required for Canvas/View).
- Stroke smoothing/prediction: main thread, must be sub-frame-budget fast
  (target <4ms per event batch) — do not push to background thread, that
  adds latency, not removes it.
- Persistence writes (autosave): background thread via coroutine, debounced
  (e.g. 500ms after last stroke), never blocks drawing.
- Shape recognition: main thread (runs only once per hold-trigger, must be
  fast — target <50ms).
- PDF export/import: background thread, with progress reporting to UI.

## 6. Module responsibility summary

- **document**: owns the truth of "what is on this page." Pure data +
  serialization. No rendering code, no Android View code.
- **drawing**: owns "how do I turn stylus input into document elements and
  paint them." Contains the stroke engine, shape engine, selection/transform
  logic, and the Canvas renderer.
- **pdf**: owns import (PDF → viewable pages + annotation targets) and
  export (Document Model → PDF bytes). Never owns stroke logic.
- **storage**: owns Room entities/DAOs and file I/O. Exposes a repository
  interface that `document` consumes — `document` should not know it's
  SQLite under the hood (keeps future migration options open, e.g. sync
  later).
- **ui**: owns Compose screens, navigation, ViewModels, theming
  (light/dark), toolbars. Talks to `document`/`drawing`/`pdf` through clean
  interfaces, never reaches into their internals.

## 7. What "frozen" means in practice

Once Phase 0 is done:
- Module boundaries above do not change without a doc update + explicit
  human approval.
- New modules can be added (e.g. a future `sync/` module) but must be
  proposed in this doc first, not created ad hoc by an AI agent mid-task.
