# Changelog

**English** | [Español](CHANGELOG.es.md)

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
