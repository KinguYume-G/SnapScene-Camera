<div align="center">

<img src="app/src/main/res/drawable/apu_logo.png" alt="APU Logo" width="120"/>

# SnapScene Camera

### APU Virtual Campus Guide & AI Photo Studio

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-blue?style=flat-square)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-green?style=flat-square)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

> A dual-purpose Android application combining an **AI-powered photo background replacement studio** with an **APU smart campus virtual guide**, featuring real-time face detection, ML Kit selfie segmentation, and a 3-language digital human assistant.

</div>

---

## 📱 App Overview

SnapScene Camera serves two core functions within a single elegant app:

1. **🤖 APU Virtual Campus Guide** — An interactive digital human assistant (with TTS voice) that greets visitors and guides them through APU's campus features.
2. **📸 AI Photo Studio** — A full-featured camera with real-time face detection, intelligent background replacement (30+ world landmark backgrounds), photo editing, and multi-format export.

---

## ✨ Key Features

### 🎭 Digital Human Guide (`NewMainActivity`)
| Feature | Description |
|---------|-------------|
| Digital Human Avatar | Animated avatar with breathing + blinking effects (`DigitalHumanAnimationView`) |
| Text-to-Speech (TTS) | Automatic welcome greetings in 3 languages |
| Multilingual Support | 🇨🇳 Chinese / 🇬🇧 English / 🇲🇾 Malay (runtime switch) |
| Live Clock & Date | Real-time clock display updated every second |
| Weather Display | Current temperature & humidity info |
| Navigation Buttons | Quick access to AI Camera, Campus Map, and Visit Booking |

### 📸 AI Camera (`CameraActivity`)
| Feature | Description |
|---------|-------------|
| CameraX Preview | Full-screen camera preview with PreviewView |
| Real-Time Face Detection | ML Kit Face Detection with auto-focus on detected faces |
| Face Overlay | Green bounding box overlay (supports front/rear camera mirroring) |
| Flash Control | Off / On / Auto cycle |
| Front/Rear Switch | One-tap camera toggle |
| Gallery Import | Pick existing photos from device gallery |
| Photo Resolution | Configurable via Settings (High / Medium / Low) |
| Guide Lines | Optional composition grid lines |
| Save Original | Option to auto-save raw photo before editing |

### 🎨 AI Photo Editor (`EditActivity`)
| Feature | Description |
|---------|-------------|
| ML Kit Selfie Segmentation | Intelligent human cutout with mask smoothing & edge feathering |
| 30+ World Backgrounds | Landmarks from Malaysia, Japan, China, Singapore, France, Italy, USA, etc. |
| Custom Backgrounds | Load any photo from device gallery as background |
| Color Backgrounds | White, Black, ID Photo Blue |
| Gesture Control | Pinch-to-zoom + drag-to-reposition the subject (Adjust Mode) |
| Auto Enhance | Auto white balance + skin tone optimization |
| Image Cropping | Free-form crop with rotation via uCrop library |
| Multi-Format Export | JPEG (95% / 85% / 70%), PNG (lossless), WebP |
| Share | Direct share to any app via Android Intent |
| Color Harmony | Subtle warm/cool foreground-background harmonization |

### 🗺️ Campus Map (`SchoolMapActivity`)
- Zoomable and draggable APU campus map via PhotoView (1× to 5×)

### 📅 Visit Booking (`BookingActivity`)
- Form-based tour booking with full validation (name, email, phone)

### 🖼️ My Gallery (`GalleryActivity`)
- Displays all photos saved by the app via MediaStore
- Multi-select mode: select all / deselect all
- Batch delete with Android 10+ `RecoverableSecurityException` handling
- Batch share multiple photos

### ⚙️ Settings (`SettingsActivity`)
| Setting | Options |
|---------|---------|
| Photo Resolution | High (1080p) / Medium (720p) / Low (480p) |
| Default Background | 8 solid color presets |
| Guide Lines | On / Off |
| Auto Focus (Face) | On / Off |
| Save Original | On / Off (auto-save raw photo before editing) |

---

## 🏗️ Architecture & Project Structure

