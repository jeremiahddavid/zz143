# Releasing zz143

## Prerequisites

- Sonatype Central account with `io.github.jeremiahddavid` namespace verified
- GPG signing key (ID: `D2930B1B`) uploaded to `keyserver.ubuntu.com`
- GitHub repository secrets configured (see below)

## GitHub Secrets Required

| Secret | Description |
|--------|-------------|
| `OSSRH_USERNAME` | Sonatype Central username (or token username) |
| `OSSRH_PASSWORD` | Sonatype Central password (or token password) |
| `SIGNING_KEY_ID` | GPG key short ID (e.g., `D2930B1B`) |
| `SIGNING_KEY` | Base64-encoded GPG private key (`gpg --export-secret-keys --armor \| base64`) |
| `SIGNING_PASSWORD` | GPG key passphrase (empty if no passphrase) |

## Release Process

### 1. Update version

```bash
# In gradle.properties
ZZ143_VERSION=0.2.0
```

### 2. Update CHANGELOG.md

Add a new section for the version with changes.

### 3. Commit and tag

```bash
git add -A
git commit -m "chore: release v0.2.0"
git tag -a v0.2.0 -m "Release 0.2.0 — description of changes"
git push origin main --tags
```

### 4. Verify

The `publish.yml` workflow triggers on tag push. Check:
- GitHub Actions: workflow should pass (tests + publish)
- Sonatype Central: artifacts should appear in staging
- Close and release the staging repository on central.sonatype.com

### 5. Verify on Maven Central

After release (may take 30min-2h to sync):
```
https://central.sonatype.com/artifact/io.github.jeremiahddavid/zz143-android
```

## Local Publishing (for testing)

```bash
./gradlew publishToMavenLocal
# Check: ~/.m2/repository/io/github/jeremiahddavid/
```

## Artifact Coordinates

```kotlin
implementation("io.github.jeremiahddavid:zz143-android:0.2.0")

// Individual modules (if needed):
implementation("io.github.jeremiahddavid:zz143-core:0.2.0")
implementation("io.github.jeremiahddavid:zz143-capture:0.2.0")
implementation("io.github.jeremiahddavid:zz143-learn:0.2.0")
implementation("io.github.jeremiahddavid:zz143-suggest:0.2.0")
implementation("io.github.jeremiahddavid:zz143-replay:0.2.0")
```
