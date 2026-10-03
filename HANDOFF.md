# Nougat for Android: handoff

Read `AGENTS.md` first. Keep this file short and true: rewrite "Current state" each session, add to the logs.

## Current state

| | |
|---|---|
| Updated | 2026-10-03 by Claude |
| Phase | 1, design system and shell. P1.1 done. |
| Next task | P1.2: type, metrics, icons. |
| Code | `design/Colors.kt` (fixed tokens, light and dark), `design/Accent.kt` (seven accents, saved in `SharedPreferences` "nougat", key "accent"), `design/Theme.kt` (`NougatTheme` provides `LocalColors` and `LocalAccent`), `design/Gallery.kt` (debug catalogue, shown by `MainActivity` until the shell exists; it is in the main source set for now, so P1.4 should move it behind a debug-only entry). Wave N icon and launch screen. Unit tests in `app/src/test/.../DesignSystemTests.kt`. Material 3's own `MaterialTheme` colour scheme is not mapped to the tokens yet: do it when the first Material component is used (P1.3 or P1.4). |
| Git | Repository on `main`, remote `origin` = `https://github.com/vsvanshi/nougat_android_app.git`, private. Commit or push only when Varun says. |
| Tools on the Mac | JDK 17 (Zulu and JetBrains), ffmpeg, Homebrew. Android command-line tools from Homebrew (`sdkmanager` 22.0 on the path). SDK at `~/Library/Android/sdk` with `platforms;android-37.0`, `build-tools;37.0.0`, `platform-tools` 37.0.1 (about 380 MB); SDK licences accepted with Varun's agreement. `adb` on the path is Homebrew's, same version as the SDK one. No emulator (A7). |
| Test phone | Samsung Galaxy A07, Android 16 (API 36), One UI 8.5, arm64, 720 x 1600 at 300 dpi (about 384 dp wide, a narrow screen). USB debugging is on and this Mac is authorised; it shows as `device` in `adb devices`. |
| Known broken | Nothing. Two harmless build warnings: `android.enableJetifier=true` comes from Varun's own `~/.gradle/gradle.properties` (it overrides the project's file, so it cannot be turned off here; do not edit his file without asking), and `Configuration.setVisible` comes from inside the Android Gradle plugin. |
| Gradle download | The wrapper's own Java download of the Gradle zip times out on the GitHub redirect on this Mac, though Gradle itself reaches Google's and Maven's repositories fine. Gradle 9.8.0 was fetched with `curl`, checked against its published SHA-256 and placed in `~/.gradle/wrapper/dists`. A future Gradle upgrade will need the same. |

## Where the reference is

The finished iPhone version: `/Users/varun/work/NougatMobileApp`. Read its `HANDOFF.md` "Decisions" table once; the decision numbers quoted in `PLAN.md` (D32, D41 and so on) are from there. Never edit that folder.

## Open questions for Varun

None right now.

## Decisions

Settled. Do not reverse without Varun. Add new ones at the bottom with a date.

| # | Date | Decision | Why |
|---|---|---|---|
| A1 | 2026-10-03 | A separate project in `/Users/varun/work/NougatAndroid`, next to the iPhone one, never inside it. | The iPhone repo must not mention Android, and the two build systems have nothing to share. |
| A2 | 2026-10-03 | Native Kotlin and Jetpack Compose. | The iPhone version is fully native; this one should feel just as native. |
| A3 | 2026-10-03 | Behaviour matches the iPhone version unless `DESIGN.md` here or a decision here says otherwise. | One product on two platforms. |
| A4 | 2026-10-03 | No dependencies outside AndroidX / Jetpack without Varun's approval. | Same as the iPhone version: buildable with the stock tools, nothing to trust beyond Google's own libraries. |
| A5 | 2026-10-03 | Commit only when Varun says; short plain messages; no co-author or AI attribution lines. | Varun's standing instruction. |
| A6 | 2026-10-03 | The name stays Nougat on Android, with the same Wave N icon. A Play Store listing, if one comes, gets a distinctive title such as "Nougat: Folder Music Player" and says it is independent of Google. | Varun's choice. Play already allows other apps with "Nougat" in the name; only a listing needs to stand out from them. |
| A7 | 2026-10-03 | Testing is on Varun's phone over USB (`adb`). No emulator. | Varun's choice. A real device is the truer test and saves the emulator image download. |
| A8 | 2026-10-03 | `minSdk` 26 (Android 8.0). | Varun's choice: covers almost every phone in use. |
| A9 | 2026-10-03 | Music comes from the system media library (`MediaStore`); nothing is copied into the app. | The usual Android way; Varun's choice. |
| A10 | 2026-10-03 | Where the iPhone version removes a song or folder, Android only hides it: Nougat stops showing it and never deletes a file. Hidden items can be shown again from Settings. | Varun's choice: no "Delete from phone". Without an unhide, a hidden folder would be lost for good. |
| A11 | 2026-10-03 | Where Android has a usual way of doing something, follow it. Search is an icon in the top app bar, not a fourth tab. | Varun's choice. |
| A12 | 2026-10-03 | Distribution: a signed APK on GitHub Releases only. No Play Store for now. | Varun's choice. |
| A13 | 2026-10-03 | The repository stays private for now. | Varun's choice. Still keep private details out of it, in case it becomes public. |
| A14 | 2026-10-03 | A seventh accent, "System", uses the wallpaper colours (Material You) on Android 12 and later; it is not offered on older versions. Teal stays the default. | Varun's choice. |

