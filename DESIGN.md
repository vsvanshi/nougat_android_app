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
| Tabs | System tab bar with a search tab | Material bottom navigation bar on `surface`: Folders (`folder`), Songs (`music_note`), Playlists (`queue_music`). Selected is `accentText`, the rest `ink2`, with no indicator pill (the 2016 look). Each tab keeps its own back stack; tapping the tab that is showing goes back to its root. Search is an icon in the top app bar of each tab (decision A11) and opens on the current tab's stack with the keyboard up |
| Moving between pages | System push | A new page slides in from the right over the old one, which drifts a quarter of the way left (300 ms, standard curve); going back is the reverse. A folder's name flies from its row up into the new page's large title, growing as it goes, and back down on the way out (shared element). Drag a pushed page to the right from anywhere on it to go back: it follows the finger and can be let go halfway (more than a third of the width, or a fling, completes it). The drag is fed to the system's back events, so it is the same animation as the predictive back gesture |
| Mini player | System bottom-accessory slot | Our own 64 dp bar above the bottom navigation, shown once there is a song (Android players hide it before that; iPhone shows "Nothing playing"), `surface` with depth 1, same contents; play/pause and next are 34 and 30 dp glyphs in 56 dp wide tap areas (D45, which Android can follow fully because the bar is ours); a 2 dp progress line along its top edge, `accentText` on `hairline`, moving twice a second while playing (Varun, 2026-10-04); tap opens Now playing |
| Snackbar | Our own overlay with a 4 s timer | Material's `SnackbarHost` drawn in Nougat's look (`NoticeHost`): it gives the 4 s timeout, a longer one when TalkBack needs time to reach the action, and the announcement |
| Now playing | Full-screen cover with zoom transition | Full screen over the tabs, sliding up (225 ms) and down (195 ms). System back closes it; with gesture navigation the predictive back gesture shrinks it to 90% with rounded corners as it is dragged. The `expand_more` button and a swipe down on the cover (160 dp or a fast fling) also close it |
| Queue | Pushed from Now playing; Edit button for drag and delete | Slides in over Now playing from its `queue_music` button. Tap to jump, long press and drag to reorder, swipe sideways to remove; no row menu (a long press starts the drag). TalkBack gets Remove, Move up and Move down as actions |
| Add to playlist | Sheet with medium and large detents | Material modal bottom sheet: "New playlist", then the playlists; the snackbar says what was added, with Undo, and the success haptic plays (D57). Opened from a song's or folder's menu, a swipe on a song row, Now playing's `playlist_add` button and a folder's "Add all to playlist" |
| Playlist screen | Edit button for drag and delete | Long press and drag to reorder (no menu on long press; `more_vert` still has it), swipe to remove with Undo, "Add songs" opens a full-screen picker with search |
| Equalizer | Pushed inside Now playing | Slides in over Now playing from the shortcut at its bottom; a saved preset's chip offers "Delete preset" on long press. Sound: decision A20 |
| Visualizer source | Taps the audio after the equalizer | Taps decoded audio before the platform's effects, so the bars do not show the equalizer's change, and may run a little ahead of the sound (decision A21) |
| Menus, dialogs, sheets | System iOS menus and alerts | Material dropdown menus, alert dialogs and modal bottom sheets |
| Long press | System context menu | A dropdown menu at the row with the same actions as `more_vert` |
| Swipe actions | System swipe actions | Swipe a song row to reveal "Add to playlist", or "Remove" in a playlist |
| Haptics | `sensoryFeedback` | `HapticFeedbackConstants`: `CLOCK_TICK` for the EQ 0 dB tick, `CONFIRM` for adding to a playlist (Android 11 and later; `VIRTUAL_KEY` before) |
| Toggle switch | System switch tinted `switchOn` | Material switch tinted `switchOn` |
| Accent | Six accents | Seven: the six plus "System", the wallpaper colour from Material You (the primary of `dynamicLightColorScheme` / `dynamicDarkColorScheme`) on Android 12 and later, not offered on older versions (decision A14). Taken from the system tonal palette (`system_accent1_*`) by tone, not from the Material colour roles, because the role `primary` (tone 40) is too dark for the play button on the header. Light: accent tone 60, accentText and accentFill tone 40, onAccent tone 100, switchOn tone 50. Dark: accent, text and fill tone 80, onAccent tone 20, switchOn tone 60. accentOnInverse follows the same rule as the six. These tones meet every contrast rule at any hue; the unit tests check it |
| Removing | "Remove" deletes the app's copy | "Hide" in a folder's or song's menu: Nougat stops showing it, with Undo in the snackbar; no file is deleted. The Hidden screen (Folders menu now, Settings from P7.1) shows them again with a tap (decision A10) |
| Top of Folders | The Documents folder | The deepest folder that holds every song (usually "Music"), titled "Music" (decision A17) |
| Cover art beside a song | `cover` or `folder` .jpg/.png | Embedded art first. Beside the song: `cover`/`folder` .jpg/.png on Android 8 and 9; from Android 10 only what the system's media library supplies, which is `AlbumArt*.jpg` (decision A18) |
| Pull to refresh | System refresh control | Material pull-to-refresh, the indicator just below the top bar, `accentText` on `surface` |

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
