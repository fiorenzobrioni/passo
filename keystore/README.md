# Keystores

Two keystores live in this folder, and both are committed **on purpose**. Neither is secret:
their passwords are written below.

| File | Alias | Store / key password | What it signs | Lifetime |
|---|---|---|---|---|
| `debug.keystore` | `passo-debug` | `android` / `android` | Debug builds, and the debug-signed release APK CI builds for testing (`-PsignReleaseWithDebugKey`) | Permanent. Do not regenerate it: every debug install would have to be uninstalled. |
| `temporary-release.keystore` | `passo-temporary` | `passo-temporary` / `passo-temporary` | Tag builds (`release.yml`) while the real release key does not exist yet | **Temporary.** Delete it once the real key is in GitHub Secrets. |

Committing the debug key means debug APKs from CI and from any machine share one signature and
update each other in place, the same arrangement as Chiaro.

SHA-256 certificate fingerprints:

- `debug.keystore`: `6E:41:A3:9E:EE:98:75:3B:4C:7B:F2:D9:B6:56:61:54:39:28:EA:C6:55:50:2A:A2:4B:80:04:5F:FA:FE:1F:D1`
- `temporary-release.keystore`: `86:60:62:84:81:FD:94:37:C3:9B:9B:09:F7:A4:2D:61:36:5F:5F:86:73:B7:34:BF:14:57:A9:7A:58:3D:93:32`

## The real release key

It must **never** enter the repo (`.gitignore` refuses `*.jks`, `*.keystore`, `*.p12` outside
the two files above). It signs every release for the life of the app, and a future Google Play
listing must reuse it (PLANNING.md §11 Phase 9), so losing it means users cannot update without
uninstalling. Keep it outside the repo, with an offline backup.

### Creating it and retiring the temporary key

1. Generate it, outside the repo:

   ```sh
   keytool -genkeypair -v -keystore passo-release.jks -storetype PKCS12 \
     -alias passo -keyalg RSA -keysize 4096 -validity 10000 \
     -dname "CN=Fiorenzo Brioni, O=callbackdev, C=IT"
   ```

2. Back it up offline, with its passwords.
3. Add four repository secrets (Settings, Secrets and variables, Actions):
   - `KEYSTORE_BASE64`: `base64 -w0 passo-release.jks`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`: `passo`
   - `KEY_PASSWORD`
4. From then on `release.yml` signs with it automatically. Retire the stand-in:
   - delete `keystore/temporary-release.keystore` and its row in the table above;
   - in `release.yml`, remove the fallback branch in "Prepare the release keystore" (make a
     missing secret an error), the `temporary` expressions and the warning in the notes;
   - drop its `!keystore/temporary-release.keystore` line from `.gitignore`;
   - publish the new fingerprint in the README (`keytool -list -v -keystore passo-release.jks`).
5. Anyone who installed a build signed with the temporary key must uninstall it before
   installing one signed with the real key.

For local signed builds, put the four properties in `~/.gradle/gradle.properties`:

```properties
PASSO_KEYSTORE=/absolute/path/to/passo-release.jks
PASSO_KEYSTORE_PASSWORD=...
PASSO_KEY_ALIAS=passo
PASSO_KEY_PASSWORD=...
```
