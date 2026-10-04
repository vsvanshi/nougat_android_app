# Nougat for Android: handoff

Read `AGENTS.md` first. Keep this file short and true: rewrite "Current state" each session, add to the logs.

## Current state

| | |
|---|---|
| Updated | 2026-10-03 by Claude |
| Phase | 7 and 8. P7.4, P8.1 and P8.2 done: v1.0 is on GitHub Releases. P7.1 to P7.3 built, waiting for Varun to confirm on his phone (he asked to check them himself); he reported UI issues on Now playing on 2026-10-04, one fixed so far (see the session log). |
| Next task | P8.4: Varun opens the IzzyOnDroid inclusion request (text below in the session log); then answer their review. Still open: other Now playing UI issues Varun mentioned, his confirmation of P7.1 to P7.3, predictive back with gesture navigation at the end. |
| Code | `design/` (tokens, type, sizes, components, `Page` with pull to refresh, gallery), `screens/AppShell.kt` (tabs, back stacks, transitions, swipe back, library and artwork for the whole app), `screens/LibraryScreens.kt` (Folders, Songs, Search, Hidden, `TrackRow`), `screens/NowPlaying.kt` (mini player, Now playing, Queue), `screens/Placeholders.kt` (Playlists until phase 4), `screens/Access.kt`, `screens/TrackThumbnail.kt`, `library/` (`Track`, `MediaLibrary` following MediaStore with hiding and sort settings, `Listing` with folder tree, search and name order, `ArtworkStore`). `App.kt` (one library, artwork store and player per process), `playback/PlayQueue.kt`, `playback/Player.kt` (ExoPlayer, Nougat's queue, saved state in `files/player.json`), `playback/PlaybackService.kt` (Media3 session service). 21 JVM tests. Phases 4 to 6: `playlists/` (`Playlists` store, `PlaylistFile` JSON), `screens/PlaylistScreens.kt` (Playlists tab, playlist screen, song picker, add-to-playlist sheet, name dialog, success haptic), `equalizer/` (`EqualizerState`, `EqualizerSettings` saving `equalizer.json`, `SoundEffects`), `screens/EqualizerScreen.kt`, `visualizer/` (`Spectrum`, `SpectrumAnalyzer` with its own FFT, `Visualizer` with six looks), `design/Reorder.kt` (drag to reorder, used by Queue and playlists). 30 JVM tests; lint has no errors. |
| Git | Repository on `main`, remote `origin` = `https://github.com/vsvanshi/nougat_android_app.git`, **public since 2026-10-04** (A23). Commit or push only when Varun says. |
| Tools on the Mac | JDK 17 (Zulu and JetBrains), ffmpeg, Homebrew. Android command-line tools from Homebrew (`sdkmanager` 22.0 on the path). SDK at `~/Library/Android/sdk` with `platforms;android-37.0`, `build-tools;37.0.0`, `platform-tools` 37.0.1 (about 380 MB); SDK licences accepted with Varun's agreement. `adb` on the path is Homebrew's, same version as the SDK one. No emulator (A7). |
| Test phone | Samsung Galaxy A07, Android 16 (API 36), One UI 8.5, arm64, 720 x 1600 at 300 dpi (about 384 dp wide, a narrow screen). USB debugging is on and this Mac is authorised; it shows as `device` in `adb devices`. |
| Known broken | Nothing. Two harmless build warnings: `android.enableJetifier=true` comes from Varun's own `~/.gradle/gradle.properties` (it overrides the project's file, so it cannot be turned off here; do not edit his file without asking), and `Configuration.setVisible` comes from inside the Android Gradle plugin. |
| Release key | `~/.nougat-release/nougat-release.jks` and `keystore.properties` (password inside; both chmod 600), made by Claude on 2026-10-04. Never commit them. Varun should back the folder up somewhere safe: without it no update to the released app can be signed. Certificate SHA-256 `47:E7:05:14:99:34:B9:F0:0D:05:2A:F1:46:AB:C1:F2:D9:99:ED:30:41:73:8D:35:7A:94:07:0C:78:88:70:F9`. Release builds use it when the file exists, else the debug key. |
| Release v1.0 | https://github.com/vsvanshi/nougat_android_app/releases/tag/v1.0 (private repository), asset `Nougat-1.0.apk`, signed with the release key. Published 2026-10-04 with Varun's go-ahead; its APK was replaced the same day, at his request, by a build with the Now playing fix (SHA-256 `5891697c69ee4db6ad12b897cdaa173eca19055292e5698596c9dfab9aaade1f`). The `v1.0` tag still points at `cb8bb29`, before that fix; move it, or make 1.0.1, when Varun says. |
| Release v1.0.1 | https://github.com/vsvanshi/nougat_android_app/releases/tag/v1.0.1, asset `Nougat-1.0.1.apk` (version code 2), signed with the release key, published 2026-10-04 with Varun's go-ahead. The latest release; tag on `b95bd9c`. |
| App on Varun's phone | An optimised build signed with the **debug** key. A release-key APK cannot install over it: Android would need the old one uninstalled first, which deletes Nougat's data (playlists, equalizer, hidden list, position). Ask Varun before doing that. |
| Test music on the phone | `/sdcard/Music/Nougat Test/` (eight short tones in MP3, M4A, FLAC, OGG, Opus, untagged MP3 and WMA, nested folders; Kite Season has embedded art; Rainy Days has `cover.jpg` and `AlbumArt.jpg`), pushed by Claude on 2026-10-03. The two images made Google Photos offer to back up "Rainy Days"; Varun should decline. Remove all with `adb shell rm -rf "/sdcard/Music/Nougat Test"` and the scan command in `PLAN.md`. The phone has one song of its own ("Over the Horizon"). |
| Real music on the phone | `/sdcard/Music/3. Pahari/` (10 songs, 66 MB), copied at Varun's request on 2026-10-03 from his external drive (`/Volumes/Extreme/Music/3. Pahari`). Varun's own music: never delete it. Most files have no artist tag. |
| Gradle download | The wrapper's own Java download of the Gradle zip times out on the GitHub redirect on this Mac, though Gradle itself reaches Google's and Maven's repositories fine. Gradle 9.8.0 was fetched with `curl`, checked against its published SHA-256 and placed in `~/.gradle/wrapper/dists`. A future Gradle upgrade will need the same. |

