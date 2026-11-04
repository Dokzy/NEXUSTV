# NEXUS TV – Android TV Home Launcher

Open this folder in Android Studio (Koala+), let it sync (it creates the Gradle wrapper), then Run on an Android TV device/emulator.

## Make it the default HOME
1. Press HOME → choose NEXUS TV → "Always" (if prompted), or
2. In-app: Settings → "Set NEXUS TV as default launcher", or
3. ADB (no root): `adb shell cmd package set-home-activity com.nexus.tv/.HomeActivity`

To revert: `adb shell cmd package set-home-activity <original launcher component>` or Settings → Apps → NEXUS TV → Clear defaults.

## Controls
- D-pad: navigate, OK: launch, long-press OK: favorite / reorder / app info / uninstall, Back: return to top (never exits).

## Getting the APK
- **Android Studio:** Build > Build APK(s). Output: app/build/outputs/apk/debug/app-debug.apk
- **GitHub (no local setup):** push this folder to a GitHub repo; the "Build NEXUS TV APK" workflow produces the APK under Actions > Artifacts.
- **Install on TV:** `adb connect <tv-ip>` then `adb install app-debug.apk`
