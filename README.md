# Quick APK build with GitHub Actions

You do **not** need Android Studio.

1. Create a new empty GitHub repository.
2. Upload the **contents of this `F12FanIRTester` folder** to the repository root.
   The repository root should directly contain `app`, `.github`, `build.gradle`, and `settings.gradle`.
3. Commit/upload the files to `main`.
4. Open the repository's **Actions** tab.
5. Open **Build Android APK**.
6. If it did not start automatically, choose **Run workflow**.
7. Wait for the green checkmark.
8. Open the completed run and download the artifact **F12FanIRTester-debug**.
9. Unzip that artifact to obtain `app-debug.apk`.
10. Copy `app-debug.apk` to the Xiaomi K50 and install it.

The workflow uses Java 17, Gradle 8.9, Android SDK 35, and builds `assembleDebug`.

---

# F12 Fan IR Tester

A tiny Android app for manually scanning F12 infrared function codes.

Default scan target:

- Protocol: F12
- Carrier: 37.9 kHz
- Device: 3
- Subdevice: 1
- Function: 0–255

The UI lets you:

- Send one function at a time
- Move Previous / Next or ±16
- Jump to decimal or hex function numbers
- Mark `NO RESPONSE`, `POWER`, `SPEED +`, `SPEED -`, `OSCILLATE`, `TIMER`, `LIGHT`, or `OTHER`
- Add an optional note
- Automatically advance after marking
- Keep results persistently on the phone
- Copy the complete scan as CSV
- Change F12 Device (0–7) and Subdevice (0–1)

## Why this is a native Android app

Browsers do not expose Android's ConsumerIrManager API. Native Android code does.

The app calls:

```java
ConsumerIrManager.transmit(carrierFrequency, pattern)
```

with an alternating mark/space pattern in microseconds.

## F12 encoding used

From the IrpTransmogrifier protocol definition:

```text
{37.9k,422}<1,-3|3,-1>((D:3,S:1,F:8,-80)2)*
```

Implemented as:

- carrier 37,900 Hz
- time unit T = 422 µs
- zero bit = 422 µs mark + 1266 µs space
- one bit = 1266 µs mark + 422 µs space
- D = 3 bits, LSB first
- S = 1 bit
- F = 8 bits, LSB first
- 80T leadout
- frame transmitted twice

## Build

Open this directory in current Android Studio.

If Android Studio asks to install Android SDK 35, allow it.

Then:

1. Build > Build APK(s)
2. Copy/install the debug APK onto the Xiaomi phone
3. Android may require enabling installation from unknown sources for the file manager/browser used to install it

No network permission and no storage permission are requested.

## Suggested testing

Start with D=3, S=1.

Useful Airmate IRDB function values to test first:

- 9 / 0x09
- 17 / 0x11
- 33 / 0x21
- 65 / 0x41
- 99 / 0x63
- 129 / 0x81
- 195 / 0xC3

Then scan 0–255 manually.

Keep the fan where you can see it clearly. When a Power code turns it off, turn it back on before continuing.

## Important

The Airmate LBT-F01 profile used by the phone app may not correspond exactly to the public Airmate IRDB profile. D=3/S=1 is therefore a strong hypothesis, not yet proven. The app allows D and S to be changed if necessary.
