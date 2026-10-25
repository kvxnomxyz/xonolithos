# Xstore

Xstore browses the public `appstore` branch of `kvxnomxyz/xonolithos`. The app source is on `xstore`; an integrated copy also lives in `main/apps/Xstore`. Its interface uses a bright catalog header, search, availability filters, and clear installable/browse-only labels.

Each catalog entry is a folder with `title.txt`, `author.txt`, and `description.txt`. For installation, add a HTTPS `apk.txt` (or `app.apk`), `sha256.txt`, and `package.txt`. Xstore pins a branch revision while reading entries, verifies the downloaded APK hash and package ID, then hands installation to Android for confirmation. It does not silently install or contain a GitHub token.

See the complete [catalog contract](https://github.com/kvxnomxyz/xonolithos/blob/main/docs/XSTORE-CATALOG.md). Catalog control is a trust root: a checksum alone does not vet an app publisher. Submit catalog additions for review.
