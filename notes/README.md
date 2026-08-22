# prsnl — Docs

This is the source-of-truth doc set for **prsnl**, an Android stylus
notebook app (target: Xiaomi Pad + Focus Pen Pro). Read `docs/` in order —
each file builds on the last.

## Reading order

| # | File | Purpose |
|---|---|---|
| 1 | `01_PRODUCT.md` | What v1 does and explicitly does NOT do |
| 2 | `02_ARCHITECTURE.md` | Tech stack, module map, dependency rules |
| 3 | `03_DATA_MODEL.md` | The object-based document schema — the most important file |
| 4 | `04_DRAWING_ENGINE.md` | Stylus-to-pixel pipeline, undo/redo, eraser |
| 5 | `05_STYLUS.md` | MotionEvent handling, palm rejection, Focus Pen Pro tiers |
| 6 | `06_SHAPE_ENGINE.md` | Hold-to-recognize algorithm, shape editing |
| 7 | `07_PDF_ENGINE.md` | Import, annotation, export — kept isolated from drawing |
| 8 | `08_UI_SYSTEM.md` | Screens, theming, toolbar structure |
| 9 | `09_TESTING.md` | What must be tested, and why it protects you from AI regressions |
| 10 | `10_AI_RULES.md` | The operating contract — give this to every AI coding session |
| 11 | `11_ROADMAP.md` | Phased build order, each phase ends in a working app |

## How to actually use this with an AI coding agent

1. Every new session, point the agent at `10_AI_RULES.md` first, plus
   whichever module doc is relevant to the task.
2. Never let the agent skip the INSPECT → PLAN → IMPLEMENT → TEST → REPORT
   workflow described there.
3. Work phase-by-phase per `11_ROADMAP.md`. Don't let scope jump ahead —
   Phase 2 (raw stylus feel) is the single highest-risk phase; get it
   right before building anything on top of it.
4. If an agent's plan requires changing something in these docs
   (architecture, data model), that's a deliberate, human-approved edit to
   the doc itself — not a silent code change.

## Why this exists

The goal is to front-load every architectural decision so an AI agent
never has to guess, never has reason to "helpfully" restructure things
mid-feature, and every change is checkable against a written contract.
That's what turns "AI-assisted development" from a debugging loop into
actual leverage.
