# Lunas

Lunas is an Android app for keeping track of paylater services, bills, payments, and due dates. It also includes a home-screen summary widget and JSON backup/restore.

## Project status

**Experimental and mostly vibe-coded.** Lunas was built through iterative prompting and substantial use of AI-generated code, with human direction. The code has not had a comprehensive independent review, security audit, or test pass. Expect bugs and verify the app and its backups before relying on it.

This app is a personal organizer, not financial advice or a payment service. You remain responsible for checking bill details and keeping your own backups.

The app is provided as-is. Data may be lost, corrupted, or inaccurately displayed, and you use it at your own risk. Keep independent backups of important information and verify exported backups before relying on them. To the extent permitted by law, the project authors and contributors are not liable for damages or data loss arising from use of the app; see [LICENSE](./LICENSE).

## Privacy

Lunas stores the bill, payment, and settings information you enter on your device. The app does not send this information to a Lunas server, and Android automatic backups are disabled. If you export a JSON backup, you choose where to save it; that location may be managed or synced by another provider. Keep exported backups private.

## Build

Requirements:

- Android Studio with Android SDK Platform 37 and Build Tools 37.0.0
- JDK 17

From the repository root, build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is written under `app/build/outputs/apk/debug/`.

Release builds enable R8 code minification and resource shrinking.

## Release signing

Release signing credentials are read from a local `keystore.properties` file in the repository root. Create it with these entries:

```properties
storeFile=keystore/release.jks
storePassword=your-keystore-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

Keep both `keystore.properties` and the keystore file private; they are excluded from Git. Without this file, Gradle can build an unsigned release artifact.

## License

This project is licensed under the MIT License. See [LICENSE](./LICENSE) for details.
