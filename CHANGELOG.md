# Changelog

**English** | [Español](CHANGELOG.es.md)

## 0.5.1

### Fixes
- On the plain background in light theme, the tab bar now shows the liquid glass effect, as it does in dark theme.
- Glass and controls updated to match the rest of the suite: the tab bar can also be dragged with a finger, and animations follow the system setting to remove them.

## 0.5.0

### New
- Interface style: besides Liquid Glass (the default, with all its usual options) there is "Solid", with opaque cards on a plain background and no glass effects. It is plainer: less rounded corners, rectangular buttons, outline icons and a docked bottom bar. It has light or dark theme and its own accent colour.

## 0.4.5

### Fixed
- A time typed on the alarm or timer wheels was only applied after confirming on the keyboard. It now applies while you type, so tapping save keeps it.
- After scrolling a wheel, the preview below ("Sonará en…") took a moment to update, and saving before it did kept the old time. The value now updates as soon as the centre number changes.
- On the light theme, cards showed a lighter rectangle inside them that disappeared while pressed. The card shadow showed through the translucent fill; it is now drawn only outside the card.
- On the light theme, the selection band on the wheels no longer looks like a white box.

## 0.4.1

### New
- *Plain* background, now the default: a flat background with a blue accent, lighter on the dark theme and stronger on the light one. The *Moving colours* and *Accent colour* backgrounds are still available.
- The Samsung-style icon is now the app's default icon. Notifications show it too, together with the theme's blue.

### Improved
- Much smoother with the *Plain* and *Accent colour* backgrounds. Over a flat or almost flat background the glass effects look the same without refraction or blur, so cards, buttons and chips skip them. The bottom bar and menus still refract the content below. On a Galaxy S24 FE, the slowest frame when switching tabs went from 40 ms to 15 ms.
- On the light theme, blue buttons and selected chips are more opaque, so their text is easier to read.

### Fixed
- Swiping between tabs could stop working after pushing past the first tab (Alarms), the same way it happened on the last one.

### For developers
- New `profile` build type: optimized like the release, but installs as `dev.glassclock.debug` to test with real performance.

## 0.2.4

### Fixed
- Swiping between tabs could stop working for good after pushing past the last tab (World clock) right after a fast swipe. Tabs now use their own gesture handling, and pushing against the edge no longer bounces.

### Changed
- Dates are shown as day/month/year, for example "Thursday 15/10/2026" in the world clock and "Fri 17/10" in the alarm list.
- "Turn off until tomorrow" is now called "Turn off just for today" ("Desactivar solo por hoy").

## 0.2.3

### Improved
- Accent colours are toned down: the same hues, less saturated, and a softer glow in the background.
- Icons, text and lines in the accent colour are adjusted so they stay readable. A dark tone on the dark theme, or a light tone on the light theme, no longer blends into the background.
- Screen titles have a small top margin so they don't sit right under the status bar.

### Changed
- The alarms header is back to its previous layout: "Próxima en 8 h 54 min · hoy a las 23:30" next to the buttons.

## 0.2.1

### New
- Alarm groups. Each group can be collapsed and has its own switch, which turns all its alarms on or off at once; each alarm keeps its own switch. Alarms can be moved between groups, and a group can be deleted with or without its alarms.
- Turn a repeating alarm off just for today: it comes back on by itself tomorrow.
- A notification 2 hours before each alarm lets you dismiss that one occurrence. It can be turned off in Settings.
- Swipe left or right to change tabs. The bubble in the bottom bar follows your finger.
- Tap the number in any wheel to type it with the keyboard.
- Appearance settings:
  - System, light or dark theme.
  - Moving colours or a single accent colour for the background. There are 10 accent colours with 3 tones each.
  - Alternative app icons in Material, Samsung, iPhone and Motorola styles.
- The keyboard's Done button confirms dialogs.

### Improved
- Smoother at 120 Hz. The animated background no longer redraws the glass on every frame (about 16 frames per second when idle instead of 120), and tabs switch instantly without rebuilding the screen.
- The alarms header now reads "Próxima alarma en 8 hs 54 min ·" with the day and time on the line below.

### Fixed
- The ":" in the timer wheels sat lower than the numbers.
- Fast consecutive swipes between tabs were blocked for a moment.
- When a swipe ended, the bottom bar bubble jumped back before moving to the new tab.
- After switching theme, glass surfaces could keep the previous theme's tint.

## 0.1.0

First release: alarms with adjustable snooze while ringing, stopwatch, timers, world clock, bedtime reminder and the Liquid Glass interface.
