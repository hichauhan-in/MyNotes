<div align="center">

# MyNotes+

### Private. Encrypted. Yours.

An offline‑first, privacy‑first, **encrypted** note‑taking app for Android. Every note is
encrypted at rest with AES‑256‑GCM using a key held in the Android Keystore — plaintext
never touches storage, and no account or login is ever required.

</div>

---

## ✨ Features

- **End‑to‑end on‑device encryption** — notes **and their image/voice attachments** are encrypted with **AES‑256‑GCM**; the key lives in the **Android Keystore** and is decrypted only in memory.
- **Offline‑first** — everything works with no internet connection. No sign‑in required.
- **Premium, neumorphic UI** — a calm indigo→violet design system built on **Material 3**, with light / dark / system themes and optional **dynamic color**.
- **Fast home dashboard** — staggered note grid with search and filter chips (All, Recent, Favorites, Pinned, Archived, Trash) plus a morphing “create” FAB.
- **Multi‑select** — long‑press to select multiple notes and pin / favorite / archive / trash them in bulk. A per‑note `⋮` menu handles single‑note actions.
- **Distraction‑free editor** — debounced auto‑save, a Markdown formatting toolbar with **live styling** (bold / italic / headings render as you type), note color labels, and live word‑count / reading‑time stats.
- **Rich blocks** — interactive **checklists**, **tables** (labelled A/B/C columns and 1/2/3 rows, add / remove, drag to resize), **callouts** (a highlighted tip / warning box), inline **scribbles** (a resizable freehand sketch pad), a heading button with **selectable sizes** (long‑press for H1 / H2 / H3), a list button and a quote button that remember your last choice, all interleaved inline with text and media.
- **Whiteboard notes** — a pan‑and‑zoom infinite canvas where freehand scribbling is the default; choose pen thickness (long‑press the pen) and colour, drop draggable text notes that zoom with the board, and move around smoothly.
- **Sheets** — a full-note spreadsheet with editable cells and **drag-resizable rows and columns** (grab the dotted handle between column letters).
- **Expenses** — a private, fully customisable budgeting dashboard: log income and build your own sections (rename, add, or remove Expenses / Savings / Investments and any custom section you like, such as Accounts) with a live allocation bar that updates what's still unallocated.
- **Checklists** — turn a note into an interactive, checkable to‑do list with live progress.
- **Image attachments** — insert photos from the **gallery** or capture live with the **camera**, placed **inline at your cursor** so text and images can be interleaved freely. Drag a corner to **resize** (smoothly) or **crop** an image. Each note keeps its own private, **encrypted‑at‑rest** on‑device copy (removed with the note); captures never leave the app unless you export them.
- **Voice notes** — record audio straight into a note and play it back inline. Recordings are stored **encrypted at rest** on‑device and deleted with the note.
- **Books** — organise notes into nestable folders ("books"). Create a book from the + menu, open it to browse or add notes inside, and move single or multiple notes between books. **Long‑press a book to select several at once** and move or delete them together (its **⋮ menu** still gives per‑book actions). Deleting a book (after a confirmation) moves it and **everything nested inside it to Trash together, keeping its structure**, so you can restore the whole book or delete it for good.
- **Tags** — label notes and search any tag to pull up every note that carries it. Tagging, pin, favorite and colour sit together in one row right under the title on **every** note type (text, checklist, whiteboard, sheet and expenses).
- **Reusable templates** — kept on a dedicated **templates button in the bottom-left corner**; “Manage templates” lists them, and **New template** opens a full note editor where the title becomes the template name, you pick an icon, and a **Save as template** button stores your draft. Deleted templates go to **Trash** and can be recovered within the retention window.
- **App lock** — optional unlock with **fingerprint, face, or device PIN** (biometric / device‑credential).
- **Read‑only by default** — reopening a note shows it read‑only so you can't change it by accident; tap the **pencil** in the top bar to edit. Every note also has a **⋮ menu** (Share, Export, Move to Trash).
- **Organization** — pin, favorite, archive, color labels, and a recoverable Trash.
- **Export** — save any note as **plain text, Markdown, HTML or PDF** (notes with images or voice notes come out as a **ZIP** with their attachments), or export a whole **book as a ZIP** that keeps its folder structure and attachments. Exports go straight to your **Downloads/MyNotes** folder by default (no permission needed on Android 10+); pick a different **default export folder** in Settings, or on older devices you'll get the system "Save to…" picker.
- **Optional encrypted cloud backup** — off by default; when enabled, only encrypted blobs are uploaded (keys never leave your device).

