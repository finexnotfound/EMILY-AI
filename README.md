# Emily AI — Personal Android Jarvis

![Emily AI Banner](app/src/main/res/drawable/ic_emily_logo.png)

> **A native Android voice assistant engineered for maximum responsiveness, low battery usage, and low memory consumption on 3 GB RAM devices (Samsung Galaxy SM-A042F running Android 14).**

---

## 📦 Direct APK Download

The compiled, ready-to-install APK file is located directly in the root of this repository:

* 📱 **[`EmilyAI.apk`](./EmilyAI.apk)** *(Ready to install on your phone)*
* 📱 **[`app-debug.apk`](./app-debug.apk)** *(Alternative debug build)*

Simply download **`EmilyAI.apk`** to your phone, tap to install, and grant the required permissions.

---

## ⚡ Highlights

* **Local "Hey Emily" Wake Phrase**: Local speech detector running in the background without cloud latency.
* **Continuous Conversation**: Speak naturally back and forth without having to touch your phone or press buttons between replies.
* **Android 14 Compliant**: Uses `FOREGROUND_SERVICE_MICROPHONE` with a persistent notification and an instant stop button.
* **Gemini AI Brain**: Powered by Google Gemini with Function Calling tools for intelligent reasoning, coding help, and explanations.
* **Real Phone Automation**: Controls apps, alarms, timers, volume, flashlight, web searches, and navigation via native Android APIs.
* **Accessibility UI Automation (`EmilyAccessibilityService`)**: Reads screen text aloud, scrolls apps, clicks buttons, and executes Back/Home actions hands-free.
* **Notification Reader (`EmilyNotificationListener`)**: Reads aloud active WhatsApp, SMS, and app notifications upon voice command.
* **Offline Fallback Engine**: Core phone commands execute instantly even without an internet connection or API key.
* **Nothing OS Minimal Aesthetic**: Monochromatic black-and-white interface featuring a reactive audio orb that animates with speech decibel levels.

---

## 📱 Device Specifications & Optimization

| Specification | Target Details |
| :--- | :--- |
| **Target Device** | Samsung Galaxy SM-A042F |
| **Android Version** | Android 14 (One UI) |
| **RAM** | 3 GB (Engineered for light memory footprint and fast garbage collection) |
| **Audio Processing** | Duty-cycled recognition with gentle haptic vibration feedback |
| **UI Framework** | 100% Jetpack Compose with Material Design 3 and Nothing OS styling |

---

## 🚀 Beginner-Friendly Setup Guide

Follow these steps on your phone after installing **`EmilyAI.apk`**:

### Step 1: Open Permissions Setup
Open Emily AI and tap the **ACCESS** tab at the top.

### Step 2: Grant Microphone & Notifications
* Tap **GRANT MIC** → Select **"While using the app"**.
* Tap **GRANT NOTIFS** → Allow notifications (required by Android 14 for the background listener).

### Step 3: Exempt from Battery Optimization *(CRITICAL for Samsung)*
* Samsung One UI aggressively stops background apps. Tap **EXEMPT BATTERY** in the **ACCESS** tab and choose **"Allow / Unrestricted"**. This allows "Hey Emily" to continue working when your screen is locked.

### Step 4: Enable Accessibility Service *(For UI Automation)*
* Tap **ENABLE SERVICE** to open Android Accessibility settings.
* Under **Installed apps**, select **Emily AI** and toggle it **On**.
* This enables hands-free commands like *"Read my screen"*, *"Scroll down"*, and *"Go back"*.

### Step 5: Enable Notification Access *(To Read Alerts)*
* Tap **ENABLE ACCESS** in the **ACCESS** tab and toggle notification access for Emily AI.

### Step 6: Start Background Listener
* Tap **START BACKGROUND LISTENER**.
* You will see a persistent notification in your status bar indicating Emily is active.

---

## 🗣️ Voice Commands Cheat Sheet

### 1. Wake & Standby
* *"Hey Emily"*
* *"Wake up Emily"*
* *"Go to sleep"* / *"Stop listening"*

### 2. Phone Control & Hardware
* *"Hey Emily, open YouTube"* (or WhatsApp, Chrome, Camera, Spotify, etc.)
* *"Hey Emily, turn on the flashlight"* / *"Turn off flashlight"*
* *"Hey Emily, increase volume"* / *"Volume down"* / *"Mute"*
* *"Hey Emily, set a timer for 5 minutes"*
* *"Hey Emily, set an alarm for 7:00 AM"*
* *"Hey Emily, search Google for Python tutorials"*
* *"Hey Emily, navigate to Central Park"*

### 3. Screen & Navigation (via Accessibility)
* *"Hey Emily, read this screen to me"*
* *"Hey Emily, scroll down"* / *"Scroll up"*
* *"Hey Emily, go back"*
* *"Hey Emily, go home"*
* *"Hey Emily, open recent apps"*

### 4. Notifications
* *"Hey Emily, read my notifications"* (reads unread WhatsApp, SMS, and app alerts)

### 5. Knowledge & AI Reasoning (Gemini)
* *"Hey Emily, explain how quantum computing works in two sentences."*
* *"Hey Emily, help me write a polite email to my manager."*
* *"Hey Emily, who are you?"*

---

## ⚙️ Configuration & Settings

Inside the **CONFIG** tab:
* **Continuous Conversation**: Toggle on to keep Emily listening for follow-up commands after speaking.
* **Language & Accent**: Choose between **English / Hinglish (India)**, **English (US)**, and **English (UK)**.
* **Natural Female Voice**: Fine-tune voice pitch (default `1.05x`) and speech speed (default `1.00x`).
* **AI Model Selection**:
  * `gemini-3.5-flash` *(Default, fast and versatile)*
  * `gemini-3.1-pro-preview` *(High thinking mode for complex reasoning and code)*
  * `gemini-3.1-flash-lite-preview` *(Ultra-fast response)*
* **API Key**: Automatically loaded from AI Studio build configuration (`BuildConfig.GEMINI_API_KEY`). You can also input a custom key override in the settings.

---

## 🛠️ Building from Source

If you want to compile the project yourself:

```bash
# Clone the repository
git clone <repo-url>
cd emily-ai

# Build the Debug APK
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```

The resulting APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
This project is open-source and intended for personal Android device automation.
