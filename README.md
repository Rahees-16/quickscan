# QuickScan — QR & Barcode Scanner

A modern, feature-rich QR code and barcode scanner for Android, built with Kotlin and Jetpack Compose.

## Features

### Scanning
- **Real-time Camera Scanning** — CameraX + ML Kit with all major formats
- **Single / Multi Scan Mode** — toggle between one-at-a-time or continuous batch scanning
- **Gallery Scan** — pick an image and decode QR/barcodes from it
- **Flashlight Toggle** — one-tap torch for low-light scanning
- **Scan Behaviors** — configurable vibrate, beep sound, auto-copy to clipboard

### Supported Formats
QR Code, EAN-13, EAN-8, UPC-A, UPC-E, Code-128, Code-39, Code-93, ITF, Codabar, PDF417, Aztec, Data Matrix

### Smart Actions
Auto-detects content type and offers contextual actions:
- URL → Open Browser, Copy Link
- WiFi → Connect to Network, Copy Password
- Contact (vCard) → Add Contact, Call, Email
- Phone → Call, Send SMS, Copy
- Email → Compose Email, Copy
- Location → Open Maps, Copy Coordinates
- Plain Text → Copy, Search Web

### QR & Barcode Generator
- **9 barcode formats**: QR Code, Code 128, Code 39, EAN-13, EAN-8, UPC-A, PDF 417, Aztec, Data Matrix
- **Custom QR colors**: 8 foreground + 6 background colors with real-time preview
- **QR content types**: Text, URL, WiFi, Contact, Email, Phone
- **Format validation**: digit count validation for EAN/UPC
- **Save & Share**: save to gallery or share via any app

### History & Stats
- Full scan history with search, filter (All/QR/Barcode/Favorites), swipe-to-delete
- Scan statistics: total scans, QR count, barcode count, favorites
- CSV export of scan history
- Empty states with call-to-action

### User Experience
- Onboarding screen (3-slide first launch)
- Rate app prompt (non-intrusive, after 5th launch)
- Material3 + Material You dynamic colors
- Light/Dark mode with in-app theme switcher
- Splash screen with brand colors
- Responsive tablet layouts (2-column history, side-by-side generator)
- AdMob banner ad (home screen only)

### Internationalization
14 languages: English, Spanish, French, German, Hindi, Arabic, Malay, Marathi, Tamil, Malayalam, Telugu, Kannada, Gujarati, Punjabi

## Tech Stack
Kotlin, Jetpack Compose, Material3, Hilt, Room, CameraX, ML Kit Barcode Scanning, ZXing, DataStore, AdMob

**Min SDK:** 26 | **Target SDK:** 35

## Building
```bash
./gradlew assembleDebug
```
