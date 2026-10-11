# ZeroGrid — Mobile & Web Operations Run Guide

> [!IMPORTANT]
> **LOCAL TESTING & EVALUATION NOTICE**  
> All emulator configurations, loopback endpoints (`10.0.2.2`), and local mock containers described in this guide are provided **strictly for local testing, offline development, and hackathon evaluation**.
> - **Mobile Client**: The default production APK connects directly to the **Live Hosted Emergency Service**. Connecting to local IP addresses is intended **strictly for local testing**.
> - **Local Containers**: Any local container orchestration is intended **strictly for local offline sandbox testing**.

---

## 📑 Table of Contents
1. [📱 Part 1: Mobile Client (Android APK)](#-part-1-mobile-client-android-apk)
   - 1.1 [Recommended: Install Latest Released APK](#11-recommended-install-latest-released-apk)
   - 1.2 [Sideloading & ADB Installation](#12-sideloading--adb-installation)
   - 1.3 [Permissions & Operational Verification](#13-permissions--operational-verification)
   - 1.4 [Building from Source (Optional for Developers)](#14-building-from-source-optional-for-developers)
2. [☁️ Part 2: Live Hosted Service & Endpoints](#️-part-2-live-hosted-service--endpoints)
   - 2.1 [Live Hosted Endpoints](#21-live-hosted-endpoints)
   - 2.2 [Service Health & Public API Verification](#22-service-health--public-api-verification)
3. [💻 Part 3: Web Command Center (Next.js 16)](#-part-3-web-command-center-nextjs-16)
   - 3.1 [Local Web Execution](#31-local-web-execution)
   - 3.2 [Hosting & Deployment](#32-hosting--deployment)
4. [🐳 Part 4: Docker Containerization](#-part-4-docker-containerization)
   - 4.1 [Frontend Docker Container](#41-frontend-docker-container)
   - 4.2 [Local Testing Multi-Container Stack](#42-local-testing-multi-container-stack)

---

## 📱 Part 1: Mobile Client (Android APK)

The ZeroGrid Android client delivers life-saving communications via dual operational modes:
* **Off-Grid Radio Mesh**: P2P radio broadcast over BLE 5.0 and Wi-Fi Direct when cellular networks fail.
* **Hosted Cloud Gateway**: Automated distress synchronization with the Hosted Emergency Service when internet is available.

---

### 1.1 Recommended: Install Latest Released APK

> [!TIP]
> **For evaluators, emergency responders, and testers**:  
> You do **not** need to set up Android Studio or compile native C++ toolchains. Please install the **latest pre-built APK release**:
>
> 📥 **Download**: [**ZeroGrid Latest Release APK**](https://github.com/hehemohit/Zero_Grid/releases)  
> *(Or use the local pre-built artifact: `app/build/outputs/apk/debug/app-debug.apk`)*

This build is pre-configured out-of-the-box to connect securely to the **Live Hosted Emergency Service** (`https://zero-grid-l28u.onrender.com/`).

---

### 1.2 Sideloading & ADB Installation

#### Method A: Direct Sideloading onto Physical Device
1. Download `app-debug.apk` directly to your Android device (via browser, Google Drive, or USB file transfer).
2. Tap the file in your device's File Manager.
3. Select **Install**. If prompted, grant permission to *"Install unknown apps"* for your browser or file manager.

#### Method B: Install via ADB (Android Debug Bridge)
1. Enable **Developer Options** and **USB Debugging** on your phone.
2. Connect your device via USB and verify connection:
   ```bash
   adb devices
   ```
3. Install the APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

### 1.3 Permissions & Operational Verification

When launching ZeroGrid for the first time, grant the requested system runtime permissions:
1. **Location** (*Fine & Coarse*): Computes offline GPS SOS coordinates and calculates Safe Route Detours around flooded hazard zones.
2. **Nearby Devices / Bluetooth**: Powers BLE 5.0 Peripheral Advertising & Central Scanning.
3. **Wi-Fi / Nearby Wi-Fi Devices**: Forms high-throughput Wi-Fi Direct P2P emergency clusters.
4. **Notifications**: Maintains Android Foreground Service discovery when the screen is locked.

#### Operational Verification Scenarios:
* **Scenario A: Hosted Cloud SOS Mode**  
  With mobile data or Wi-Fi active, trigger an SOS alert. The beacon dispatches directly to the Hosted Emergency Service and syncs in real-time with the Web Command Center.
* **Scenario B: Off-Grid Radio Mesh Mode (Strictly No Internet)**  
  Enable **Airplane Mode**, then turn on **Bluetooth** and **Wi-Fi**. Trigger an SOS beacon. ZeroGrid generates 32-bit word-aligned binary RF packets. A nearby phone running ZeroGrid receives the hop, verifies TTL, and re-broadcasts without any cellular towers.

---

### 1.4 Building from Source (Optional for Developers)

If you are modifying Android Kotlin code or the C++ native routing library:

#### Toolchain Requirements:
* **Android Studio**: Ladybug (2024.2.1+) or Meerkat (2024.3+)
* **JDK**: 17
* **Android SDK**: API 35 (minSdk: 26 — Android 8.0+)
* **Android NDK**: Version `27.0.12077973`
* **CMake**: Version `3.22.1+`

#### Configuration (`local.properties`):
```properties
sdk.dir=C:\\Users\\<YOUR_USERNAME>\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=AIzaSyYourGoogleMapsApiKeyHere
```

#### Build Commands:
* **Windows (PowerShell)**:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
* **macOS / Linux**:
  ```bash
  ./gradlew assembleDebug
  ```
The output APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

> [!NOTE]
> **Strictly for Local Testing**: If you need to redirect the mobile client from the default Hosted Service to a local mock server during debugging, configure `BASE_URL` in `app/build.gradle.kts`:
> ```kotlin
> // Strictly for local emulator testing:
> buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5000/\"")
> ```

---

## ☁️ Part 2: Live Hosted Service & Endpoints

ZeroGrid operations are anchored by a live hosted cloud platform managing disaster triage, incident tracking, and real-time feeds.

### 2.1 Live Hosted Endpoints

The hosted emergency platform is live and operational:
* **Hosted Base URL**: `https://zero-grid-l28u.onrender.com`
* **System Health Check**: `https://zero-grid-l28u.onrender.com/health`
* **Real-time Distress WebSocket**: `wss://zero-grid-l28u.onrender.com/sos`
* **Operational Telemetry Metrics**: `https://zero-grid-l28u.onrender.com/metrics`

---

### 2.2 Service Health & Public API Verification

You can verify connectivity to the hosted service using standard terminal tools:

#### 1. Health Probe Verification
```bash
curl -i https://zero-grid-l28u.onrender.com/health
```
**Expected Response (`200 OK`)**:
```json
{
  "status": "healthy",
  "service": "zerogrid-backend",
  "timestamp": "2026-10-11T11:00:00.000Z",
  "database": "connected"
}
```

#### 2. Nearest Emergency Headquarters Query
```bash
curl -X GET "https://zero-grid-l28u.onrender.com/api/v1/public/zones/nearest?lat=19.4565&lng=72.8125"
```

#### 3. Disaster Risk Density Query
```bash
curl -X GET "https://zero-grid-l28u.onrender.com/api/v1/public/sos/density?lat=19.4539&lng=72.8115&radiusKm=10"
```

#### 4. Distress Alert Ingestion
```bash
curl -X POST "https://zero-grid-l28u.onrender.com/api/v1/public/sos/ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "lat": 19.4539,
    "lng": 72.8115,
    "sourceApp": "Mobile-P2P-Gateway",
    "category": "MEDICAL",
    "message": "Trapped on roof with infant. Water at first floor level.",
    "batteryPercentage": 68
  }'
```

---

## 💻 Part 3: Web Command Center (Next.js 16)

The Web Command Center provides emergency personnel with interactive live map tracking, risk telemetry, and distress management.

### 3.1 Local Web Execution

1. **Navigate to the frontend directory**:
   ```bash
   cd gridZeroExpress/frontend
   ```

2. **Configure Environment (`.env.local`)**:
   Point directly to the live hosted emergency service:
   ```env
   # Target Hosted Service:
   NEXT_PUBLIC_API_URL=https://zero-grid-l28u.onrender.com

   # Google Maps JavaScript API Key:
   NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=AIzaSyYourGoogleMapsApiKeyHere
   ```

3. **Install & Run**:
   ```bash
   npm install
   npm run dev
   ```
   Open [http://localhost:3000](http://localhost:3000) in your browser.

---

### 3.2 Hosting & Deployment

* **Vercel**: Connect the repository, set Root Directory to `gridZeroExpress/frontend`, and define `NEXT_PUBLIC_API_URL=https://zero-grid-l28u.onrender.com`.
* **AWS Amplify**: Configure monorepo app build targeting `gridZeroExpress/frontend`.

---

## 🐳 Part 4: Docker Containerization

### 4.1 Frontend Docker Container

A production-ready Dockerfile is provided at [`gridZeroExpress/frontend/Dockerfile`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/gridZeroExpress/frontend/Dockerfile):
* Multi-stage build using Alpine Node 20.
* Generates Next.js standalone output (~150MB container footprint).
* Runs securely under a non-root `nextjs` user.

#### Build and Run:
```bash
cd gridZeroExpress/frontend

# Build frontend container image:
docker build -t zerogrid-web --build-arg NEXT_PUBLIC_API_URL=https://zero-grid-l28u.onrender.com .

# Run container on port 3000:
docker run -d -p 3000:3000 --name zerogrid-web zerogrid-web
```
Access the dashboard at `http://localhost:3000`.

---

### 4.2 Local Testing Multi-Container Stack

> [!WARNING]
> **Strictly for Local Testing**: The Docker Compose configuration provided at [`docker-compose.yml`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/docker-compose.yml) is meant **strictly for local offline testing and sandboxed evaluation** on your workstation.

```bash
# Strictly for local testing:
docker compose up --build -d
```
* **Web Interface**: `http://localhost:3000`
* **Local Test Services**: `http://localhost:5000`

To stop local testing containers:
```bash
docker compose down
```
