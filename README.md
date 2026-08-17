# F12 Fan IR Tester

A small native Android app for testing fan infrared signals with a phone IR blaster.

This repository is a protocol-discovery tool, not an everyday remote. For the
finished five-button Angelcook `AGC-WL2200WH` remote and reusable DIY codes, see
[Angelicook Fan Remote](https://github.com/mahlernim/Angelicook-Fan-Remote).

## Airmate power-code scan

The dedicated power scanner contains 21 deduplicated candidates:

- 19 recorded raw Airmate power waveforms
- one decoded Airmate NEC power command
- one independently published generic fan NEC power command

The Airmate candidates come from 25 profiles in the MIT-licensed
[Flipper Devices IRDB](https://github.com/flipperdevices/IRDB). Duplicate
waveforms are transmitted only once. Each candidate retains its recorded carrier
frequency and complete raw timing pattern.

Press **SEND POWER** for the displayed candidate and then choose:

- **NO EFFECT** to mark it, advance, and immediately send the next candidate
- **POWER CHANGED** to save the match and stop automatic transmission

The scanner remembers its position and tested results. Previous and next buttons
allow any candidate to be repeated. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
for attribution.

## Confirmed NEC command scan

Candidate 21 successfully controlled the target fan, confirming:

- protocol: NEC
- address: `0x00`
- power command: `0x45`
- legacy display: `0x00FFA25D`

The dedicated NEC scanner transmits commands `0x00` through `0xFF` at address
`0x00`. It supports decimal/hex jumping, previous/next and +/-16 navigation,
response labels, notes, persistent results, and NEC-specific CSV export.

With both automatic options enabled, mark each observed result once and the app
immediately advances and transmits the next NEC command. Command `0x45` is visibly
identified as the confirmed power command.

## F12 comparison scan

The original function scanner remains available for comparing three F12 waveforms:

- **F12-1 (default):** four frames with 34T, 88T, and 34T inter-frame gaps
- **F12-0:** two frames with a 34T inter-frame gap
- **Legacy F12:** the earlier two-frame implementation with an 80T leadout after each frame

All modes use a 37.9 kHz carrier, T = 422 microseconds, and 12 LSB-first data bits: D (3), H/S (1), and F (8). Device D (0–7), H/S (0–1), and function F (0–255) remain selectable.

The app requires Android's consumer IR API. It requests no Internet or storage permissions.

## Fast Airmate test

Start with **F12-1, D=3, H/S=1**. Seven prominent quick-test buttons jump to and immediately transmit these published Airmate function values:

- 9 / 0x09
- 17 / 0x11
- 33 / 0x21
- 65 / 0x41
- 99 / 0x63
- 129 / 0x81
- 195 / 0xC3

After each transmission, mark the observed response. Results are stored separately for every waveform mode, D, H/S, and function combination. Existing results made with the old app remain visible under **Legacy F12**. `COPY CSV` exports the selected mode and includes the mode name in every row.

For a one-tap-per-code scan, leave **Automatically advance after marking** enabled and turn on **Send next code immediately after marking**. Each response button then saves the current result, advances one function, and transmits the next code.

The full 0–255 scan, decimal/hex jump, previous/next and +/-16 navigation, notes, automatic advance, and response marking remain available.

## Build with GitHub Actions

The `Build Android APK` workflow runs unit tests, builds the debug APK, and uploads the artifact `F12FanIRTester-debug`. It uses Java 17, Gradle 8.9, Android SDK 35, and Android Gradle Plugin 8.7.3.

You can also open the repository in Android Studio and choose **Build > Build APK(s)**.

## Protocol source

The waveform definitions follow HARCToolbox's current `IrpProtocols.xml` entries:

```text
F12-0: (D:3,H:1,F:8,-34,D:3,H:1,F:8) {H=0}
F12-1: (D:3,H:1,F:8,-34,D:3,H:1,F:8,-88,D:3,H:1,F:8,-34,D:3,H:1,F:8)* {H=1}
Legacy F12: ((D:3,S:1,F:8,-80)2)*
```

The app intentionally leaves H/S selectable in every waveform mode for controlled comparison, even though the formal F12-0 and F12-1 definitions constrain H to 0 and 1 respectively.

The public Airmate profile is a useful lead, not proof that a specific fan model uses this protocol.