## Contract audit (P7.2)

`DESIGN.md` section 4, on Varun's Galaxy A07 (Android 16). "Agent" means checked by Claude over `adb`; "Varun" means he tested it.

| Behaviour | Status |
|---|---|
| Back | Passes for the Back button everywhere and for leaving at a tab's root (agent). Predictive back gesture: to check with gesture navigation at the end (Varun's call). |
| Edge to edge | Passes (agent, screenshots): header colour behind the status bar, bottom navigation to the bottom edge. |
| Media notification and lock screen | Passes (Varun, P3.4). |
| Headphone, Bluetooth and car buttons | Passes (Varun, P3.4); media keys also checked by the agent. |
| Audio focus | Passes for calls (Varun, P3.4). Other apps' audio and ducking: handled by Media3, not checked separately. |
| Becoming noisy | Passes (Varun, P3.4). |
| Background playback | Passes (Varun, P3.4; agent saw the foreground service). |
| Permissions | Passes (agent, P2.1): explanation first, refusal keeps the app working, Settings route after a second refusal. |
| Appearance | Passes (Varun checked dark mode in phases 1 and 4 to 6). |
| Font size and display size | To confirm (Varun). Code: rows wrap at large sizes, Now playing scrolls, the mini player grows, the equalizer's marks stop growing at 1.3x, its buttons stack. |
| TalkBack | To confirm (Varun). Code: every icon button has a label; sliders are adjustable; row menus and queue edits are custom actions; the equalizer switch is one labelled control; dimmed equalizer controls are hidden from TalkBack while it is off. |
| Remove animations | To confirm (Varun). Code: page changes, Now playing, Queue and Equalizer become a 150 ms fade; the visualizer stands still. |

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
| A19 | 2026-10-03 | ExoPlayer is given one song at a time and Nougat's `PlayQueue` picks the next, as on iPhone. The media session talks to a `ForwardingPlayer` whose next, previous and play go to the queue, so the notification and headset buttons always offer them. The app and the service share one `Player` owned by the `App`; the activity connects a `MediaController` while visible, which starts the service. Hidden songs stay in the queue; only deleted files leave it. | Same behaviour as iPhone (shuffle, repeat, previous after 3 s, D40) with Media3 still doing focus, noisy, notification and lock screen. Not gapless, like iPhone. |
| A20 | 2026-10-03 | The equalizer sounds through `DynamicsProcessing` on Android 9 and later: its pre-EQ stage with five bands whose edges sit halfway (in octaves) between 60, 230, 910, 3,600 and 14,000 Hz (117, 457, 1,810, 7,100, 20,000 Hz), and its input gain for the preamp and headroom (D43). Android 8 uses the platform `Equalizer`, each of its bands following the nearest of ours, with the preamp added to every band. Effects sit on an audio session Nougat creates and gives ExoPlayer. | Exact frequencies where the system allows them, and no library. The bands are steps between edges rather than the iPhone's shelves and peaks, so the shape differs a little. |
| A21 | 2026-10-03 | The visualizer reads decoded audio through Media3's `TeeAudioProcessor`, only while Now playing is on screen and the app is in front, and turns each 1,024-frame slice into 24 bars on the playback thread. It sees the music before the platform's effects (so not the equalizer) and a buffer ahead of the speaker; marked `ponytail:` in `Player.kt` with how to delay it if the lead shows. | No microphone permission and no library; the platform effects are out of reach of the app. |
| A22 | 2026-10-04 | The release key is kept in `~/.nougat-release/`, outside the repository; `app/build.gradle.kts` uses it when present and the debug key otherwise, so anyone can still build. Release builds are minified and resource-shrunk; APK signature scheme v2 (enough from Android 7). Version 1.0 (code 1), as on iPhone. | Keeps secrets out of a repository that may become public, without making building harder. |
| A23 | 2026-10-04 | Nougat is also offered through IzzyOnDroid, which serves the release APK from GitHub Releases with our own signature, so it updates over the GitHub APK and vice versa. For it: the repository becomes public (changes A13), store text, icon and screenshots live in `fastlane/metadata/android/en-US` with a changelog per version code, and the APK carries no dependency block (`dependenciesInfo` off). Every release raises `versionCode` and gets a tag. Main F-Droid may follow later. | Varun's choice; faster than main F-Droid and keeps one signature. Extends A12. |

