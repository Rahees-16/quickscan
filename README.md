# QuickScan — QR & Barcode Scanner

A modern, feature-rich QR code and barcode scanner for Android, built with Kotlin and Jetpack Compose.

## Features

- **Real-time Camera Scanning** — CameraX + ML Kit barcode scanning with all major formats
- **Multi-Format Support** — QR Code, EAN-13, EAN-8, UPC-A, UPC-E, Code-128, Code-39, Code-93, ITF, Codabar, PDF417, Aztec, Data Matrix
- **Smart Actions** — Auto-detects content type and offers contextual actions:
  - URL → Open Browser, Copy Link
  - WiFi → Connect to Network, Copy Password
  - Contact → Add Contact, Call, Email
  - Phone → Call, Send SMS, Copy
  - Email → Compose Email, Copy
  - Location → Open Maps, Copy Coordinates
- **Scan History** — Full history with search, filter by type, favorites, swipe-to-delete
- **QR Code Generator** — Create QR codes for Text, URL, WiFi, Contact, Email, Phone
- **Batch Scanning** — Continuous scan mode for scanning multiple codes in one session
- **Gallery Scan** — Scan QR/barcodes from gallery images
- **Flashlight** — One-tap torch for low-light scanning

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3 (Material You dynamic colors)
- **Architecture:** MVVM (ViewModel + Repository)
- **DI:** Hilt
- **Database:** Room
- **Camera:** CameraX
- **ML:** ML Kit Barcode Scanning
- **QR Generation:** ZXing
- **Settings:** DataStore Preferences
- **Min SDK:** 26 | **Target SDK:** 35

## Building

```bash
./gradlew assembleDebug
```

## Screenshots

_Coming soon_

## License

All rights reserved.
