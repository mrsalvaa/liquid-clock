# Liquid Clock

**English** | [Español](README.es.md)

Shows up on your phone as **Clock** (or *Reloj*, depending on the language).

A free, tracker-free clock app for Android with an Apple *Liquid Glass*-style interface: glass surfaces that refract what's behind them like a lens (thanks to [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)), with glowing edges and buttons that squish like a drop when pressed.

Inspired by Samsung's clock app, built for personal use and released under GPL-3.0.

> The in-app interface is currently in Spanish only.

## Features

**Alarms**
- Repeat on selected days or ring once, with a label.
- System ringtone, silent or vibration.
- Gradually increasing volume (15 s to 2 min).
- Configurable snooze (interval and maximum count).
- **Pick the snooze length while the alarm is ringing**: quick chips (5/10/15/30) and fine-tuning with −/+ from 1 to 120 min.
- Volume buttons snooze (can be turned off).
- Skip only the next occurrence of a repeating alarm.
- Alarm groups: collapsible, with a switch that turns all their alarms on or off at once.
- Turn a repeating alarm off just for today: it comes back on by itself tomorrow.
- A notification 2 hours before each alarm lets you dismiss that one occurrence (can be turned off).
- Auto-silence after N minutes (snoozes if possible).
- Full-screen alarm over the lock screen.

**World clock**: glass analog clock, city search by time zone, time difference and relative day. Long-press a city to remove it.

**Stopwatch**: laps, with the fastest and slowest highlighted. Keeps running even if you close the app.

**Timer**: several at once, saveable presets, pause, +1 min and a countdown notification.

Long-press an alarm, timer or city to see its options (edit, duplicate, skip, delete...).

Swipe left or right to change tabs. Tap the number in any wheel to type it with the keyboard.

**Appearance**: system, light or dark theme; moving colour backgrounds or a single accent colour (10 colours, 3 tones each); and alternative app icons in Material, Samsung, iPhone and Motorola styles.

**Bedtime**: a reminder to go to bed, showing how much sleep you'll get before your next alarm.

## Download

Get the APK from [Releases](../../releases) and install it (Android 12 or newer).

Check that the file hasn't been tampered with:

```bash
sha256sum -c liquid-clock-v0.1.0.apk.sha256
```

And that it's signed with the project key (SHA-256 certificate fingerprint):

```
D3:09:86:98:9C:2F:A7:E2:EA:C4:44:E1:B3:1F:0B:48:18:C8:EA:57:FE:21:9A:BD:D6:A3:BF:61:1F:86:BE:29
```

```bash
apksigner verify --print-certs liquid-clock-v0.1.0.apk
```

On first launch, grant the permissions it asks for, especially **full screen**:
without it the alarm won't turn on the screen of a locked phone.

## Build

Requirements: JDK 17 and the Android SDK (platform 35 and build-tools).

On Arch / CachyOS:

```bash
sudo pacman -S --needed jdk17-openjdk
yay -S --needed android-sdk-cmdline-tools-latest android-sdk-platform-tools android-sdk-build-tools android-platform
```

Point Gradle to the SDK (with the AUR packages it's usually `/opt/android-sdk`):

```bash
echo "sdk.dir=/opt/android-sdk" > local.properties
```

Build and install:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

The debug build installs as `dev.glassclock.debug`, so it can live alongside the release.

`./gradlew assembleRelease` produces a minified APK. It's signed with the key described in
`~/.android-keys/liquid-clock.properties` (or the path given by `-PsigningProps=...` or the
`LIQUID_CLOCK_SIGNING` environment variable); if that file doesn't exist, the debug key is used:

```properties
storeFile=/path/to/key.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Permissions

| Permission | What for |
|---|---|
| `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` | Ring at the exact time. |
| `USE_FULL_SCREEN_INTENT` | Show the alarm over the lock screen. |
| `POST_NOTIFICATIONS` | Alarms, timers and reminders. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Play the ringtone while ringing. |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after a reboot. |
| `VIBRATE`, `WAKE_LOCK` | Vibrate and keep the phone awake while ringing. |
| `SYSTEM_ALERT_WINDOW` | Optional, to open the alarm even while you're using the phone. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional, for manufacturers that kill background apps. |

The app doesn't request internet access.

## Project structure

```
app/src/main/java/dev/glassclock/
├── data/       Models and JSON storage
├── alarm/      Alarm scheduling, snooze, skip, reboot
├── ring/       Ringing service and alarm screen
├── timer/      Timers
├── bedtime/    Bedtime reminder
├── ui/glass/   Liquid glass components (surface, buttons, wheel, bar...)
└── ui/screens/ Screens
```

## License

[GPL-3.0](LICENSE)
