---
name: android-headless-env
description: Setup headless Android SDK, OpenJDK, Gradle, and Android build tools on Windows PowerShell
---

# Android Headless Environment Setup

## Installation with winget
Run the following PowerShell commands:
```powershell
winget install --id Microsoft.OpenJDK.17 -e --silent --accept-package-agreements --accept-source-agreements
winget install --id Google.PlatformTools -e --silent --accept-package-agreements --accept-source-agreements
winget install --id Gradle.Gradle -e --silent --accept-package-agreements --accept-source-agreements
```

## Android SDK & Command-line Tools Setup
If `$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat` does not exist:
1. Download `https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip`
2. Extract into `$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest` (ensure directory structure puts `bin` and `lib` directly inside `latest`).
3. Set environment variables:
   - `JAVA_HOME` pointing to installed OpenJDK 17
   - `ANDROID_HOME` pointing to `$env:LOCALAPPDATA\Android\Sdk`
   - Update `PATH` to include cmdline-tools, platform-tools, and OpenJDK.
4. Accept licenses:
```powershell
1..15 | ForEach-Object { "y" } | & "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```
5. Install platform and build-tools:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-34" "build-tools;34.0.0"
```
