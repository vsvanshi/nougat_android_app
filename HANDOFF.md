# Nougat for Android: handoff

Read `AGENTS.md` first. Keep this file short and true: rewrite "Current state" each session, add to the logs.

## Current state

| | |
|---|---|
| Updated | 2026-10-03 by Claude |
| Phase | 2, library, finished (P2.1 to P2.5). Next is phase 3, playback. |
| Next task | P3.1: port `PlayQueue` with its tests. |
| Code | `design/` (tokens, type, sizes, components, `Page` with pull to refresh, gallery), `screens/AppShell.kt` (tabs, back stacks, transitions, swipe back, library and artwork for the whole app), `screens/LibraryScreens.kt` (Folders, Songs, Search, Hidden, `TrackRow`), `screens/NowPlaying.kt` (mini player and Now playing, still a sample song), `screens/Placeholders.kt` (Playlists until phase 4), `screens/Access.kt`, `screens/TrackThumbnail.kt`, `library/` (`Track`, `MediaLibrary` following MediaStore with hiding and sort settings, `Listing` with folder tree, search and name order, `ArtworkStore`). 13 JVM tests. Song taps and Play show "Playback arrives in phase 3". |
| Git | Repository on `main`, remote `origin` = `https://github.com/vsvanshi/nougat_android_app.git`, private. Commit or push only when Varun says. |
| Tools on the Mac | JDK 17 (Zulu and JetBrains), ffmpeg, Homebrew. Android command-line tools from Homebrew (`sdkmanager` 22.0 on the path). SDK at `~/Library/Android/sdk` with `platforms;android-37.0`, `build-tools;37.0.0`, `platform-tools` 37.0.1 (about 380 MB); SDK licences accepted with Varun's agreement. `adb` on the path is Homebrew's, same version as the SDK one. No emulator (A7). |
| Test phone | Samsung Galaxy A07, Android 16 (API 36), One UI 8.5, arm64, 720 x 1600 at 300 dpi (about 384 dp wide, a narrow screen). USB debugging is on and this Mac is authorised; it shows as `device` in `adb devices`. |
| Known broken | Nothing. Two harmless build warnings: `android.enableJetifier=true` comes from Varun's own `~/.gradle/gradle.properties` (it overrides the project's file, so it cannot be turned off here; do not edit his file without asking), and `Configuration.setVisible` comes from inside the Android Gradle plugin. |
| Test music on the phone | `/sdcard/Music/Nougat Test/` (eight short tones in MP3, M4A, FLAC, OGG, Opus, untagged MP3 and WMA, nested folders; Kite Season has embedded art; Rainy Days has `cover.jpg` and `AlbumArt.jpg`), pushed by Claude on 2026-10-03. The two images made Google Photos offer to back up "Rainy Days"; Varun should decline. Remove all with `adb shell rm -rf "/sdcard/Music/Nougat Test"` and the scan command in `PLAN.md`. The phone has one song of its own ("Over the Horizon"). |
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
| A15 | 2026-10-03 | Navigation uses AndroidX Navigation 3 (`navigation3-ui` 1.2.0, stable). The app owns one back-stack list per tab and saves them itself; `NavDisplay` shows the current tab's stack and handles back and predictive back. | It is AndroidX (no approval needed), and owning the lists makes per-tab stacks, popping to a tab's root and saving simple. |
| A16 | 2026-10-03 | A track's path is relative to the storage root on the phone's own storage ("Music/Road Trip/Kite Season.mp3") and starts with the volume's name on any other, such as an SD card. Only files MediaStore marks as music (`IS_MUSIC`) are listed, so ringtones, notifications and recordings stay out. Unplayable on Android: WMA, APE, WavPack. | One identity per file across volumes (no duplicates), and the usual music-player filter. |
| A17 | 2026-10-03 | The top of the Folders tab is the deepest folder that holds every song, titled "Music", so a library entirely inside "Music" opens inside it instead of on a lone "Music" folder. | MediaStore paths start at the storage root; one extra tap on every visit is not worth it. |
| A18 | 2026-10-03 | Cover art on Android 10 and later comes from the system's media library (`loadThumbnail`): embedded art, or an `AlbumArt*.jpg` beside the song. A `cover.jpg` or `folder.jpg` is used only on Android 8 and 9. | Audio-only access cannot open image files; reading them would need the photos permission, too much to ask of a music player. The system's own thumbnails need nothing more. |

