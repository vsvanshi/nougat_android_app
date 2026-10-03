# Nougat for Android: handoff

Read `AGENTS.md` first. Keep this file short and true: rewrite "Current state" each session, add to the logs.

## Current state

| | |
|---|---|
| Updated | 2026-10-03 by Claude |
| Phase | Not started. Only the documents exist: `AGENTS.md`, `CLAUDE.md`, `DESIGN.md`, `PLAN.md`, this file. |
| Next task | Ask Varun the Open questions (at least the phone and the tools), then P0.1. |
| Code | None. |
| Git | Repository on `main`, remote `origin` = `https://github.com/vsvanshi/nougat_android_app.git`. The documents are committed and pushed. Commit or push only when Varun says. |
| Tools on the Mac | JDK 17 (Zulu and JetBrains) and `adb` are installed. Android Studio and the Android SDK are not. ffmpeg is installed. |
| Known broken | Nothing. |

## Where the reference is

The finished iPhone version: `/Users/varun/work/NougatMobileApp`. Read its `HANDOFF.md` "Decisions" table once; the decision numbers quoted in `PLAN.md` (D32, D41 and so on) are from there. Never edit that folder.

## Open questions for Varun

1. **The phone.** Varun has an Android phone (said 2026-10-03). Which model and Android version? To test on it from the Mac, it needs Developer options with USB debugging on, then a USB cable; `adb` can then install builds, take screenshots and tap, so Claude can check screens on it directly.
2. **Oldest Android supported.** The plan says Android 8.0 (API 26), which covers almost every phone in use. Newer only (Android 10 or 12) would simplify storage and the equalizer.
3. **Where the music comes from.** The plan reads the music already on the phone through the system's media library, so copying music into the Music folder over USB is enough, the way Android players worked in 2016. The iPhone way, copying folders into the app, would also work on Android but uses twice the space. Media library (recommended) or copying?
4. **What "remove" means.** With the media library, songs are the real files on the phone, not a copy. Options: "Hide folder" (Nougat stops showing it; nothing is deleted) and "Delete from phone" (deletes the file after Android's own confirmation). The plan assumes both, with hide as the usual one.
5. **Search.** Bottom navigation with Folders, Songs and Playlists, and search as an icon at the top (usual on Android), or a fourth Search tab like the iPhone?
6. **Installing the tools.** Android Studio is not needed. The plan's suggestion: Google's command-line tools through Homebrew (`brew install --cask android-commandlinetools`), then with `sdkmanager` the platform tools, one SDK platform, build tools, the emulator and one arm64 system image; about 2 to 2.5 GB in all, most of it the emulator image, plus Gradle's own downloads on the first build. The emulator runs without a window (`emulator -avd <name> -no-window`) and Claude drives it with `adb` (install, `screencap`, `input tap`). May Claude download these?
7. **Distribution.** A signed APK on GitHub Releases (free), or the Play Store too (one-time USD 25 developer account)?
8. **Repository.** `nougat_android_app` exists on GitHub. Public, like the iPhone one, or private for now?
9. **Accent "System".** On Android 12 and later the system can supply colours from the wallpaper (Material You). Add it as a seventh accent choice, or keep the six?

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

## Session log

Newest first. Copy this template for each session:

```
### YYYY-MM-DD, Claude or ChatGPT
- Tasks: IDs worked on, and whether each is finished
- Changed: main files or folders touched
- Verified: built? tests run? seen on a device? Say "not verified" where true
- Left for next: anything half-done, broken, or worth knowing
```

### 2026-10-03, Claude
- Tasks: none from the plan. Wrote the starting documents at Varun's request, from the finished iPhone version.
- Changed: `AGENTS.md`, `CLAUDE.md`, `DESIGN.md`, `PLAN.md`, this file.
- Verified: nothing to build. Checked the Mac's tools (see Current state).
- Left for next: the Open questions, then P0.1. Later the same day: `git init` and the remote added, the name settled (A6), and the documents committed and pushed, all at Varun's request.
