# prsnl — Testing Strategy

> The whole point of this doc: when an AI agent changes code, you need a
> fast, mechanical way to know if it broke something — without manually
> retesting the entire app by hand every time. Tests are the guardrail.

## 1. Test pyramid for this project

```
        ┌─────────────────────┐
        │   Manual device      │   small number, high-value
        │   testing (stylus)   │   (latency/feel can't be unit tested)
        ├─────────────────────┤
        │  UI / instrumented   │   medium number
        │  tests (Compose)     │
        ├─────────────────────┤
        │   Unit tests          │   large number, run on every change
        │  (document, shape,    │
        │   pdf export logic)   │
        └─────────────────────┘
```

Prioritize the bottom of the pyramid — it's fast, deterministic, and
exactly where AI-driven regressions show up (silently changed logic in a
pure function is easy to catch with a unit test, easy to miss by eye).

## 2. What MUST have unit tests (non-negotiable, per module)

### `document`
- Serialization round-trip: create a `Page` with every element type →
  save → load → deep-equal to original.
- Schema version migration (once >1 version exists).
- `Command.apply()` / `Command.invert()` correctness for every command type.

### `drawing` (non-UI logic only — pure functions)
- Stroke smoothing function: given known input points, output matches
  expected smoothed output within tolerance.
- Eraser split logic: given a stroke and an erase region, correct
  remaining stroke segments are produced.
- Selection hit-testing: given element bounding boxes and a tap point,
  correct element is selected.

### `drawing` → shape engine (see `06_SHAPE_ENGINE.md` §7 for the full list)
- Recognizer correctly classifies known-good synthetic stroke data for
  each shape type (generate test strokes programmatically, don't rely
  only on hand-drawn samples).
- Recognizer correctly REJECTS a messy/ambiguous stroke (no false positive).

### `pdf`
- Export produces a valid PDF (parseable, correct page count) for a
  synthetic document fixture.
- Import correctly maps N PDF pages → N `prsnl` pages.

## 3. Instrumented / UI tests (fewer, but cover critical flows)

- Create notebook → create page → draw a stroke → close app → reopen →
  stroke still present (persistence smoke test).
- Undo/redo through a mixed sequence of add/move/delete actions produces
  the expected element list at each step.
- Tool switching mid-gesture does not crash or corrupt state.

## 4. Manual device testing (can't be automated, must be checklist-driven)

Maintain a `MANUAL_TEST_CHECKLIST.md` (create when Phase 2 starts) covering:
- Latency/feel on target hardware (Xiaomi Pad + Focus Pen Pro).
- Palm rejection in realistic writing posture.
- Battery/thermal behavior during extended writing sessions.
- Real-world PDF import from various sources (scanned textbook, exported
  doc, etc.) — synthetic PDFs in unit tests won't catch every edge case.

## 5. Rule for AI agents

Before marking any feature "done":
1. Unit tests exist for new pure logic.
2. Existing test suite passes (run it, don't assume).
3. If a change required modifying an existing test's expected output,
   explain WHY in the implementation report (see `10_AI_RULES.md` §5) —
   a changed test expectation is a signal worth double-checking, not a
   rubber stamp.

## 6. CI (set up once Phase 0 project scaffolding exists)

- Run full unit test suite on every commit/PR.
- Fail the build on any test failure — no merging with red tests.
- Instrumented tests can run less frequently (e.g. pre-release) if device
  farm access is limited, but must run before any release build.
