# prsnl — Ultra-Responsive Android Stylus Digital Notebook

**prsnl** is a high-performance, vector-based Android digital notebook application tailored for Android tablets with active stylus support. Built with Jetpack Compose, custom high-efficiency View Canvas rendering, and clean architecture, **prsnl** delivers native paper feel, rich digital stationery aesthetic, and advanced vector note-taking capabilities.

---

## 🚀 Key Features

### 🖋️ 1. Precision Digital Ink Engine
- **Pressure & Velocity Modulation:** Real-time ink stroke smoothing, pressure sensitivity, and speed-modulated stroke width for Pen, Pencil, and Highlighter tools.
- **0ms Instant Stylus Locking & Palm Rejection:** Microsecond 0 stylus lock rejects simultaneous finger touch events, eliminating palm rejection latency and preventing accidental scrolling while writing.
- **Straight-Line Hold Snapping:** Holding the pen tip still for 400ms at the end of a freehand stroke automatically snaps lines straight.

### ✂️ 2. Freehand Lasso Selection & Transformation
- **True Freehand Ray-Casting Lasso:** Draw any custom freehand loop (zig-zags, stars, organic curves around handwritten notes) to select enclosed elements.
- **Animated Marching-Ants Border:** Selected elements display a moving dashed border.
- **Top Axis Rotation Handle:** Rotate selected shapes/elements around their center axis via a dedicated top handle.
- **Corner Scaling & Drag-to-Move:** Interactive handles to resize, scale, recolor, and reposition selected objects anywhere on the canvas.

### 📐 3. Vector Shapes & Graph Tools
- **14 Built-in Vector Shapes:** Straight Line, Single Arrow, Double Arrow, Corner, Single/Double Corner Arrows, Rectangle, Rounded Rectangle, Ellipse, Parallelogram, Triangle, Diamond, 2D Graph Axes, 4-Quadrant Coordinate Plane, and 3D Coordinate Axis.
- **Real-Time Drag & Stretch:** Drag-to-size bounding box geometry during creation.

### ⌨️ 4. Typer / Text Tool
- **Inline Text Insertion:** Tap anywhere on the page workspace with the Typer tool to insert editable formatted text boxes.

### 🖼️ 5. Image & Photo Attachments
- **Gallery Import:** Pick images from device storage and place them directly onto the canvas.
- **Interactive Transform:** Select, scale, rotate via top handle, and position image attachments freely across the workspace.

### 📄 6. Stationery Templates & Universal Page Numbers
- **Paper Templates:** Margin Ruled (with red margin line, top header margin, and dedicated `Date: ____ / ____ / 20__` section), Ruled, Grid, Isometric (3D Grid), Dotted, Cornell Notes, 2-Column Layout, Music Staves, and Blank.
- **Universal Page Numbers:** Every page automatically renders `Page {N}` in the bottom-right footer.
- **Stationery Paper Colors:** Warm Ivory Cream, Legal Pad Yellow, Soft Mint, Pastel Blush Pink, Lavender, Slate Gray, OLED Dark Mode, and Pure White.
- **Inherited Formatting:** New pages automatically inherit paper color, line spacing, and template format from preceding pages.

### 📁 7. Persistent Room Database Folder Management
- **SQLite Room DB Storage:** Persistent storage for Folders and Notebooks. Empty folders remain saved permanently and never disappear when navigating away.
- **Long-Press Context Menus:** Long-press folder or notebook cards to edit titles, change accent colors, move notebooks between folders, or delete.

### 🎨 8. Warm Stationery Light Theme
- **Moleskine Ivory Aesthetic:** Soft cream surfaces (`#F5F0E6`), warm ivory canvas (`#FBF9F4`), amber gold accents (`#C88A4B`), and charcoal ink typography (`#2D2B28`).

---

## 🛠️ Technology Stack

- **UI Framework:** Android Jetpack Compose + Custom Canvas View rendering
- **Architecture:** Clean Architecture (Modular: `app`, `core`, `document`, `drawing`, `storage`, `ui`, `pdf`)
- **Local Database:** Room SQLite DB v2
- **DI Framework:** Dagger Hilt
- **Build System:** Gradle Kotlin DSL

---

## 📱 Module Architecture

```
notes/
├── app/          # Application entry point, Hilt setup, Activity
├── core/         # Common utilities, logger, crash reporting
├── document/     # Document domain model (Notebook, Page, Element, RectData)
├── drawing/      # Drawing engine, rendering, lasso selection, shape recognition
├── pdf/          # PDF export & import annotation engine
├── storage/      # Room database, DAOs, entities, file persistence
└── ui/           # Jetpack Compose screens, navigation, floating toolbars
```