## Session log

Newest first. Copy this template for each session:

```
### YYYY-MM-DD, Claude or ChatGPT
- Tasks: IDs worked on, and whether each is finished
- Changed: main files or folders touched
- Verified: built? tests run? seen on a device? Say "not verified" where true
- Left for next: anything half-done, broken, or worth knowing
```

### 2026-10-04, Claude (fifteenth session)
- Tasks: P8.2 finished. With Varun's go-ahead: committed (`Add settings, accessibility, signing and readme`), pushed `main`, and published v1.0 with the signed APK. Varun then stopped the session: Now playing had UI issues. Screenshot showed the controls row cut off; fixed. At his request the release APK was replaced with the fixed build and the fix committed (not pushed).
- Changed: `screens/NowPlaying.kt` (the scrolling details take all the space above the equalizer shortcut; the Spacer that halved it is gone). `PLAN.md` (P8.2 ticked), this file.
- Verified: screenshot of Now playing on the phone after the fix (controls whole); the published asset's SHA-256 matches the new release build; 30 unit tests pass.
- Worth knowing: to put a debug-key build on Varun's phone (keeping its data) while the release key exists, build with `~/.nougat-release` moved aside for the build, then move it back; a release-key APK cannot install over the debug-key app without uninstalling it. Varun said there were UI issues (plural); only the cut-off controls are known so far.
- Later: Varun asked for a progress line in the mini player; added (2 dp along its top edge, `MiniProgress` in `NowPlaying.kt`, `DESIGN.md` mini player row), built and installed (debug key); it shows in the store screenshots.
- Later still: Varun asked about F-Droid, chose IzzyOnDroid and said to make the repository public (A23). Checked the repository and its history for secrets and device identifiers (none; only Mac paths, the phone model and the name of his music folder appear in the docs). Added `fastlane/metadata/android/en-US` (title, short and full description, changelogs 1 and 2, 512 px icon from the iPhone icon, four phone screenshots taken for it: a folder, Now playing, the equalizer on Bass boost, playlists; the equalizer was set back to Off and Flat afterwards), `dependenciesInfo` off, version 1.0.1 (code 2). Release APK built and verified with the release key. PLAN P8.4 added.
- Varun then said go: committed (`Add mini player progress and store listing for IzzyOnDroid`), pushed, made the repository public, published v1.0.1.
- IzzyOnDroid request, for Varun to open at https://gitlab.com/IzzyOnDroid/repo/-/issues (new issue, inclusion request): app Nougat, package `in.varunsuryawanshi.nougat`, source https://github.com/vsvanshi/nougat_android_app, licence MIT, APKs on GitHub Releases (latest v1.0.1, signing certificate SHA-256 as above), metadata in `fastlane/metadata/android/en-US`, no trackers, no Google services, no network use; libraries AndroidX, Compose, Media3, Navigation 3, Guava, Kotlin (all Apache 2.0).
- Left for next: Varun's IzzyOnDroid request and their answer; other Now playing issues still to hear.

