# prsnl — PDF Engine

> Scope: import, annotation-target preparation, and export. Lives in the
> `pdf` module. Depends on `document` + `core` ONLY — never on `drawing`
> internals (see `02_ARCHITECTURE.md` §3). It consumes the Document Model,
> it does not know how strokes are captured or recognized.

## 1. Two distinct features — do not conflate

### A. Export (build first — lower risk)
Turning a `Notebook`/`Page` (our own object-based document) into a PDF
file.

```
Page (elements: strokes, shapes, text, images)
      │
      ▼
PDF Page Writer
      │
      ├── Strokes  → vector paths (preserve quality, avoid rasterizing)
      ├── Shapes   → vector paths
      ├── Text     → real PDF text objects where feasible
      ├── Images   → embedded image objects
      └── Background (ruled/grid/dotted) → drawn as vector lines/dots
      │
      ▼
Assembled PDF (Android PdfDocument API or a suitable library)
```
- Prefer Android's `android.graphics.pdf.PdfDocument` for v1 — it draws
  directly via `Canvas`, which our renderer already knows how to target.
- Keep export vector-based wherever the underlying element supports it;
  only rasterize where unavoidable (e.g. certain image compositing edge
  cases).

### B. Import + Annotation (build second — higher complexity)
Taking an external PDF and letting the user write/draw on top of it.

```
External PDF file
      │
      ▼
PdfRenderer (Android) — render each PDF page to a bitmap background
      │
      ▼
Page.background = Background(type = PDF, pdfSourceRef = <path>)
      │
      ▼
User draws on top via normal drawing engine — these become normal
Stroke/Shape/TextBox/ImageElement entries in Page.elements, layered
above the PDF bitmap background
      │
      ▼
Export (re-run Export pipeline: PDF background page image + our vector
annotations layered on top, merged into output PDF)
```

- The imported PDF page is treated as a **background image**, not editable
  content — v1 does not support modifying the original PDF's own content,
  only annotating on top of it. This matches `Background.type == PDF` in
  `03_DATA_MODEL.md`.
- Each PDF page maps 1:1 to a `prsnl` `Page`. A multi-page PDF import
  creates multiple pages in a notebook.

## 2. Why this must stay isolated from `drawing`

- `drawing` renders onto "whatever the current page's background is" —
  it should not have PDF-specific branches. `pdf` is responsible for
  producing a `Background` object that `drawing`'s renderer already knows
  how to draw underneath the elements.
- This separation is what lets us swap or upgrade the PDF library later
  without touching ink code at all.

## 3. Performance considerations

- PDF page rendering (import) can be memory-heavy for large files — render
  at a reasonable resolution for the current zoom level, not full source
  resolution up front; consider tile/re-render-on-zoom for very large PDFs
  (flag as Phase 4 optimization if needed, not required for v1 MVP unless
  early testing shows it's necessary).
- Export of large notebooks (50+ pages) should run with progress reporting
  and be cancellable — never block the UI thread.

## 4. Explicit non-goals for v1

- Editing/extracting the original PDF's own text or vector content.
- OCR of scanned PDF text.
- Merging multiple existing PDFs together as a standalone utility (only
  in the context of exporting our own notebooks).

## 5. Test checklist

- [ ] Export a page with strokes + shapes + text + image → PDF opens
      correctly in a third-party viewer, all elements present and in
      correct position/z-order.
- [ ] Export at various zoom/page-size settings produces correctly scaled
      output (no clipping, no distortion).
- [ ] Import a multi-page PDF → each page becomes an editable, annotatable
      `prsnl` page.
- [ ] Annotate an imported PDF page, export it → merged output shows both
      original PDF content and annotations in correct positions.
- [ ] Large PDF (50+ pages) import doesn't OOM or freeze the UI.
- [ ] Corrupted/invalid PDF file import fails gracefully with a clear error,
      no crash.