## Session log

Newest first. Copy this template for each session:

```
### YYYY-MM-DD, Claude or ChatGPT
- Tasks: IDs worked on, and whether each is finished
- Changed: main files or folders touched
- Verified: built? tests run? seen on a device? Say "not verified" where true
- Left for next: anything half-done, broken, or worth knowing
```

### 2026-10-03, Claude (eleventh session)
- Tasks: committed P2.1 at Varun's request (`Read the music library`). P2.2, P2.3, P2.4 and P2.5, all finished; Varun asked for the whole phase and a short report.
- Changed: new `library/Listing.kt`, `library/ArtworkStore.kt`, `screens/LibraryScreens.kt`, `screens/TrackThumbnail.kt`, `ListingTests.kt`; `library/MediaLibrary.kt` (follows MediaStore with a content observer and a conflated request channel, hiding, sort settings); `design/Page.kt` (pull to refresh); `screens/AppShell.kt` (Hidden screen, library follows while resumed); `screens/Placeholders.kt` now only Playlists. Decisions A17, A18; `DESIGN.md` rows for the top of Folders, cover art and pull to refresh.
- Verified on the phone (optimised build): Folders opens on "Music" with real folders and counts, three levels deep; Hide on a folder with Undo, still hidden after a force-stop, shown again from the Hidden screen; a song pushed and then deleted from outside appears and vanishes while the app is open; search finds songs by title, artist and album, and folders by name, and a folder result opens; Songs sorts by name and by date added; pull to refresh reads the library again; embedded art and `AlbumArt.jpg` thumbnails show, `cover.jpg` does not on Android 16 (A18). Scrolling 508 songs with art: 1 janky frame of 1,180, 95th percentile 14 ms (`dumpsys gfxinfo`); the 500-song folder was removed afterwards. 13 unit tests pass.
- Left for next: phase 3. Not committed yet (only P2.1 was).

### 2026-10-03, Claude (tenth session)
- Tasks: committed P1.4 at Varun's request (`Add app shell and folder navigation`). P2.1, finished.
- Changed: new `library/Track.kt`, `library/MediaLibrary.kt`, `screens/Access.kt`, `TrackTests.kt`; manifest (`READ_MEDIA_AUDIO`, and `READ_EXTERNAL_STORAGE` up to Android 12); `design/Rows.kt` (`enabled`, greying a row out); `screens/AppShell.kt` (library created and refreshed on every resume, `LocalLibrary`); `screens/Placeholders.kt` (real Songs list, access request on Folders root). Decision A16. `PLAN.md`: the scan command for pushed test music.
- Verified on the phone with the optimised build: with access reset (`pm revoke` and `pm clear-permission-flags` on Nougat only) the explanation shows with Allow; Allow opens Android's dialog; one refusal keeps Allow and asks again; a second refusal turns the button into Open settings; granting while the app is away and coming back lists the songs (8 read in 65 ms). Titles, artists and durations match the tags; the untagged file shows its file name and "Unknown artist"; WMA is greyed out. 8 unit tests pass.
- Worth knowing: when a song has no album tag, MediaStore fills in the folder's name, which cannot be told apart from a real tag; harmless for now (the album shows only in Now playing). Android 11 and later can reset an unused app's permissions by itself, which is why the Open settings state is worked out from Android's answer rather than remembered.
- Left for next: P2.2.

### 2026-10-03, Claude (ninth session)
- Tasks: P1.4 follow-up. Varun found plain folder navigation flat and asked for a swipe animation or something more creative; predictive back with gesture navigation is to be checked at the end.
- Changed: `screens/AppShell.kt` (slide push and pop, `SharedTransitionLayout` with `sharedTitle`, `swipeBack` feeding `DirectNavigationEventInput`; the Folders root decides its own back arrow by path, which fixed the page underneath changing its bar mid-animation), `design/Rows.kt` and `design/Page.kt` (`titleModifier`), `screens/Placeholders.kt`; `app/build.gradle.kts` (release build minified, signed with the debug key for now, marked `ponytail:`). `DESIGN.md`: "Moving between pages" row. `PLAN.md`: release build commands.
- Verified with screen recordings cut into frames: on the optimised build the page slides in while the folder name grows into the title, and a slow drag to the right takes the page back with the name returning to its row. Search also slides. The debug build stalls about 200 ms on the first frame of a push, so judge motion on the release build. Tests pass.
- Left for next: Varun's look (he then said it is fine and asked for a commit; P1.4 ticked); the optimised build is installed, so the design gallery is not on the phone right now (install the debug build to get it back).