### 2026-10-04, Claude (fourteenth session)
- Tasks: committed phases 4 to 6 at Varun's request (he had tested them). Phase 7 and phase 8 up to P8.2, at his request, with screenshots only where needed. P7.4 and P8.1 finished; P7.1 to P7.3 built for Varun to confirm; P8.2 signed, release not yet published.
- Changed: new `screens/SettingsScreen.kt` (accent swatches, Hidden link, version, licence screen), `res/raw/apache_2_0.txt`, `README.md`; Folders menu "Settings" (replaces the Hidden entry); `design/Metrics.kt` (`rememberReducedMotion`), fades with animations removed in `AppShell.kt` and `NowPlaying.kt`; large text: Now playing scrolls, mini player grows, equalizer marks capped; TalkBack: equalizer switch row, dimmed controls hidden, mini player click label; speed: titles sorted off the main thread, search and song picker off the main thread with a 150 ms pause, the Folders top folder remembered, a second simultaneous library read avoided (`Mutex`); `app/build.gradle.kts` (release signing from `~/.nougat-release/`, resource shrinking, version 1.0). Decision A22. Contract audit section above.
- Verified: build, 30 unit tests, lint 0 errors. P7.4 with 5,018 songs on the phone (no screenshots, numbers from logcat and `dumpsys gfxinfo`): the library read takes 432 ms on a background thread and the app opens in 0.6 to 0.8 s; Songs flinging 2 janky frames of 776; opening a folder and flinging, 95th percentile 14 ms after the fixes (65 ms before); typing in search has 8 janky frames of 36 with or without results, so it is the keyboard on this phone, not the library. The 5,000 test songs were removed. The release APK verifies with the new key (`apksigner`).
- Left for next: Varun's confirmation of P7.1 to P7.3 and his go-ahead to push and publish the release.

