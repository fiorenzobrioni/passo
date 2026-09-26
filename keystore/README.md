# Keystores

One keystore lives in this folder, committed **on purpose**. It is not secret: its passwords are
written below.

| File | Alias | Store / key password | What it signs |
|---|---|---|---|
| `debug.keystore` | `passo-debug` | `android` / `android` | Debug builds, and the debug-signed release APK CI builds for testing (`-PsignReleaseWithDebugKey`) |

Do not regenerate it: every debug install would have to be uninstalled. Committing it means
debug APKs from CI and from any machine share one signature and update each other in place, the
same arrangement as Chiaro.

SHA-256 certificate fingerprints:

- `debug.keystore`: `6E:41:A3:9E:EE:98:75:3B:4C:7B:F2:D9:B6:56:61:54:39:28:EA:C6:55:50:2A:A2:4B:80:04:5F:FA:FE:1F:D1`
- release key: `8B:40:22:8A:8D:EF:E3:E3:F1:6E:FE:1A:DC:C0:4C:C7:F5:B5:82:E4:18:F0:15:E8:27:B9:59:D5:BF:39:7F:B5` (the one published in the root README)

## The release key

It must **never** enter the repo (`.gitignore` refuses `*.jks`, `*.keystore`, `*.p12`, `*.pfx`
outside `debug.keystore`). It signs every release for the life of the app, and a future Google
Play listing must reuse it (PLANNING.md §11 Phase 9), so losing it means users cannot update
without uninstalling. It is kept outside the repo, with an offline backup.

### In GitHub Actions

`release.yml` reads it from four repository secrets (Settings, Secrets and variables, Actions),
the same names as Chiaro's:

- `KEYSTORE_BASE64`: the keystore file, `base64 -w0 passo-release.jks`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The workflow decodes the keystore into the runner's temp folder, passes the rest to Gradle as
`ORG_GRADLE_PROJECT_*` variables, and stops by name if a secret is missing. Nothing secret is
written to the logs or to the command line.

### Locally

Put the four properties in `~/.gradle/gradle.properties`:

```properties
PASSO_KEYSTORE=/absolute/path/to/passo-release.jks
PASSO_KEYSTORE_PASSWORD=...
PASSO_KEY_ALIAS=...
PASSO_KEY_PASSWORD=...
```

With all four set, `./gradlew :app:assembleRelease` signs with the release key; without them the
release build is unsigned, and `-PsignReleaseWithDebugKey` signs it with the debug key for
testing only.
