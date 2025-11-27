# GNSS Skyplot Android App - Buildable project for GitHub Codespaces

This project targets Android 10 (API 29) and is ready to build in GitHub Codespaces or any environment with the Android SDK and JDK installed.

Recommended steps in Codespaces:
1. Open the repository in Codespaces.
2. Install Android SDK / set `ANDROID_SDK_ROOT` in Codespaces environment (Codespaces images often provide it).
3. From the terminal run:
   - If Gradle is available: `./gradlew assembleDebug` (if gradlew is present)
   - If gradlew is missing or fails, run: `gradle wrapper` then `./gradlew assembleDebug`
4. APK will be at `app/build/outputs/apk/debug/app-debug.apk`

If you want me to also create a GitHub repository with Actions for CI builds, tell me and I'll include a GitHub Actions workflow.
