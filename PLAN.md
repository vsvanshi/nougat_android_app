# Nougat for Android: plan

Read `AGENTS.md` first. The iPhone version at `/Users/varun/work/NougatMobileApp` is the reference: each task names the Swift files and tests to read before porting.

## Stack

- Kotlin, Jetpack Compose, one `app` module, Gradle with the Kotlin DSL and the Gradle wrapper.
- Material 3 Compose components for structure (scaffold, top app bar, navigation bar, dialogs, bottom sheets, menus, switch, snackbar host), themed entirely with Nougat's tokens. Our own Compose drawing for the header, rows, play button, chips, slider and visualizer, as on iPhone.
- Playback: AndroidX Media3 (ExoPlayer and a `MediaSessionService`). It gives the notification, lock screen, Bluetooth and car buttons, audio focus and "becoming noisy" handling.
- Library: the system `MediaStore`, so music already on the phone appears without copying (open question 4 in `HANDOFF.md`).
- Saved state: small JSON files in the app's files folder, using Android's built-in `org.json`, as on iPhone (`library`, `player`, `playlists`, `equalizer`). The accent goes in `SharedPreferences`.
- No dependencies outside AndroidX / Jetpack without Varun's approval. In particular no image library (decode artwork with `MediaMetadataRetriever` and `BitmapFactory`) and no FFT library (write a small radix-2 FFT).
- `minSdk` 26 (Android 8.0), `targetSdk` and `compileSdk` the latest stable. `applicationId` `in.varunsuryawanshi.nougat`. The Kotlin `namespace` is `app.nougat`, because `in` is a Kotlin keyword and cannot start a package name without backticks.
- Unit tests: JUnit on the JVM for all pure logic (queue, playlists, equalizer state, listing, search, spectrum), ported from the iPhone tests.

## Layout

```
app/src/main/java/app/nougat/
  design/      tokens, accents, type, metrics, components
  library/     tracks, MediaStore reader, listing, search, artwork
  playback/    PlayQueue, the media service, Player
  playlists/
  equalizer/
  visualizer/  spectrum analyser and the six looks
  screens/
app/src/main/res/drawable/   the 14 Material icons as vector drawables
app/src/test/                JVM unit tests
```

## Commands

