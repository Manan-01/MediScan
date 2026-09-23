# 💊 MediScan - Intelligent Medicine Expiry Tracker & Scanner

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java_11-orange.svg?logo=java)](https://www.oracle.com/java/)
[![MinSDK](https://img.shields.io/badge/MinSDK-26-blue.svg)](https://developer.android.com/about/versions/oreo)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-brightgreen.svg)](https://developer.android.com/about/versions/14)
[![ML Kit](https://img.shields.io/badge/ML--Kit-Text_%26_Barcode-blue.svg)](https://developers.google.com/ml-kit)
[![Room](https://img.shields.io/badge/Database-Room_2.8.5-red.svg)](https://developer.android.com/training/data-storage/room)

Submitted for **Global Innovation Hackathon 2026 – Build for a Better Future** organized by **Bharat Academix**.

---

## 📲 Direct APK Download & Installation

You can download and install the pre-built Android APK directly onto your Android device:

[![Download APK](https://img.shields.io/badge/Download-MediScan.apk-brightgreen?style=for-the-badge&logo=android)](https://github.com/Manan-01/MediScan/raw/main/apk/MediScan.apk)

### Quick Installation Guide:
1. Tap **[Download MediScan.apk](https://github.com/Manan-01/MediScan/raw/main/apk/MediScan.apk)** on your Android device.
2. Open the downloaded file (`MediScan.apk`).
3. If prompted, enable **"Install from unknown sources"** in your device settings.
4. Tap **Install** and launch **MediScan**!

---

## 📌 Problem Statement & Overview

Unused, forgotten, or expired medicines in households pose serious health risks if consumed accidentally and contribute to environmental hazards when disposed of improperly. Manually maintaining medicine expiry dates is tedious, error-prone, and often overlooked.

**MediScan** is a native Android application designed to eliminate accidental consumption of expired drugs. MediScan leverages device camera capabilities, on-device Machine Learning (Google ML Kit), local offline-first database storage (Room), and daily background scheduling (WorkManager) to scan medicine labels, extract expiration dates, track inventory, and send timely alerts before medicines expire.

---

## ✨ Key Features

### 📸 1. Dual Scanning Engine
- **Text Recognition Burst Mode**: Uses CameraX to capture a 3-frame burst of label images. Runs on-device ML Kit Text Recognition with majority-voting algorithms and regex pattern matchers to parse expiration dates (`MM/YY`, `DD.MM.YYYY`, `EXP MMM YYYY`).
- **GS1 DataMatrix Barcode Scanner**: Reads 2D GS1 DataMatrix barcodes (standard on modern pharmaceutical packaging) to extract Application Identifier `17` (Expiration Date `YYMMDD`).

### 🤖 2. Offline-First with Optional AI Refinement
- Works 100% offline using local ML Kit models and regex logic.
- Connects to the live cloud Gemini AI backend service deployed on Render (`https://mediscan-backend-oes2.onrender.com`) when internet is available to refine extracted medicine names and expiration dates.

### 📦 3. Local Room Database & Validation
- Saves verified medicines in an offline SQLite database via Jetpack Room.
- Prevents saving already-expired medicines with automated validation rules.

### 🎨 4. Urgency Color-Coded Inventory & Live Search
Displays medicine inventory sorted chronologically by expiry date with clear visual urgency indicators:
- 🔴 **Expired**: Red indicator
- 🟧 **Expires within 30 days**: Orange indicator
- 🟨 **Expires within 90 days**: Yellow indicator
- 🟢 **Safe / Fresh (>90 days)**: Green indicator
Includes instant live search to quickly find specific medicines.

### 🔔 5. Automated Daily Alerts & Persistent Reminders
- **Background WorkManager**: Enqueues a daily `ExpiryCheckWorker` task to monitor inventory in the background without draining battery.
- **Push Notifications**: Sends high-priority notifications when a medicine is nearing expiry or has expired.
- **Sticky Nag Notification**: Displays a non-intrusive ongoing system alert if expired medicines remain in the home cabinet, urging safe disposal.

---

## 🛠️ Technology Stack & Architecture

- **Language**: Java 11
- **UI Architecture**: Single Activity (`MainActivity`) with Jetpack Fragments (`ScanFragment`, `MedicineListFragment`)
- **Camera Pipeline**: Android Jetpack `CameraX` (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)
- **On-Device Machine Learning**: Google ML Kit (`text-recognition`, `barcode-scanning`)
- **Persistence Layer**: Android Jetpack `Room` (`room-runtime`, `room-compiler`)
- **Background Scheduler**: Jetpack `WorkManager` (`work-runtime`)
- **UI Components**: Material Components (`BottomNavigationView`, `TabLayout`, `RecyclerView`, `CardView`), `ConstraintLayout`
- **Networking**: `HttpURLConnection` for optional AI refinement endpoint

---

## 📁 Directory Structure

```
MediScan/
├── app/                  # Android Application module (CameraX, ML Kit, Room, WorkManager)
├── backend/              # Spring Boot Gemini AI Backend
│   ├── src/
│   ├── pom.xml
│   ├── run.bat.example
│   └── README.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md             # Master Hackathon Documentation
```

---

## 🚀 Getting Started & Installation

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 11
- Android device or emulator running Android 8.0 (API level 26) or higher with camera support

### Setup Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/Manan-01/MediScan.git
   cd MediScan
   ```
2. **Open in Android Studio**:
   - Select **File > Open** and navigate to the cloned `MediScan` folder.
   - Wait for Gradle sync to complete.
3. **Build & Run**:
   - Connect an Android device or start an emulator.
   - Click **Run > Run 'app'** or press `Shift + F10`.

---

## 👨‍💻 Author

**Manan Pareek**  
- Participant - Global Innovation Hackathon 2026  
- Bharat Academix Participant  

---
*Built for a Better Future 🌿*
