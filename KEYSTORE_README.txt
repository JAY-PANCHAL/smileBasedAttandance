SMILE ATTENDANCE - RELEASE SIGNING KEY
======================================

WHY THIS MATTERS
Every release APK must be signed with the SAME key. If the key is lost, new
APKs cannot be installed over the old app - every tablet must uninstall the
app, then re-pair and re-sign-in. (This already happened once: the original
key was lost with a deleted folder, and a new key was created on 2026-10-02.)

KEY DETAILS
  File location : /Users/mindtechm2/StudioProjects/keystore/release.keystore
                  (one folder ABOVE this repo - deliberately NOT in git)
  Alias         : smileattendance
  Algorithm     : RSA 2048, valid 10000 days (created 2026-10-02)
  Owner         : CN=GoldStar Jewellery, OU=IT, O=GoldStar Jewellery, C=IN
  Password      : NOT stored here. Keep it in the team password manager.
                  (Store password and key password are the same.)

WHERE THE BUILD READS IT
  build.gradle.kts -> android { signingConfigs { create("release") { ... } } }
  storeFile = file("../keystore/release.keystore")

BACKUP CHECKLIST
  [ ] Copy release.keystore to a safe place (company drive / password manager
      attachment). Do NOT commit it to this repo.
  [ ] Save the password in the password manager next to the file.

BUILDING A RELEASE APK
  1. Make sure ../keystore/release.keystore exists (restore from backup).
  2. Run:  ./gradlew assembleRelease
  3. Output: build/outputs/apk/release/smileBasedAttandance-release.apk

IF THE KEY IS LOST AGAIN
  1. Create a new one:
       keytool -genkeypair -keystore ../keystore/release.keystore \
         -alias smileattendance -keyalg RSA -keysize 2048 -validity 10000
  2. Rebuild the release APK.
  3. On every tablet: uninstall the old app, install the new APK, re-pair the
     device (server address + device code + device key) and sign in again.