> Screenshots and the full feature roadmap live in the app itself and in [`plan.txt`](plan.txt).

---

## 🔐 Security & privacy at a glance

| Area | Approach |
| --- | --- |
| Encryption | AES‑256‑GCM, random IV per operation |
| Key storage | Android Keystore (hardware‑backed where available) |
| At rest | Only ciphertext is written to the Room database |
| Accounts | None required; no telemetry, ads, or trackers |
| App lock | Biometric / device credential, re‑locks on background |
| Cloud | Optional, encrypted‑only, disabled by default |

Full policy: **[Privacy Policy](docs/index.html)** (also published via GitHub Pages — see below).

---

## 🧱 Tech stack & architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (Compose BOM)
- **Architecture:** MVVM + Repository pattern, unidirectional state with `StateFlow`
- **Persistence:** Room (encrypted payloads) + Jetpack DataStore (non‑sensitive settings)
- **Async:** Coroutines & Flow
- **Security:** Android Keystore, `androidx.biometric`
- **Navigation:** Navigation‑Compose with shared‑axis transitions
- **DI:** lightweight manual container (`di/AppContainer`)

### Project structure

```
app/src/main/java/com/example/
├─ MainActivity.kt            # Entry point, theme + app‑lock gate
├─ VaultNotesApp.kt           # Application, initialises AppContainer
├─ data/
│  ├─ local/                  # Room database, entities, DAOs
│  ├─ repository/             # NoteRepository (encrypt/decrypt boundary)
│  ├─ security/               # EncryptionManager (AES‑256‑GCM + Keystore)
│  └─ settings/               # SettingsRepository (DataStore)
├─ di/                        # AppContainer
├─ domain/model/             # Domain models
└─ ui/
   ├─ components/             # Reusable neumorphic components
   ├─ editor/                 # Note editor screen + view model
   ├─ home/                   # Home screen + view model
   ├─ lock/                   # Biometric app‑lock gate
   ├─ navigation/             # NavHost
   ├─ settings/               # Settings screen + view model
   └─ theme/                  # Colors, type, shapes, neumorphism
```

---

## 🚀 Build & run

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (latest stable), JDK 11+, Android SDK 36.

1. Open Android Studio → **Open** → select this project directory.
2. Let Gradle sync and resolve dependencies.
3. Run on an emulator or device (min SDK 24 / Android 7.0).

> **Debug signing:** the project defines a custom debug signing config pointing at `debug.keystore` (which is git‑ignored). If a fresh clone doesn’t have it, either let Android Studio use the default debug keystore (remove the `signingConfig = signingConfigs.getByName("debugConfig")` line from the `debug { }` block in `app/build.gradle.kts`) or drop in your own `debug.keystore`.

### Release signing

Release builds are signed via environment variables (no secrets in the repo):

| Variable | Purpose |
| --- | --- |
| `KEYSTORE_PATH` | Path to your upload keystore (defaults to `my-upload-key.jks`) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password (alias: `upload`) |

Keystores (`*.jks`, `*.keystore`), `.env`, and `google-services.json` are all git‑ignored — **never commit signing keys or secrets.**

---

## 🌐 Privacy policy hosting (GitHub Pages)

A ready‑to‑publish privacy policy lives at [`docs/index.html`](docs/index.html). To publish it for your Play Store listing:

1. Push this repo to GitHub.
2. Go to **Settings → Pages**.
3. Set **Source** to `Deploy from a branch`, branch `main`, folder **`/docs`**.
4. Your policy will be live at `https://<your-username>.github.io/<repo-name>/`.
5. Paste that URL into the Play Console **Privacy Policy** field.

> Remember to replace the placeholder contact email in `docs/index.html`.

---

## 🗺️ Roadmap

Planned / in progress (see [`plan.txt`](plan.txt) for the full vision): rich‑text formatting, drawing canvas, tags & folders, reminders, full Google Drive encrypted sync, import/export, and home‑screen widgets.

---

## 📄 License

Copyright © 2026. All rights reserved. (Add your preferred license here before open‑sourcing.)

