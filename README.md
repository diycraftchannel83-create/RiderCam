# RiderCam

RiderCam is an Android motorsport dashcam concept for motorcycles and cars, with a live camera preview, GPS speedometer, trip telemetry, and recorded-video overlays.

## Initial product specification
- Automatic portrait and landscape layouts
- Front/rear camera selection where supported by the device
- Target recording mode: 1080p at 60 FPS, subject to camera capabilities
- GPS telemetry: speed, coordinates, timestamp, distance, and altitude
- Burn-in telemetry on exported/recorded video
- Automatic segmented recording
- Dark racing UI with neon-green accents

## Project status
This repository is the initial native Android/Kotlin scaffold. The dashboard UI is a prototype; production camera recording, telemetry burn-in encoding, segmented file finalization, and device-specific 60 FPS validation must be implemented and tested before release.

## Build
Open this repository in Android Studio with a compatible Android SDK and Gradle installation, then sync and run the app on a physical Android device.

## Safety
Use a secure vehicle mount. Configure the app before riding. GPS speed can lag and is not a substitute for vehicle safety instruments. Do not interact with the phone while driving.
