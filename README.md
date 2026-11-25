# Seed Auto Farm v1.2.1 alpha
Generic Android touch-sequence prototype for permitted automation only. No root, injection, anti-cheat bypass or automatic reconnect. Account sanctions remain possible.

Enter percentage coordinates and delays (x,y,delay ms), one per line. Save, review and explicitly accept the accessibility disclosure, then enable accessibility manually, open the permitted target and press Start. Drag the control handle to move the overlay. Pause/Resume retains the current step (an in-flight tap can finish or repeat). Stop resets the sequence. Duration 0 is unlimited; 1–720 minutes is optional. The timer includes paused time.

Stops on window changes, screen lock/off, rotation, severe Android thermal status, low reported battery, rejected gestures, overlay overlap or manual stop. Optional default-on network validation and local screen text recognition stop on lost connectivity or recognized warning text. Text recognition is heuristic: missed or false detections are possible; no game-specific validation. Screen images are processed in memory, not stored or uploaded; INTERNET permission is explicitly removed. Accessibility screenshot access is powerful; disable service after use. Do not run unattended until physical device testing. No swipe or visual coordinate picker yet. Screen timeout remains controlled by Android.

Build with Gradle 8.9, JDK 17, Android SDK 35. Bundled ML Kit text recognition. Debug APK is not a production release.

## Experimental throw-cycle mode
Opt in from settings, save, open the permitted landscape target and position the player manually. Local OCR looks for TAP TO THROW or the paired THROW AGAIN / GO BACK reward screen. It issues at most one tap per alternating recognized prompt; no touches during unrecognized animation. Stops on no recognized progress for 60 seconds or safety warning. Does not attempt perfect timing or guarantee OCR accuracy, game compatibility or account safety. Test supervised first.


## Security review and signed release candidates
The app is not an accessibility aid for people with disabilities. It requests a powerful accessibility service for user-directed touch automation and local screen recognition. Do not disable Play Protect or install against a security warning. Internet-sideloaded apps using accessibility may be automatically blocked even if correctly release-signed. Review Google's official developer guidance: https://developers.google.com/android/play-protect/warning-dev-guidance . If the classification is believed to be incorrect after policy and security review, use the official appeal linked there.

Release signing configuration reads credentials from environment variables. The manual Signed release candidate workflow requires the repository secrets SEED_KEYSTORE_BASE64, SEED_STORE_PASSWORD, SEED_KEY_ALIAS and SEED_KEY_PASSWORD. The owner must generate and securely retain their own signing key; never commit the key or passwords. The workflow verifies the resulting APK signature and removes the temporary keystore. A signed candidate is not Google approval, a guarantee of installability, or a security certification. Do not distribute or use a blocked candidate until the security issue is resolved through official review.
