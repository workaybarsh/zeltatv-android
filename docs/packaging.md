# Android Packaging

## Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The **Android Debug** GitHub Actions workflow uploads this APK as
`zeltatv-android-debug` (artifact, 14 days). This is the APK to sideload on Android TV,
phones and tablets during M1/M2 testing.

## Release APK / AAB

Release signing requires a private keystore. Do **not** commit the keystore or passwords.

1. Create a keystore outside the repository:

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\zeltatv-release.jks" -alias zeltatv `
  -keyalg RSA -keysize 2048 -validity 10000
```

2. Add `keystore.properties` (already gitignored) locally:

```properties
storeFile=C:/Users/<user>/zeltatv-release.jks
storePassword=...
keyAlias=zeltatv
keyPassword=...
```

3. Add a `signingConfigs` block to `app/build.gradle.kts` that reads this file when present.
4. Build:

```powershell
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:bundleRelease
```

For GitHub Actions, store the keystore as a base64 secret and the passwords as repository
secrets; never upload the `.jks` itself.

## Play Store checklist

- Signed AAB, target SDK 35
- Android TV banner and leanback launcher (already present)
- Privacy policy / data safety form
- Screenshots for TV, phone and tablet
- Closed testing track before production

## Uninstall

The manifest sets `android:allowBackup="false"`. Android removes the app's private database,
preferences and files on uninstall; no cloud backup is kept for restoration.
