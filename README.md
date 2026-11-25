# Seed Auto Farm v1.2 alpha
Generic Android touch-sequence prototype for permitted automation only. No root, injection, anti-cheat bypass or automatic reconnect. Account sanctions remain possible.

Enter percentage coordinates and delays (x,y,delay ms), one per line. Save, enable accessibility manually, open the permitted target and press Start. Drag the control handle to move the overlay. Pause/Resume retains the current step (an in-flight tap can finish or repeat). Stop resets the sequence. Duration 0 is unlimited; 1–720 minutes is optional. The timer includes paused time.

Stops on window changes, screen lock/off, rotation, severe Android thermal status, low reported battery, rejected gestures, overlay overlap or manual stop. Optional default-on network validation and local screen text recognition stop on lost connectivity or recognized warning text. Text recognition is heuristic: missed or false detections are possible; no game-specific validation. Screen images are processed in memory, not stored or uploaded; INTERNET permission is explicitly removed. Accessibility screenshot access is powerful; disable service after use. Do not run unattended until physical device testing. No swipe or visual coordinate picker yet. Screen timeout remains controlled by Android.

Build with Gradle 8.9, JDK 17, Android SDK 35. Bundled ML Kit text recognition. Debug APK is not a production release.

## Experimental throw-cycle mode
Opt in from settings, save, open the permitted landscape target and position the player manually. Local OCR looks for TAP TO THROW or the paired THROW AGAIN / GO BACK reward screen. It issues at most one tap per alternating recognized prompt; no touches during unrecognized animation. Stops on no recognized progress for 60 seconds or safety warning. Does not attempt perfect timing or guarantee OCR accuracy, game compatibility or account safety. Test supervised first.
