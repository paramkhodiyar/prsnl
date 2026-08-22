# prsnl — AI Development Rules

> Give this file to Claude/Cursor/whatever AI agent at the start of EVERY
> session working on this codebase. It's the contract that keeps AI-assisted
> development from turning into build → error → patch → rewrite → repeat.

## 1. Before touching any code

1. Read `02_ARCHITECTURE.md` for module boundaries.
2. Read the specific doc for the module you're about to touch
   (`03_DATA_MODEL.md`, `04_DRAWING_ENGINE.md`, `05_STYLUS.md`,
   `06_SHAPE_ENGINE.md`, `07_PDF_ENGINE.md`, `08_UI_SYSTEM.md`).
3. Confirm which Phase (`11_ROADMAP.md`) the project is currently in —
   do not implement Phase 6 features while Phase 2 is incomplete.

## 2. Hard rules

1. **Do not invent architecture.** If `02_ARCHITECTURE.md` doesn't cover a
   decision you need to make, stop and ask, don't improvise a new pattern.
2. **Do not add dependencies (libraries, modules) without flagging it
   first.** State the library, the reason, and the alternatives considered.
3. **Do not rewrite a working module to fix an unrelated bug.** Fix the
   actual root cause in the actual location.
4. **Do not change the data model (`03_DATA_MODEL.md`) without updating
   the doc in the same change** and adding a schema version bump +
   migration if the app has any persisted data using the old shape.
5. **Respect module dependency direction** (see `02_ARCHITECTURE.md` §3).
   If a change seems to require `drawing` to import from `pdf` (or similar
   violation), stop — that's a sign the feature belongs elsewhere or the
   architecture doc needs a deliberate update.
6. **Every new piece of non-trivial logic gets a unit test** (see
   `09_TESTING.md`). Not optional, not "I'll add it later."
7. **Never delete working functionality without explicit approval.**
   "Cleaning up" is not a reason to remove a feature.
8. **Preserve public interfaces** (function signatures, module APIs) unless
   an architecture change has been explicitly approved and documented.
9. **Build (and run tests) after every significant change.** Don't chain
   five unverified changes together.
10. **If uncertain, stop and explain the ambiguity** rather than guessing
    and hoping it's right.
11. **Before modifying more than 3 files in one change, explain why** —
    what triggered the wider blast radius, and confirm it's necessary.

## 3. Required workflow per task

```
1. INSPECT   — read relevant existing code before writing anything
2. PLAN      — state explicitly:
                 Files to modify: [...]
                 Files NOT to modify: [...]
                 New dependencies: [none / list + justification]
                 Risks: [...]
3. IMPLEMENT — write the code, following the plan
4. TEST      — run/build, run relevant test suite
5. REPORT    — summarize:
                 Implemented: [...]
                 Tests: [pass/fail, what was added]
                 Known limitations: [...]
```

Do not skip straight to step 3. The plan step is what catches
architecture violations and scope creep BEFORE they're written.

## 4. Scope discipline

- Implement only what the current phase (`11_ROADMAP.md`) and the relevant
  spec doc call for. Do not "while I'm in here" add out-of-scope features
  from `01_PRODUCT.md` §3's excluded list.
- If you (the AI) notice something that seems missing or worth adding,
  name it in the report as a suggestion — do not silently implement it.

## 5. When something breaks

- Fix the root cause. If the root cause is unclear, say so explicitly and
  propose a diagnostic step, rather than patching around the symptom.
- If fixing requires touching a module boundary or the data model, treat
  that as a "stop and confirm" moment, not a routine fix.

## 6. Human's responsibility (not the AI's)

- Approve any architecture/data-model changes before they're implemented.
- Periodically review that generated code actually matches these docs —
  docs drifting from reality silently is the failure mode this whole
  system exists to prevent.
