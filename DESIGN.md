# Nougat for Android: design

The iPhone version's `DESIGN.md` (`/Users/varun/work/NougatMobileApp/DESIGN.md`) is the design system. This file says only what Android does differently. Where this file is silent, the iPhone rule applies.

## 1. The idea

On iPhone the rule was "iOS draws the chrome, Material fills the page". On Android there is nothing to mix: **the whole app is Material, in the 2016 spirit** that the iPhone version recalls. Blue-grey header, Roboto, one accent colour, a round play button sitting on the header's bottom edge, 72 dp rows, a snackbar. The point is a player that feels like the simple Android players of about 2016, built with today's Android so every system gesture and control works.

## 2. Shared exactly (from the iPhone `DESIGN.md`)

- **Colour** (section 2): every fixed token (`background`, `paper`, `surface`, `fill`, `hairline`, `ink`, `ink2`, `ink3`, `header`, `onHeader`, `onHeader2`, `avatar`, `onAvatar`, `inverse`, `onInverse`) with its light and dark values, and all six accents (teal default, amber, orange, pink, indigo, blue) with their contrast rules. Port the hex values exactly; the iPhone tests check them. Android adds a seventh, "System" (see section 3).
- **Type** (section 3): the same styles and sizes, in sp, so they follow the system font size.
- **Layout** (section 4): the same sizes, in dp (8 grid, 16 margin, 72 keyline, 72 and 56 rows, 48 touch target, 56 header play button, 64 Now playing play button).
- **Shape, depth, motion** (section 5): the same radii, the two shadow levels, the 300 / 225 / 195 ms motion with the standard curve, and the fade under reduced motion.
- **Icons** (section 6, page side): the same 14 Material icons. Copy them from `Nougat/Resources/Assets.xcassets/Icons/*.imageset/*.svg` in the iPhone repo and import them as vector drawables. On Android they are used everywhere, including the bars, so there are no SF Symbols to replace. The bars need a few more than the 14 (for example `search`, `arrow_back`, `music_note`, `queue_music`, `expand_more`, `edit`, `close`): take them from the same Material Icons filled set, as vector drawables named `ic_<name>`, when the screen that uses them is built.
- **Components** (section 7): header, rows, letter tile, play button, filled and text buttons, chips, the slider, snackbar, empty state, toggle icon, and the visualizer with its six looks.
- **Screens** (section 8): the same screens with the same contents.

## 3. Different on Android

| Area | iPhone | Android |
|---|---|---|
| Font | Roboto bundled | Roboto is the system font: nothing to bundle, no font licence to show |
| Top bar | System glass bar over the header | A Material top app bar (64 dp) over the header, filled with the `header` colour so the large title slides under it and never shows behind the clock (the reason for iPhone D54). The header's large title starts 8 dp below the bar. When the dark block's bottom edge reaches the bottom of the bar, the bar turns `paper` with `ink` icons, the Bar title fades in (300 ms) and the status-bar icons follow the theme; before that they are light. One switch is enough on Android because the bar is opaque. Edge to edge, `header` colour behind the status bar. Android's stretch overscroll leaves no gap above the header, so there is no backdrop as on iPhone (D28) |
| Tabs | System tab bar with a search tab | Material bottom navigation bar: Folders, Songs, Playlists. Search is an icon in the top app bar of each tab (decision A11) |
| Mini player | System bottom-accessory slot | Our own bar above the bottom navigation, `surface` with depth 1, same contents; tap opens Now playing |
| Snackbar | Our own overlay with a 4 s timer | Material's `SnackbarHost` drawn in Nougat's look (`NoticeHost`): it gives the 4 s timeout, a longer one when TalkBack needs time to reach the action, and the announcement |
| Now playing | Full-screen cover with zoom transition | Full-screen screen; system back and predictive back close it, and so does a swipe down |
| Menus, dialogs, sheets | System iOS menus and alerts | Material dropdown menus, alert dialogs and modal bottom sheets |
| Long press | System context menu | A dropdown menu at the row with the same actions as `more_vert` |
| Swipe actions | System swipe actions | Swipe a song row to reveal "Add to playlist", or "Remove" in a playlist |
| Haptics | `sensoryFeedback` | `HapticFeedbackConstants`: `CLOCK_TICK` for the EQ 0 dB tick, `CONFIRM` for adding to a playlist (Android 11 and later; `VIRTUAL_KEY` before) |
| Toggle switch | System switch tinted `switchOn` | Material switch tinted `switchOn` |
| Accent | Six accents | Seven: the six plus "System", the wallpaper colour from Material You (the primary of `dynamicLightColorScheme` / `dynamicDarkColorScheme`) on Android 12 and later, not offered on older versions (decision A14). Taken from the system tonal palette (`system_accent1_*`) by tone, not from the Material colour roles, because the role `primary` (tone 40) is too dark for the play button on the header. Light: accent tone 60, accentText and accentFill tone 40, onAccent tone 100, switchOn tone 50. Dark: accent, text and fill tone 80, onAccent tone 20, switchOn tone 60. accentOnInverse follows the same rule as the six. These tones meet every contrast rule at any hue; the unit tests check it |
| Removing | "Remove" deletes the app's copy | "Hide": Nougat stops showing the folder or song; no file is deleted. Settings lists hidden items to show again (decision A10) |

## 4. Native contract on Android

Every item is a requirement, checked on a real device.

| Behaviour | Requirement |
|---|---|
| Back | System back and predictive back work everywhere; back from a tab's root leaves the app |
| Edge to edge | Content draws behind the status and navigation bars; the header colour reaches the top of the screen |
| Media notification and lock screen | Title, artist, artwork, play/pause, next, previous and seeking, through a `MediaSession` |
| Headphone, Bluetooth and car buttons | Play, pause, next, previous |
| Audio focus | Pause for calls and other apps' audio, resume when the system says; lower the volume for short sounds |
| Becoming noisy | Pause when headphones are unplugged or Bluetooth disconnects |
| Background playback | Continues with the screen off, as a foreground media service |
| Permissions | Ask for audio access the first time, explain why, and work with "not now" |
| Appearance | Follows the system light and dark theme |
| Font size and display size | Up to the largest without clipped or overlapping text |
| TalkBack | Every control has a label; sliders are adjustable; long-press actions are also custom actions |
| Remove animations | Our animations become a short fade |
