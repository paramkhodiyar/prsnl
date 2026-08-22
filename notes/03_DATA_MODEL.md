# prsnl — Data Model

> This is the single most important document in the project. The whole
> "no debugging hell" plan depends on this schema being right before code
> exists. Any change here must be deliberate and versioned (see §6).

## 1. Core principle

A page is a **list of typed elements**, never a bitmap. Every element is
independently selectable, movable, resizable, deletable, and re-renderable
at any zoom level.

## 2. Hierarchy

```
Notebook
 ├── id: String (UUID)
 ├── title: String
 ├── createdAt: Long (epoch ms)
 ├── updatedAt: Long
 ├── coverColor / coverStyle
 └── pages: List<PageRef>   // ordered list of page IDs

Page
 ├── id: String (UUID)
 ├── notebookId: String
 ├── index: Int              // order within notebook
 ├── width: Float             // in "page units", not pixels
 ├── height: Float
 ├── background: Background
 └── elements: List<Element>  // z-ordered, index = paint order

Background
 ├── type: enum { BLANK, RULED, GRID, DOTTED, PDF }
 ├── lineSpacing: Float?      // for RULED/GRID/DOTTED
 ├── colorLight: Color
 ├── colorDark: Color
 └── pdfSourceRef: String?    // set only if type == PDF (see pdf module doc)

Element (sealed class — every element type below is a variant)
 ├── id: String (UUID)
 ├── zIndex: Int
 ├── boundingBox: Rect        // always kept up to date, used for selection/culling
 ├── createdAt: Long
 └── (one of the variants below)
```

## 3. Element variants

### Stroke
```
Stroke : Element
 ├── points: List<StrokePoint>
 ├── color: Color
 ├── baseWidth: Float
 ├── tool: enum { PEN, HIGHLIGHTER }
 └── brushStyle: enum { ROUND, FLAT, MARKER }   // v1: keep minimal, expand later

StrokePoint
 ├── x: Float
 ├── y: Float
 ├── pressure: Float     // 0.0–1.0, normalized from raw MotionEvent
 ├── tiltX: Float?        // optional, only if device reports it
 ├── tiltY: Float?
 └── timestampMs: Long    // relative to stroke start, used for velocity-based smoothing
```

### Shape
```
Shape : Element
 ├── type: enum { RECTANGLE, ELLIPSE, LINE, ARROW, TRIANGLE }
 ├── boundingBox: Rect     // duplicated at Element level for convenience
 ├── rotation: Float        // radians
 ├── strokeColor: Color
 ├── strokeWidth: Float
 ├── fillColor: Color?      // null = no fill
 └── sourceStrokeId: String? // if created via hold-to-recognize, keep the
                              // original raw stroke ID for undo/debug, but
                              // it is NOT rendered once converted
```

### TextBox
```
TextBox : Element
 ├── content: String
 ├── fontSize: Float
 ├── color: Color
 ├── boundingBox: Rect
 └── alignment: enum { START, CENTER, END }
```

### ImageElement
```
ImageElement : Element
 ├── assetPath: String     // relative path in app-private storage
 ├── boundingBox: Rect
 └── rotation: Float
```

### PdfAnnotationRef
```
PdfAnnotationRef : Element
 └── // Used only on PDF-background pages. Wraps a Stroke/Shape/TextBox/
     // ImageElement that sits ON TOP of an imported PDF page. Structurally
     // identical to the above — this is a marker, not a new schema. See
     // 07_PDF_ENGINE.md for how import/export handles this layer.
```

## 4. Undo/redo model (Command pattern — see 04_DRAWING_ENGINE.md for detail)

```
Command (sealed)
 ├── AddElement(element)
 ├── DeleteElement(elementId)
 ├── MoveElement(elementId, from: Rect, to: Rect)
 ├── ResizeElement(elementId, from: Rect, to: Rect)
 ├── ChangeStyle(elementId, before, after)   // color/width/fill changes
 └── ReplaceElement(oldElementId, newElement) // used by shape recognition:
                                               // raw Stroke -> recognized Shape
```

Every mutation to a `Page` MUST go through a `Command`. Direct mutation of
`elements` from UI code is forbidden — this is what makes undo/redo,
autosave, and future collaboration features possible without a rewrite.

## 5. Persistence mapping (implementation note, not schema)

- `Notebook` and `Page` metadata → Room tables.
- `elements` for a given page → serialized using `kotlinx.serialization` (JSON format)
  to a single `.json` file per page under app-private storage (e.g. `pages/{page_id}.json`),
  referenced by path from the Room `Page` row.
- Rationale for JSON choice: human-readable, schema versioning friendly (`schemaVersion`),
  inspectable for debugging, and fully supports Kotlin sealed classes via `@Serializable`.
- Images/PDF sources → raw files in app-private storage, referenced by path.
- Rationale: keeps SQLite rows small and fast to query for notebook lists,
  while page content (which can be large) is loaded lazily per-page.

## 6. Schema versioning

- Every serialized page file includes a `schemaVersion: Int` field.
- Any change to an Element variant's fields requires a bump + a migration
  function. No silent format drift.
- Do not delete old fields from serialized data structures without a
  migration path — this is the #1 source of "old notes are corrupted" bugs.

## 7. What this schema deliberately does NOT include (v1)

- No per-character handwriting recognition metadata.
- No collaborative editing fields (no CRDT/OT structures).
- No cloud IDs / sync tokens.
These can be added later without breaking the above, but must not be
scaffolded early — see `01_PRODUCT.md` §3.