### 2026-10-03, Claude (eighth session)
- Tasks: committed P1.3 at Varun's request (`Add components`). P1.4 built, not ticked yet (waiting for Varun, see Next task).
- Changed: new `screens/AppShell.kt`, `screens/NowPlaying.kt`, `screens/Placeholders.kt`; six more Material icons (`search`, `arrow_back`, `expand_more`, `music_note`, `queue_music`, `close`) from google/material-design-icons; `design/Page.kt` (status-bar colour moved to the shell, no bottom padding by default), `design/Buttons.kt` (`BarIcon`), `design/Gallery.kt` (pushed inside the shell, uses the shell's snackbars); `MainActivity.kt`; manifest (`enableOnBackInvokedCallback`); `app/build.gradle.kts` (Navigation 3). Decision A15. `DESIGN.md`: tabs, mini player and Now playing rows.
- Verified on the phone (screenshots after each step): Folders, Songs and Playlists tabs; opening folders two deep and Back one step; switching tabs keeps each tab's place; tapping the showing tab returns to its root; Back at a tab's root leaves the app to the launcher; Search opens with the keyboard and dark status-bar icons on paper; Now playing opens from the mini player and closes with Back, the close button and a long swipe down on the cover, while a short swipe springs back; the header colour reaches the top of the screen and the bottom navigation reaches the bottom edge. 5 unit tests pass. Not verified: predictive back gesture (the phone uses three-button navigation), dark mode, keeping the stacks across a configuration change.
- Worth knowing: `uiautomator dump` can lag a step behind on this phone; trust screenshots. Varun sometimes uses the phone during a session, so check the starting state before each automated step. A horizontal or vertical drag that the list cannot scroll counts as a tap on a row (standard Compose behaviour).
- Left for next: Varun's checks for P1.4, then P2.1.

### 2026-10-03, Claude (seventh session)
- Tasks: committed P1.2 at Varun's request (`Add type, sizes and icons`). P1.3 built, not ticked yet (dark mode not seen).
- Changed: new `design/Page.kt`, `Buttons.kt`, `Rows.kt`, `Slider.kt`, `Messages.kt`; `Metrics.kt` (depth shadows with Compose `dropShadow`), `Theme.kt` (Material colour scheme), `Gallery.kt` (now itself a header page with a components section and a working snackbar). Slider tests ported. `DESIGN.md`: the Android top bar and snackbar rows.
- Verified: 5 unit tests pass. On the phone, light mode: compared with the iPhone `design/preview.html` (rendered with headless Chrome): header, play button on the header edge, filled and text buttons, chips, switch, toggle icons, both sliders, song and folder rows, snackbar and empty state all match. Scrolling: the header colour stays behind the status bar, and once the dark block reaches the bar it turns paper with the title and dark status-bar icons. Long press on a row opens its menu at the row; Back closes it. Not checked by the agent: dark mode, TalkBack and the largest font (system settings it did not change; TalkBack and font size are P7.3).
- Left for next: Varun's look in dark mode, then tick P1.3 and start P1.4. Varun then checked dark mode, said it is fine, and asked for a commit (P1.3 ticked).

### 2026-10-03, Claude (sixth session)
- Tasks: committed P1.1 at Varun's request (`Add colours and accents`). P1.2, finished.
- Changed: `design/Type.kt`, `design/Metrics.kt`, 14 icons converted from the iPhone SVGs into `res/drawable/ic_*.xml`, `design/Gallery.kt` (type and icon sections, tokens used in place of raw sizes). `DESIGN.md`: where the extra bar icons come from.
- Verified: built and installed; on the phone the gallery shows all eight text styles and all 14 icons. The phone's `sans-serif` is Roboto (`/system/etc/fonts.xml`). Largest font size not checked yet (that is P7.3).
- Left for next: P1.3.

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
