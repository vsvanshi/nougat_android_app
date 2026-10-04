# Nougat for Android

A free, open-source music player for Android that plays the music already on your phone. Play a folder, make playlists and tune the sound with an equalizer. No account, no ads, no subscription, and nothing locked behind a paywall. It is the Android version of Nougat for iPhone, and works the same way.

## Features

- **Folders as you made them.** Your folder tree is the library. Play a whole folder, subfolders included, or shuffle it.
- **Songs and search.** Every song in one list, by name or date added, and one search across songs, artists, albums, folders and playlists.
- **Playlists.** Create, rename, reorder and delete them. Add a song from its menu, by swiping it, or from Now playing; add a whole folder at once. A song is never added twice.
- **Equalizer.** Five bands with presets (Flat, Bass boost, Rock, Vocal), a preamp, and your own saved presets.
- **Plays like a system app.** Media notification and lock-screen controls, headphone, Bluetooth and car buttons, background playback, pausing when headphones are unplugged, pausing for calls. It remembers where you were.
- **A visualizer on Now playing** that follows the music, in six looks: spectrum, LED meter, oscilloscope, ring, cassette and record. Tap the cover to switch.
- **Your look.** Light and dark theme, six accent colours, and on Android 12 and later a seventh that follows your wallpaper.
- **Nothing is deleted.** Hiding a folder or song only stops Nougat showing it; Settings shows hidden ones again.

**Formats:** MP3, M4A and AAC, FLAC, OGG, Opus, WAV and more. WMA files are listed but greyed out, because Android cannot play them.

## Adding music

Nougat plays the music in your phone's media library, so there is nothing to import. Copy music onto the phone the usual way, for example:

- **From a computer:** connect the phone with a USB cable, choose **File transfer** on the phone, and copy folders into its **Music** folder.
- **From an SD card or downloads:** any music Android finds appears in Nougat too.

New songs appear on their own; pull down on a list to look again. The first time, Nougat asks for permission to see your audio files.

## Getting it

Download the APK from the repository's Releases page and open it on the phone. Android will ask you to allow installing apps from your browser or file manager. Android 8.0 or later is needed.

## Building

You need JDK 17 and the Android SDK (the command-line tools are enough: platform 37, build tools and platform tools). There are no dependencies outside AndroidX and Media3.

1. Clone the repository and create `local.properties` with `sdk.dir=/path/to/your/Android/sdk`.
2. Build and install a debug build on a connected phone:

```bash
./gradlew assembleDebug
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To run the unit tests:

```bash
./gradlew testDebugUnitTest
```

A release build is signed with a keystore described in `~/.nougat-release/keystore.properties` (kept outside the repository); without it, the release build is signed with the debug key.

## In this repository

| Path | What |
|---|---|
| `app/src/main/java/app/nougat/` | The app, in Kotlin and Jetpack Compose |
| `app/src/test/` | Unit tests |
| `DESIGN.md` | How the Android design differs from the iPhone one, and the native-behaviour rules |
| `PLAN.md` | The build plan, task by task |
| `HANDOFF.md` | Where the work stands, and the decisions behind it |
| `AGENTS.md` | Working rules for the coding agents that help build Nougat |

## Licence

Nougat is released under the [MIT licence](LICENSE). It includes Material Icons, Android Jetpack (AndroidX, Compose, Media3), Guava and Kotlin, all under the Apache License 2.0; the licence text is in the app under Settings, and in `app/src/main/res/raw/apache_2_0.txt`.
