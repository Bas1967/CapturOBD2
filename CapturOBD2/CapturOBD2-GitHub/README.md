# Captur OBD Diagnose – Android

Android Bluetooth Classic (SPP) ELM327 project for basic OBD-II engine diagnostics. The project is intended as a diagnostic helper; adapter and vehicle compatibility varies.

## Build APK on GitHub (no Android Studio needed)
1. Create a GitHub repository, for example `CapturOBD2`.
2. Upload **all contents of this folder**, including `.github/workflows/build-apk.yml`, to the repository root (not this enclosing folder).
3. Commit the files to `main` or `master`. The workflow runs on push automatically.
4. In the repository, open **Actions** and select **Build Captur OBD APK**.
5. When the run finishes successfully, open the run and download the artifact **CapturOBD-debug-apk**.
6. Extract the artifact ZIP and install `app-debug.apk` on your Android phone. Android may ask permission to install apps from this source.

## Bluetooth setup
- Pair the ELM327 in Android Bluetooth settings first (classic Bluetooth ELM adapters, not BLE-only adapters).
- Plug the adapter into the vehicle OBD socket and switch ignition on.
- Open the app, choose the paired adapter and connect.

## Important
- The app sends standard OBD-II commands. It cannot access all Renault-specific modules or proprietary codes.
- Clearing codes may erase freeze-frame/readiness information and does not fix the underlying fault. Do not clear faults before recording them.
- Do not use the app while driving. Use it parked, or have a passenger operate it.
- This project is not yet validated on a specific vehicle/adapter pair.
