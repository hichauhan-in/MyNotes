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
- **Tablet & large‑screen ready** — the phone layout is untouched, while on tablets (and split‑screen) the note grid flows into **more columns** and long‑form screens (the editor, expenses, settings and reminders) **centre their content at a comfortable reading width** instead of stretching edge‑to‑edge. It reacts live to window size, so rotating or resizing stays fluid.
- **Fast home dashboard** — staggered note grid with search and filter chips (All, Recent, Favorites, Pinned, Archived, Trash) plus a morphing “create” FAB. Optionally **swipe left/right** to move between tabs (toggle in Settings).
- **Home‑screen widgets** — a **Quick Create** widget (one tap to start a note, checklist, expense or board), a **New note** capture button, a **Stats** widget (note count + backup status, no content shown), and a scrollable **Reminders** widget that lists your upcoming reminders and jumps straight to a reminder's note or the add screen. All widgets **resize** on both axes.
- **Reminders** — set a one‑off or repeating (daily / weekly / monthly) reminder from the **+** menu, or straight from a note’s **⋮ → Remind me** (for the whole note or a specific thing). Reminders fire as notifications, survive reboots, and open the linked note on tap. Reminder text is **encrypted on‑device** like everything else; manage alerts and precise timing from **Settings → Notifications**.
- **Multi‑select** — long‑press to select multiple notes and pin / favorite / archive / trash them in bulk. A per‑note `⋮` menu handles single‑note actions.
- **Distraction‑free editor** — debounced auto‑save, a Markdown formatting toolbar with **live styling** (bold / italic / headings render as you type), note color labels, and live word‑count / reading‑time stats.
- **Rich blocks** — interactive **checklists**, **tables** (labelled A/B/C columns and 1/2/3 rows, add / remove, drag to resize), **callouts** (a highlighted tip / warning box), inline **sketch boxes** (a resizable freehand pad), a heading button with **selectable sizes** (long‑press for H1 / H2 / H3), a list button and a quote button that remember your last choice, all interleaved inline with text and media.
- **Draw on the whole page** — tap the pen to drop into **freehand ink mode** and write or annotate with a finger or stylus **anywhere over the note**, mixing handwriting with your typed text, images and tables (not confined to a box). Pick a pen colour and thickness; two‑finger drag scrolls the page while you draw. By default, when you finish a drawing your **typed text continues on a fresh line below it** (so text never lands on your strokes) — switch this to a **free overlay** in **Settings → Additional configurations → Drawing & text**. The ink scrolls and saves with the note, and syncs and exports along with it.
- **Board notes** — a pan‑and‑zoom infinite canvas where freehand drawing is the default; choose pen thickness (long‑press the pen) and colour, draw **shapes** (line, arrow, rectangle, ellipse — long‑press the shape tool to pick), drop **images** (move, resize keeping their shape, and **crop** them right on the board) and draggable text notes that zoom with the board, and **zoom in/out** — pinch zooms in on exactly where your fingers are — across a wide range while everything stays smooth.
- **Expenses** — a private, multi‑account money tracker: swipeable **account tabs** each with their own balance, combinable **tags** (Salary / Primary / Savings / Emergency + your own), **send money** in or out, **transfer** between your own accounts, and build your own **sections** (savings, investments, bills …) that can either spend from the balance or be tracked separately. Add reusable **bank transfers** — a saved *from → to + amount* row you tap to move money between accounts and re‑run every month. Nothing is added by default — you shape it however you like.
- **Checklists** — turn a note into an interactive, checkable to‑do list with a live **progress bar**; press Enter to add the next item, backspace on an empty line to remove it, and use the ⋮ menu to check all, uncheck all, move done to the bottom, or clear completed.
- **Image attachments** — insert photos from the **gallery** or capture live with the **camera**, placed **inline at your cursor** so text and images can be interleaved freely. Drag a corner to **resize** (smoothly) or **crop** an image. Each note keeps its own private, **encrypted‑at‑rest** on‑device copy (removed with the note); captures never leave the app unless you export them.
- **Voice notes** — record audio straight into a note and play it back inline. Recordings are stored **encrypted at rest** on‑device and deleted with the note.
- **Books** — organise notes into nestable folders ("books"). Create a book from the + menu, open it to browse or add notes inside, and move single or multiple notes between books. **Long‑press a book to select several at once** and move or delete them together (its **⋮ menu** still gives per‑book actions). Deleting a book (after a confirmation) moves it and **everything nested inside it to Trash together, keeping its structure**, so you can restore the whole book or delete it for good.
- **Tags** — label notes and search any tag to pull up every note that carries it. Tagging, pin, favorite and colour sit together in one row right under the title on **every** note type (text, checklist, board and expenses).
- **Reusable templates** — kept on a dedicated **templates button in the bottom-left corner**; “Manage templates” lists them, and **New template** opens a full note editor where the title becomes the template name, you pick an icon, and a **Save as template** button stores your draft. Deleted templates go to **Trash** and can be recovered within the retention window.
- **App lock** — optional unlock with **fingerprint, face, or device PIN** (biometric / device‑credential).
- **Read‑only by default** — reopening a note shows it read‑only so you can't change it by accident; tap the **pencil** in the top bar to edit. Every note also has a **⋮ menu** (Share, Export, Move to Trash).
- **Organization** — pin, favorite, archive, color labels, and a recoverable Trash.
- **Share** — share any note as **text, a PDF, or Markdown** through the Android share sheet. When a Google Drive account is connected you also get **“Share a link”**: the note is uploaded to a **“MyNotes Shared”** Drive folder as a readable copy with an **“anyone with the link can view”** permission, and the link is handed to the system share sheet. Unlike synced notes this copy is **not end‑to‑end encrypted** (anyone with the link can read it); revoke access anytime by deleting the file from that Drive folder. Available from both the note’s **⋮ menu** on the home screen and the editor’s share menu.
- **Share encrypted (in‑app)** — send a note **end‑to‑end encrypted** to another MyNotes user: pick a **passphrase**, and the note (with its images/voice notes) is packed into a locked **`.mynote`** file you can send through any app. The recipient chooses **Import note** from the + menu, opens the file, and enters the passphrase to unlock it — nothing is uploaded, and no one without the passphrase (not even the delivery app) can read it.
- **Export** — save any note as **plain text, Markdown, HTML or PDF** (notes with images or voice notes come out as a **ZIP** with their attachments, and any **drawings/whiteboards are rendered into the file** as images), or export a whole **book as a ZIP** that keeps its folder structure and attachments. Exports go straight to your **Downloads/MyNotes** folder by default (no permission needed on Android 10+); pick a different **default export folder** in Settings, or on older devices you'll get the system "Save to…" picker.
- **Optional end‑to‑end encrypted Google Drive sync** — off by default; when enabled, notes (every type — text, checklist, expense, board), your **custom templates** and your **reminders** are encrypted on‑device before upload so **only unreadable blobs** ever reach Drive. Sync runs automatically while the app is open — right after you add or edit something, on a light periodic timer, and when the app is opened — so your devices stay up to date without tapping anything. The key that unlocks them is wrapped by a **recovery passphrase** and kept in Drive's private app folder, so the **same account/passphrase restores everything on a new device**, while a Drive‑share recipient (or Google) can't read them.

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

