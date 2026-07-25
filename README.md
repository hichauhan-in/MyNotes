<div align="center">

# MyNotes+

### Private. Encrypted. Yours.

An offline‑first, privacy‑first, **encrypted** note‑taking app for Android. Every note is
encrypted at rest with AES‑256‑GCM using a key held in the Android Keystore — plaintext
never touches storage, and no account or login is ever required.

</div>

---

## ✨ Features

- **End‑to‑end on‑device encryption** — notes are serialized and encrypted with **AES‑256‑GCM**; the key lives in the **Android Keystore** and is decrypted only in memory.
- **Offline‑first** — everything works with no internet connection. No sign‑in required.
- **Premium, neumorphic UI** — a calm indigo→violet design system built on **Material 3**, with light / dark / system themes and optional **dynamic color**.
- **Fast home dashboard** — staggered note grid with search and filter chips (All, Recent, Favorites, Pinned, Archived, Trash) plus a morphing “create” FAB.
- **Multi‑select** — long‑press to select multiple notes and pin / favorite / archive / trash them in bulk. A per‑note `⋮` menu handles single‑note actions.
- **Distraction‑free editor** — debounced auto‑save, a Markdown formatting toolbar, note color labels, and live word‑count / reading‑time stats.
- **App lock** — optional unlock with **fingerprint, face, or device PIN** (biometric / device‑credential).
- **Organization** — pin, favorite, archive, color labels, and a recoverable Trash.
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

Planned / in progress (see [`plan.txt`](plan.txt) for the full vision): rich‑text & checklists, drawing canvas, templates, tags & folders, reminders, full Google Drive encrypted sync, import/export, and home‑screen widgets.

---

## 📄 License

Copyright © 2026. All rights reserved. (Add your preferred license here before open‑sourcing.)