## Session log

Newest first. Copy this template for each session:

```
### YYYY-MM-DD, Claude or ChatGPT
- Tasks: IDs worked on, and whether each is finished
- Changed: main files or folders touched
- Verified: built? tests run? seen on a device? Say "not verified" where true
- Left for next: anything half-done, broken, or worth knowing
```

### 2026-10-03, Claude (fifth session)
- Tasks: committed P0 at Varun's request (`Set up the Android project with icon and launch screen`, not pushed). P1.1, finished.
- Changed: `design/Colors.kt`, `design/Accent.kt`, `design/Theme.kt`, `design/Gallery.kt`, `MainActivity.kt`, `DesignSystemTests.kt`, `app/build.gradle.kts` (JUnit 4.13.2 for tests, as the plan says). `DESIGN.md`: the System accent's tones.
- Verified: 4 unit tests pass (fixed colours match the design; the six accents and the System accent at any hue meet the iPhone contrast rules; dark mode shares one accent colour). Breaking a System tone on purpose made the test fail. On the phone: the gallery shows every swatch with its light and dark hex side by side; picking System recolours at once and survives a force-stop and restart. The phone's real wallpaper palette also passes every rule (closest: play glyph 3.17 against 3). The gallery itself was seen by the agent only in light mode; Varun then checked it in dark mode and said it is fine.
- Left for next: P1.2. The phone is left on the System accent.

### 2026-10-03, Claude (fourth session)
- Tasks: P0.2, finished. Launcher icon and launch screen pulled forward at Varun's request.
- Changed: new `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, Gradle wrapper, `.gitignore`, `LICENSE` (copied from the iPhone repo), `app/` (manifest, `MainActivity.kt`, icon and theme resources); `local.properties` written but ignored. `PLAN.md` (commands checked, P0.2 ticked).
- Verified: `./gradlew assembleDebug` builds; `testDebugUnitTest` runs (no tests yet). Installed on the phone and launched: screenshots show the launch screen with the mark, then the app with light status-bar icons over the header colour. The launcher icon on the home screen was not seen by the agent.
- Left for next: P0.3 (Varun looks at the app and the icon), then P1.1. Varun then confirmed the icon (P0.3 ticked) and asked for a commit.

### 2026-10-03, Claude (third session)
- Tasks: P0.1, finished.
- Changed: installed the `android-commandlinetools` Homebrew cask, then the SDK packages above into `~/Library/Android/sdk`; Varun agreed to accepting the SDK licences. `PLAN.md` (P0.1 ticked, SDK location note), this file.
- Verified: `sdkmanager --list_installed` shows the platform, build tools and platform tools; the phone shows as `device` in `adb devices`.
- Left for next: P0.2.

### 2026-10-03, Claude (second session)
- Tasks: none from the plan. Took Varun's answers to the open questions and recorded them as decisions A7 to A14.
- Changed: `HANDOFF.md`, `PLAN.md` (open question numbers replaced by decision numbers, no emulator, no Play Store, hide instead of remove, seventh accent), `DESIGN.md` (search icon, hide, System accent).
- Verified: read the phone's details over `adb`; checked on GitHub that the repository is private. Nothing to build.
- Left for next: P0.1.

### 2026-10-03, Claude
- Tasks: none from the plan. Wrote the starting documents at Varun's request, from the finished iPhone version.
- Changed: `AGENTS.md`, `CLAUDE.md`, `DESIGN.md`, `PLAN.md`, this file.
- Verified: nothing to build. Checked the Mac's tools (see Current state).
- Left for next: the Open questions, then P0.1. Later the same day: `git init` and the remote added, the name settled (A6), and the documents committed and pushed, all at Varun's request.
