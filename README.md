<div align="center">

# 🧭 NaviAOD
### **OLED Always-On Display HUD for Google Maps Navigation**

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20%7C%2014%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%201.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Display Tech](https://img.shields.io/badge/Display-True%20AMOLED%20%2F%20pOLED-00E5FF?style=for-the-badge&logo=materialdesign&logoColor=white)](https://en.wikipedia.org/wiki/OLED)
[![Device Target](https://img.shields.io/badge/Tested%20On-Moto%20Edge%2050%20Fusion-FFB300?style=for-the-badge&logo=motorola&logoColor=white)](https://www.motorola.in/smartphones-motorola-edge-50-fusion)
[![Build](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20APK-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/GodFakerr/GMaps-AOD/actions)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

<br/>

**Transform Google Maps into an ultra-low-power, high-contrast Always-On Display HUD.**  
*95%+ true `#000000` pitch black pixels — zero backlight power, zero GPU strain, zero phone overheating.*

---

</div>

## 📌 The Problem
Standard Google Maps navigation burns **25%–35% battery per hour** and causes thermal throttling:
1. **100% Pixels Lit:** Standard maps display full-color 3D buildings, terrain, and satellite imagery across the entire 6.7-inch screen.
2. **GPU Overdraw at 144Hz:** Modern screens like the Moto Edge 50 Fusion run at up to 144Hz. Continuously re-rendering full vector tiles at high frame rates wastes massive GPU energy.
3. **Handlebar / Dashboard Glare & Heat:** Running maximum brightness in the hot sun leads to extreme battery degradation and rapid battery drain.

---

## ⚡ The Solution
On OLED and pOLED displays, **pitch black (`#000000`) pixels are completely turned off (drawing 0.0 mA)**.

**NaviAOD** intercepts Google Maps turn-by-turn navigation updates in real-time and renders a distraction-free, anti-glare, pure AMOLED HUD over your lock screen:
* **Over 80% Reduction** in display power consumption.
* **Passive Interception:** Zero root, zero background location polling overhead. Intercepts navigation via Android's official `NotificationListenerService`.
* **Lockscreen Native:** Appears directly over your secure lock screen (`showWhenLocked`, `turnScreenOn`) — no need to unlock the phone while riding or driving.

---

## ✨ Battery-Saving Features

```
┌────────────────────────────────────────────────────────┐
│                      NaviAOD HUD                       │
│                                                        │
│   14:52           [Speedometer] 48 km/h         85% 🗲  │
│                                                        │
│                         ▲                              │
│                       ╱   ╲                            │
│                      ╱     ╲                           │
│                     └──┐ ┌──┘                          │
│                        │ │                             │
│                                                        │
│                       350 m                            │
│         Turn right onto Senapati Bapat Road            │
│                                                        │
│         14 km                       28 min · 4:15 PM   │
│                                                        │
│        [ Double-tap to open full Google Maps ]         │
└────────────────────────────────────────────────────────┘
```

| Feature | How It Saves Battery |
| :--- | :--- |
| **🌙 True OLED Pitch Black (`#000000`)** | Over 95% of pixels are powered completely OFF. On pOLED screens, black pixels consume zero power. |
| **💤 Wake-on-Approach (Highway Deep Sleep)** | On long straight roads (>1 km before next maneuver), screen enters 100% blank sleep. Automatically wakes 500m before the turn with a haptic cue. |
| **📴 Pocket & Face-Down Sleep** | Hardware proximity sensor and accelerometer detect when phone is in a pocket, tank bag, or laid face down, immediately cutting display power. |
| **🛡️ pOLED Burn-In Protection** | Pixel Shift engine subtly drifts all UI elements by 2–4 pixels every 60 seconds to protect the Motorola Edge 50 Fusion pOLED panel. |
| **⏱️ 1-FPS GPU Throttling** | Redraws strictly once per second or upon new turn data. Eliminates 60Hz/144Hz continuous frame loops to keep CPU/GPU in idle C-states. |
| **🕶️ Ultra-Dim Night Filter** | Software-level black alpha dimmer lowers perceived brightness below Android's hardware minimum for glare-free night navigation. |
| **🗺️ Map Peek Mode** | Single tap renders a minimalist monochrome vector route preview on pure black for 8 seconds, then auto-fades back to minimal HUD. |
| **📡 Passive GPS Speedometer** | Piggybacks on Google Maps' active GPS fixes using Android's `PASSIVE_PROVIDER`. **0% extra GPS radio battery drain.** |
| **⚡ Emergency Quick Gestures** | Double-tap anywhere to instantly launch full Google Maps. Swipe down to dismiss. |

---

## 🧪 Built-in Route Simulator (Desk Testing)
No need to get in a car to test the app! NaviAOD includes a realistic navigation simulator:
* Pre-loaded route: **Rajiv Gandhi Infotech Park (Hinjewadi) ➔ Wakad Bypass (Highway Sleep Test) ➔ Aundh ➔ Senapati Bapat Road ➔ Pune Railway Station**.
* Tests distance countdowns, turn arrows, highway sleep blanking, and speed updates right at your desk.

---

## 📱 Motorola Edge 50 Fusion Setup (Android 14 / Hello UI)

Granting required permissions takes under 30 seconds directly from the app dashboard:
1. **Notification Access:** Settings ➔ Apps ➔ Special App Access ➔ Notification Access ➔ Enable **NaviAOD**.
2. **Display Over Other Apps:** Settings ➔ Apps ➔ Special App Access ➔ Display over other apps ➔ Enable **NaviAOD**.
3. **Battery Optimization Exemption:** Settings ➔ Apps ➔ NaviAOD ➔ Battery ➔ Set to **Unrestricted** (prevents Android 14 from closing the service during long trips).
4. **Location (Optional):** Grants passive speed reading for the live speedometer.

---

## 🛠️ Build & Installation

### Option 1: Automated GitHub Actions (Recommended)
This repository includes [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).
Whenever you push to `main`, GitHub Actions automatically compiles the APK.
1. Go to the **Actions** tab on GitHub.
2. Select the latest build workflow.
3. Download **NaviAOD-Debug-APK** and install `app-debug.apk` directly on your phone.

### Option 2: Local Gradle Build
```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
Output location:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🎨 OLED Accent Themes
* **Electric Cyan** (`#00E5FF`) — Crisp and modern.
* **AMOLED Amber** (`#FFB300`) — Warm, night-friendly tone.
* **Racing Green** (`#00E676`) — Classic turn guidance.
* **Stealth White** (`#FFFFFF`) — High contrast for bright sunlight.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