Fill these in for real during P0.2; until then they are the intended ones.

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb shell am start -n in.varunsuryawanshi.nougat/app.nougat.MainActivity
```

```bash
adb logcat -s Nougat
```

Test music: `adb push "<folder>" /sdcard/Music/`. A 5,000-song test library: run `Tools/make-test-library.sh "<folder>"` from the iPhone repo (needs ffmpeg, which is installed).

## Tasks

Every task: build, run, check its "Done when" yourself, then tick.

### Phase 0: set up

- [ ] **P0.1 Tools.** Ask Varun before downloading anything. Android Studio is not needed: install Google's command-line tools (Homebrew cask `android-commandlinetools`), then with `sdkmanager` an SDK platform, build tools, platform tools, the emulator and one arm64 system image. Create an emulator with `avdmanager`; it can run without a window and be driven with `adb`. JDK 17 and `adb` are already on the Mac; the Android SDK is not.
  Done when: an emulator (or Varun's phone) shows in `adb devices`.
- [ ] **P0.2 Project.** (The repository and its `origin` remote already exist.) `.gitignore` (build outputs, `local.properties`, keystores), Gradle wrapper, empty Compose app with the ids above, MIT `LICENSE` in Varun's name, as in the iPhone repo. Write the real commands above.
  Done when: `./gradlew assembleDebug` builds and the app opens on the emulator.
- [ ] **P0.3 On Varun's phone** (if he has an Android phone; open question 2).
  Done when: Varun sees the app on it.

### Phase 1: design system and shell

- [ ] **P1.1 Colours and accents.** Every fixed colour and the six accents from the iPhone `DESIGN.md` section 2, light and dark; the accent as a composition local, chosen in `SharedPreferences`, teal by default. Reference: `Nougat/DesignSystem/Accent.swift`, asset colour sets, `NougatTests/DesignSystemTests.swift` (port the contrast checks).
  Done when: the ported tests pass and a debug gallery shows every swatch in light and dark.
- [ ] **P1.2 Type, metrics, icons.** Text styles in sp, sizes in dp, the 14 icons as vector drawables. Reference: `Typography.swift`, `Metrics.swift`.
  Done when: the gallery shows every style and icon.
- [ ] **P1.3 Components.** Header page (the header is the first list item, the header colour fills behind the status bar, the bar title fades in once the header has left the screen; see iPhone decisions D24, D28, D54), rows with `more_vert` and long-press menu, play button, filled and text buttons, chip, slider (horizontal and vertical, adjustable with TalkBack), snackbar with an action, empty state, toggle icon. Reference: `Page.swift`, `Components/*.swift`, `design/preview.html`.
  Done when: the gallery matches the iPhone preview in light and dark.
- [ ] **P1.4 App shell.** Bottom navigation (Folders, Songs, Playlists) with a back stack per tab, a search entry, the mini player above the navigation bar, a Now playing screen; edge to edge; system and predictive back. Reference: `AppShell.swift`.
  Done when: the Back and Edge-to-edge rows of `DESIGN.md` section 4 pass on a device.

### Phase 2: library

- [ ] **P2.1 Audio access and reading.** Ask for `READ_MEDIA_AUDIO` (Android 13 and later) or `READ_EXTERNAL_STORAGE` (older) with an explanation first, and cope with a refusal. Read `MediaStore` into the same `Track` model: path relative to the storage root, title, artist, album, duration, size, date. Formats as on iPhone, except that Android plays OGG and Opus too; WMA shows greyed out. Reference: `Library/Track.swift`, `Library/LibraryScanner.swift`.
  Done when: music pushed to `/sdcard/Music` appears with its tags.
- [ ] **P2.2 Folders, sorting, search.** Port the folder tree from paths, sorting, "everything under a folder" and search, with their tests. Keep a title-sorted list made once per change (iPhone D60). Reference: `Track.swift` (`listing`, `search`, `under`), `LibraryTests.swift`.
  Done when: the ported tests pass.
- [ ] **P2.3 Artwork.** Embedded art, else `cover` or `folder` .jpg/.png beside the song; thumbnails cached in memory and in the cache folder; letter tile when none. Reference: `ArtworkStore.swift`.
  Done when: thumbnails show and scrolling stays smooth.
- [ ] **P2.4 Keeping up to date and removing.** Re-read when the app returns and when `MediaStore` reports changes. Removing follows open question 5.
  Done when: songs added or deleted outside the app appear or vanish without a restart.
- [ ] **P2.5 Screens.** Folders, Songs (sort menu), Search, with pull to refresh. Reference: `FolderScreen.swift`, `SongsScreen.swift`.
  Done when: all three match the iPhone screens.

### Phase 3: playback

- [ ] **P3.1 Queue.** Port `PlayQueue` exactly, as pure Kotlin, with the queue tests. Reference: `Playback/PlayQueue.swift`, `PlaybackTests.swift`.
  Done when: the ported tests pass.
- [ ] **P3.2 Player.** Media3 ExoPlayer inside a `MediaSessionService`. Nougat's `PlayQueue` decides the order (shuffle included), so behaviour matches iPhone: tapping a song plays the list it is in (D32), previous restarts after 3 s, shuffle stays on across lists, repeat cycles off / all / one, deleted songs leave the queue (D40), position saved on song change, pause and going to the background (D35). Audio focus and "becoming noisy" handled by Media3. Reference: `Player.swift`.
  Done when: play, pause, next, previous, seek, shuffle, repeat and restore after a restart work on a device.
- [ ] **P3.3 Mini player, Now playing, Queue.** Reference: `NowPlayingScreen.swift` (including the larger mini-player buttons, D45, and the shuffle and repeat toggle look, D53).
  Done when: all three match the iPhone screens.
- [ ] **P3.4 System controls.** Media notification, lock screen, Bluetooth and headphone buttons, calls, unplugging.
  Done when: Varun confirms each on his phone.

### Phase 4: playlists

- [ ] **P4.1 Store.** Port `Playlists` and `Playlist` with their tests: unique names ignoring case, a song at most once, missing songs kept but hidden (D41), an additions counter for the success haptic (D57). Reference: `Playlists/Playlists.swift`, `PlaylistTests.swift`.
  Done when: the ported tests pass.
- [ ] **P4.2 Screens.** Playlists list (create, rename, delete), playlist detail (play, shuffle, drag to reorder, swipe to remove, "Add songs" picker), "Add to playlist" from a song, a folder and Now playing, with an Undo snackbar. Compose has no built-in list reordering; write it with a long-press drag, no library. Reference: `PlaylistScreens.swift`.
  Done when: everything above works on a device and survives a restart.

### Phase 5: equalizer

- [ ] **P5.1 State.** Port `EqualizerState` and its tests: bands at 60, 230, 910, 3,600 and 14,000 Hz, ±15 dB in 0.5 dB steps, preamp ±12 dB, the presets, saved presets, "Custom", and the headroom rule (D43, D44). Reference: `Equalizer/Equalizer.swift`, `EqualizerTests.swift`.
  Done when: the ported tests pass.
- [ ] **P5.2 Sound.** Apply it to ExoPlayer's audio session: `DynamicsProcessing` on Android 9 and later (exact band frequencies, and an input gain for preamp and headroom), the platform `Equalizer` on 8.x (nearest bands).
  Done when: Varun hears each preset and the preamp on his phone.
- [ ] **P5.3 Screen.** Reference: `EqualizerScreen.swift`, including the 0 dB tick and the large-text layout (D59).
  Done when: it matches the iPhone screen and works with TalkBack.

### Phase 6: visualizer

- [ ] **P6.1 Analyser.** Take the decoded audio from ExoPlayer with Media3's `TeeAudioProcessor` (no microphone permission), only while Now playing is visible and the app is in front. Port the analyser: 1,024-sample slices, 24 bands from 50 Hz to 16 kHz, 3 dB per octave tilt, the level curve, the waveform. Reference: `Playback/Spectrum.swift`, `aToneLightsTheBarForItsPitch` in `PlaybackTests.swift`.
  Done when: the ported test passes.
- [ ] **P6.2 Six looks.** Port spectrum, LED meter, oscilloscope, ring, cassette and record to Compose `Canvas`; tap the cover to change look; strip over a cover, full area without one; nothing moves with animations removed. Reference: `Components/Visualizer.swift` (D50, D52).
  Done when: all six move with the music on a device.

### Phase 7: finish

- [ ] **P7.1 Settings.** Accent swatches, version, the Material Icons licence (text in the iPhone repo's `Nougat/Resources/Licences`). Reference: `SettingsScreen.swift`.
  Done when: changing the accent recolours the app at once and survives a restart.
- [ ] **P7.2 Contract audit.** Walk every row of `DESIGN.md` section 4 on a device and fix gaps.
  Done when: every row is recorded as passing in `HANDOFF.md`.
- [ ] **P7.3 Accessibility and dark.** TalkBack labels, values and custom actions; the largest font and display size; animations removed; dark theme.
  Done when: the whole app can be used with TalkBack alone, and every screen is reviewed in both themes.
- [ ] **P7.4 Scale check.** 5,000 songs from the test library.
  Done when: the first read does not block the interface and scrolling stays smooth.

### Phase 8: release

- [ ] **P8.1 README.** What it is, how to add music, how to build.
- [ ] **P8.2 Signed APK.** A release keystore kept outside the repository; a signed APK on GitHub Releases.
- [ ] **P8.3 Play Store (optional).** Needs Varun's Play developer account. The listing must not suggest the app is part of Android or made by Google (open question 1).
