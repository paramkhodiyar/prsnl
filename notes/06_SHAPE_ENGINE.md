# prsnl — Shape Engine

> Scope: converting rough hand-drawn strokes into clean geometric `Shape`
> elements. Lives inside `drawing` but is a clearly separated sub-component
> — do not mix recognition logic into the renderer or input layer.

## 1. Trigger model: hold-to-recognize

```
User draws stroke
      │
      ▼
Pointer goes still (velocity below threshold) while still down
      │
      ▼
Hold timer starts (~300–500ms, tune via testing)
      │
      ▼
Timer completes without further movement
      │
      ▼
Trigger recognition on the just-completed stroke
```

- If the pointer moves again before the timer completes, cancel the timer
  — the user is still drawing, not signaling "recognize this."
- If recognition fails (stroke doesn't match any known shape within
  tolerance), leave the raw stroke as-is — do NOT force a bad match.
- Manual shape insertion (toolbar shape picker) bypasses this entirely and
  is the reliable fallback if recognition ever feels unreliable.

## 2. Recognition pipeline

```
Raw Stroke (List<StrokePoint>)
      │
      ▼
1. Closed-shape check: distance(firstPoint, lastPoint) < closeThreshold
      │
      ▼
2. Candidate fitting: compare stroke against each candidate shape type
   using a simple geometric scoring method (v1 approach, not ML):
     - Line: check if points roughly fall on a single straight line
       (linear regression residual under threshold)
     - Rectangle: check corner count via angle-change detection along
       the stroke, verify 4 roughly-90° turns
     - Ellipse/Circle: fit to bounding box, check average deviation of
       points from the ellipse defined by that bounding box
     - Arrow: open stroke (not closed) with a small closed/branching
       cluster of points near one end (arrowhead)
     - Triangle: 3 detected corners
      │
      ▼
3. Pick best-scoring candidate below its rejection threshold; if none
   qualify, no shape is created (raw stroke remains)
      │
      ▼
4. Construct Shape element: bounding box, rotation, color/width inherited
   from the pen's current settings
      │
      ▼
5. Issue ReplaceElement command (raw Stroke → Shape), so undo reverts
   cleanly back to the original hand-drawn stroke
```

## 3. v1 algorithm approach: geometric heuristics, not ML

- Do not reach for a machine-learning classifier for v1. Simple geometric
  tests (corner detection via angle deltas, linear regression for
  straightness, bounding-box-relative deviation for circles/ellipses) are
  sufficient, fast (<50ms), explainable, and easy to unit test.
- If recognition quality proves insufficient after real testing, revisit
  with a small on-device classifier — this is a Phase 5+ decision, not a
  v1 assumption.

## 4. Tunable parameters (centralize these, don't hardcode scattered)

```
ShapeRecognitionConfig
 ├── holdDurationMs: Long           (default 400)
 ├── velocityStillThreshold: Float  (px/ms below which "still" counts)
 ├── closeShapeThreshold: Float     (max px gap to count as "closed")
 ├── lineResidualThreshold: Float
 ├── cornerAngleThreshold: Float    (degrees, for corner detection)
 └── ellipseDeviationThreshold: Float
```
Keep these in one config object so tuning doesn't require touching
recognition logic itself.

## 5. Editing recognized shapes

- Once converted, a `Shape` behaves like any other element: selectable,
  resizable (drag corner handles), rotatable, recolorable, deletable.
- `Shape.sourceStrokeId` (see `03_DATA_MODEL.md`) is kept only for
  debugging/undo traceability — never rendered, never surfaced in UI.

## 6. Graphs (v1 scope: freehand only)

- v1 graph support = drawing axes and data by hand like any other stroke;
  no special "graph object" type.
- Optional stretch within v1: recognize a hand-drawn "+" or "L" shape as
  axes and offer to snap them straight + add gridlines — this is a nice-to
  -have, not required for v1 ship. If built, it's a thin layer on top of
  the same recognition pipeline (recognize two perpendicular lines), not a
  new engine.
- Parametric/smart graphs (plotting functions) are explicitly Phase 5+,
  see `01_PRODUCT.md` §3.

## 7. Test checklist

- [ ] Rough circle (various sizes, various drawing speeds) recognized
      correctly ≥90% of casual attempts.
- [ ] Rough rectangle recognized correctly, including near-square cases.
- [ ] Straight-ish line not misclassified as a very flat ellipse.
- [ ] Arrow direction (which end has the head) preserved correctly.
- [ ] A deliberately messy scribble does NOT get force-matched to a shape.
- [ ] Undo immediately after recognition restores the exact original
      raw stroke (points, pressure, color).
- [ ] Recognition timer correctly cancels if user resumes drawing.
