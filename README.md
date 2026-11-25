# Seed Auto Farm alpha
Generic Android touch-sequence prototype. Only use in applications where automation is permitted. No network permission, root, injection, or anti-cheat bypass. Account sanctions remain possible.

Configure percentage coordinates and delays (x,y,delay ms), save, enable accessibility manually, switch to the permitted target app, and press Start. Stop before switching apps. Pause stops and Start restarts. Session duration defaults to unlimited (0); optional timer 1–720 minutes. Stops on low battery (below 10% when reported), target app changes, screen lock, or manual stop. In-app disconnect detection is not implemented; an in-app popup can still receive touches. Do not use unattended. Existing saved durations remain unchanged. Disable the service when finished. Do not run unattended until real-device testing. No screen recognition, swipes or game-specific presets.

Build with Gradle 8.9, JDK 17, Android SDK 35. Debug APK is not a production release.