### 2026-10-03, Claude (thirteenth session)
- Tasks: committed phase 3 at Varun's request (`Add playback`), after he tested P3.4 on his phone. Phases 4, 5 and 6 built in one go; Varun asked the agent not to check them on the phone, he will.
- Changed: new `playlists/`, `equalizer/`, `visualizer/` packages, `screens/PlaylistScreens.kt`, `screens/EqualizerScreen.kt`, `design/Reorder.kt`, tests for playlists, equalizer state and the spectrum analyser (ported from iPhone, plus an FFT check); `playback/Player.kt` (audio session, equalizer effects, the analyser tap, spectrum reset on pause); `App.kt` (playlists, equalizer); `screens/NowPlaying.kt` (visualizer on the cover with six looks, tap to change, add to playlist, equalizer shortcut and screen, the queue on the shared drag helper); `screens/LibraryScreens.kt` (Add to playlist in menus and on swipe, playlists in search); `screens/AppShell.kt` (playlist routes, the add sheet, snackbars above Now playing); `design/Buttons.kt` (chip long press), `design/Rows.kt` (`menuOnLongPress`), `design/Page.kt` (`list` parameter); `screens/Placeholders.kt` removed. Decisions A20, A21; `DESIGN.md` rows.
- Verified: 30 unit tests pass; debug and release builds; lint 0 errors (it caught three Android 9 calls without a version check and a wrong Media3 constant, fixed). The release build is installed on the phone. Nothing in phases 4 to 6 was looked at on the phone.
- For Varun to check: playlists (create, rename, delete, open, add songs from the picker, from menus, from a swipe and from Now playing, Undo, haptic, drag to reorder, swipe to remove, survive a restart); the equalizer (hear each preset and the preamp, on and off, save and delete a preset, the 0 dB tick, survive a restart); the visualizer (moves with the music over a cover and without one, tap through the six looks, look remembered, bars fall when paused, whether bars lead the sound).
- Left for next: fixes from Varun's check, then phase 7. Varun then tested phases 4 to 6 on his phone, reported no problems and asked for a commit (P4.2, P5.2, P5.3, P6.2 ticked).

### 2026-10-03, Claude (twelfth session)
- Tasks: committed phase 2 at Varun's request (`Add folders, search, artwork and hiding`). P3.1, P3.2, P3.3 finished; P3.4 needs Varun.
- Changed: new `App.kt`, `playback/PlayQueue.kt`, `playback/Player.kt`, `playback/PlaybackService.kt`, `PlayQueueTests.kt`; `screens/NowPlaying.kt` rewritten (real mini player, Now playing, Queue with drag and swipe); `screens/LibraryScreens.kt` (taps play the list they are in, folder Play and Shuffle, Shuffle all; playing song highlighted; "Unsupported format"); `screens/AppShell.kt` (shared player, status bar over the queue); `MainActivity.kt` (media controller); `design/Rows.kt` (dimmed rows keep their menu); `library/MediaLibrary.kt` (`track(path)`, `hasRead`, `readCount`); manifest (service, foreground and wake-lock permissions, `App`); Media3 1.11.1. Decision A19; `DESIGN.md` mini player and Queue rows.
- Verified on the phone (optimised build, phone muted, so state read from `dumpsys media_session`): tapping a song plays its list; songs follow each other at their end, skipping the WMA; next, previous (restart after 3 s), seek, shuffle and repeat (filled circles) work from Now playing; the media notification is a foreground service with three actions; media keys play, pause, skip and go back, also from the home screen; after Home and a force-stop the same song resumes at the same second; Queue: tap jumps, long-press drag reorders, swipe removes, status bar dark over it. 21 unit tests pass. Two bugs found and fixed on the way: the restored position was cleared before the seek, and the queue rows' long-press menu blocked dragging.
- Not verified by the agent (P3.4, for Varun): lock screen, real Bluetooth or wired buttons, calls, unplugging, screen-off playback over time, and anything audible.
- Left for next: Varun's P3.4 checks; phase 4. Varun then tested P3.4 with real music (lock screen, headphones, calls, unplugging, screen off), said it all works and asked for a commit (P3.4 ticked).

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
