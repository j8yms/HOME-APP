Distribution and Test Installation
=================================

This repository contains a local Android Release APK at `app/build/outputs/apk/release/app-release-unsigned.apk`.

If you want to distribute/install the app outside the Play Store, follow these steps.

1) Signing the APK

Replace placeholders below with your preferred filenames and passwords.

```bash
# Sign the unsigned APK (requires Android build-tools `apksigner`)
apksigner sign --ks keystore.jks --ks-key-alias release \
  --out app-release-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk

# Align the APK (requires `zipalign` from Android build-tools)
zipalign -v -p 4 app-release-signed.apk app-release-aligned.apk
```

2) Distribute the signed APK

- Sideload: upload `app-release-aligned.apk` to a secure hosting location and instruct users to enable "Install unknown apps" for the source app (e.g., browser or file manager).
- Firebase App Distribution: recommended for managing testers and releases.
- Enterprise MDM: use your company's device management if distributing internally.

3) Security notes

- The repository contains a generated `keystore.jks` and `keystore.properties` used for local signing. This is convenient for testing but insecure for production. Do NOT commit real keystores to public repos. Store your keystore securely (password manager or secure file storage).
- You can replace `keystore.jks` with your own production keystore and update `keystore.properties` accordingly.