```
SnapScene-Camera/
├── app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/example/snapscenecamera/
│           │   │
│           │   ├── 📱 Activities
│           │   │   ├── SplashActivity.kt          # Launch screen (2.5s → NewMainActivity)
│           │   │   ├── NewMainActivity.kt         # Digital Human Guide homepage
│           │   │   ├── MainActivity.kt            # SnapScene Camera entry hub
│           │   │   ├── CameraActivity.kt          # AI camera with face detection
│           │   │   ├── EditActivity.kt            # AI photo editor (core)
│           │   │   ├── GalleryActivity.kt         # Photo gallery & management
│           │   │   ├── SettingsActivity.kt        # App settings
│           │   │   ├── SchoolMapActivity.kt       # APU campus map viewer
│           │   │   └── BookingActivity.kt         # Visit booking form
│           │   │
│           │   ├── 🎨 Adapters
│           │   │   ├── BackgroundAdapter.kt       # Background selector RecyclerView
│           │   │   └── GalleryAdapter.kt          # Gallery grid RecyclerView
│           │   │
│           │   ├── 🤖 Digital Human
│           │   │   ├── DigitalHumanAnimationView.kt  # Animated avatar (breathing + blink)
│           │   │   └── DigitalHumanWebView.kt        # WebView-based 3D avatar (SDK ready)
│           │   │
│           │   ├── ⚙️ Engines
│           │   │   ├── engine/ExportEngine.kt     # Multi-format image export (JPEG/PNG/WebP)
│           │   │   ├── engine/FilterEngine.kt     # 12 professional photo filters
│           │   │   └── engine/ColorHarmonyEngine.kt  # Foreground-background color harmony
│           │   │
│           │   ├── 🛠️ Utilities
│           │   │   ├── utils/ImageSegmentationHelper.kt  # ML mask smoothing & alpha calc
│           │   │   └── utils/ColorCorrectionHelper.kt    # Auto white balance & skin tone
│           │   │
│           │   └── 🖼️ Custom Views
│           │       ├── ui/FaceOverlayView.kt      # Real-time face bounding box overlay
│           │       └── GuideLineView.kt           # Camera composition guide lines
│           │
│           └── res/
│               ├── layout/                        # 11 XML layouts
│               ├── drawable/                      # Icons, backgrounds, shape drawables
│               ├── values/                        # Strings, colors, styles
│               ├── values-en/ values-ms/          # English & Malay string resources
│               └── xml/                           # FileProvider paths, backup rules
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Kotlin |
| **UI Framework** | View System (XML) + Jetpack ViewBinding |
| **Camera** | CameraX 1.3.1 (Preview, ImageCapture, ImageAnalysis) |
| **AI / ML** | Google ML Kit — Selfie Segmentation + Face Detection |
| **Image Loading** | Glide 4.16.0 |
| **Image Cropping** | uCrop 2.2.8 |
| **Map Zoom** | PhotoView 2.3.0 |
| **Async** | Kotlin Coroutines + LifecycleScope |
| **Storage** | MediaStore API (Android 10+ IS_PENDING) |
| **TTS** | Android TextToSpeech (built-in) |
| **Export Formats** | JPEG, PNG, WebP |
| **EXIF** | androidx.exifinterface |
| **Min SDK** | API 24 (Android 7.0 Nougat) |
| **Target SDK** | API 34 (Android 14) |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** (use Android Studio's embedded JDK — recommended)
- Android device or emulator running **Android 7.0+ (API 24+)**

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/KinguYume-G/SnapScene-Camera.git
   cd SnapScene-Camera
   ```

2. **Open in Android Studio**
   - Select **File → Open** and choose the project root directory.

3. **Configure JDK (if needed)**
   - Go to **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
   - Set **Gradle JDK** to `Embedded JDK (jbr-17)` or `Android Studio Java Home`

4. **Sync & Build**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

---

## 📋 Permissions

| Permission | Purpose |
|-----------|---------|
| `CAMERA` | Take photos and run real-time face detection |
| `RECORD_AUDIO` | Digital Human TTS audio |
| `READ_EXTERNAL_STORAGE` | Gallery access on Android ≤ 12 |
| `READ_MEDIA_IMAGES` | Gallery access on Android 13+ |
| `WRITE_EXTERNAL_STORAGE` | Save photos on Android ≤ 8 |
| `INTERNET` | Digital Human WebView SDK (optional 3D mode) |
| `ACCESS_NETWORK_STATE` | Network availability check |

---

## 📸 App Flow

```
Launch
  └─► SplashActivity (2.5s)
        └─► NewMainActivity (Digital Human Guide)
              ├─► [AI Camera]   → MainActivity → CameraActivity → EditActivity
              ├─► [Campus Map]  → SchoolMapActivity
              └─► [Book Visit]  → BookingActivity

MainActivity
  ├─► CameraActivity  →  EditActivity  →  GalleryActivity
  ├─► GalleryActivity
  └─► SettingsActivity
```

---

## 🌍 Background Library (30+)

| Region | Locations |
|--------|-----------|
| 🇲🇾 Malaysia | Petronas Twin Towers · KL Tower · Masjid Putra · Batu Caves · Penang · Beach |
| 🇯🇵 Japan | Mount Fuji · Kyoto · Osaka Castle · Tokyo Tower |
| 🇨🇳 China | Great Wall · Forbidden City · Shanghai Bund |
| 🇸🇬 Singapore | Marina Bay Sands · Merlion · Gardens by the Bay |
| 🇦🇺 🇰🇷 🇬🇧 🇺🇸 🇩🇪 🇪🇸 🇫🇷 🇮🇹 | Sydney · Seoul · London · New York · Bavaria · Barcelona · Paris · Rome |

---

## 📐 Export Formats

| Format | Quality | Best For |
|--------|---------|---------|
| JPEG High | 95% | High-quality sharing |
| JPEG Standard | 85% | General use (balanced) |
| JPEG Compressed | 70% | Smaller file size |
| PNG Lossless | 100% | Transparent backgrounds |
| WebP | 90% | Modern format, smallest size |

---

## 🎛️ Filter Engine (12 Filters)

| # | Filter | Effect |
|---|--------|--------|
| 1 | Original | No change |
| 2 | Black & White | Weighted grayscale |
| 3 | Retro / Sepia | Classic warm brown tone |
| 4 | Cool Tone | Blue shift |
| 5 | Warm Tone | Orange/yellow shift |
| 6 | Vivid | Saturation ×1.3 |
| 7 | Soft | Saturation ×0.8 |
| 8 | High Contrast | Contrast ×1.5 |
| 9 | Japanese | Overexpose + desaturate |
| 10 | Film | Cinematic color grade |
| 11 | Cyberpunk | Blue/purple shift |
| 12 | Sunset | Warm orange/red grade |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Developer

**Jeffrey_Gao**  
Asia Pacific University (APU) · Malaysia

---

<div align="center">

Made with ❤️ for APU · Powered by Google ML Kit & CameraX

</div>
