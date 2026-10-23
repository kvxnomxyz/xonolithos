# Xstore catalog format, version 1

Repository: **`kvxnomxyz/xonolithos`**. Branch: **`appstore`**. It must be publicly readable
for this initial client; no GitHub token or Bitpost account is embedded.

Each app is a direct child folder of the branch root:

```text
appstore branch
└── example-app/
    ├── title.txt
    ├── author.txt
    ├── description.txt
    ├── apk.txt          # optional HTTPS download URL
    ├── sha256.txt       # required to enable installation
    └── package.txt      # required to enable installation
```

The three display files are the format requested for xOS. They contain UTF-8 plain text;
HTML and Markdown are displayed as text, never executed. An app folder may contain only
these three files; it will be browsable with installation disabled.

Installation needs an APK source, a 64-character SHA-256 digest and its Android package name.
These additional text fields are an initial implementation choice:

- `apk.txt`: a single HTTPS URL to a complete, standalone APK (not a split APK bundle).
- Alternatively, include `app.apk` inside the folder. `apk.txt` takes precedence if both exist.
- `sha256.txt`: the APK's hexadecimal SHA-256, with no filename or comment.
- `package.txt`: the exact Android application ID, for example `org.example.notes`.

The app obtains a branch commit first and reads all catalog files at that fixed revision.
An HTTPS redirect cannot downgrade to HTTP. APKs are limited to 512 MiB; SHA-256 and the
APK package name are checked before handing the file to Android's package installer.
Android prompts for “install unknown apps” permission and installation confirmation.
Xstore never silently installs or grants runtime permissions.

Repository control is the catalog's trust root. The checksum stops corruption or a changed
download; it does not protect against someone who can change both the APK URL and checksum
in the catalog. Existing-app updates remain subject to Android's signing-certificate checks.
Before public store operation, add a reviewed publisher/submission policy and decide whether
an independently signed catalog is needed.

Limits for the initial client: 100 catalog apps; folder names contain ASCII letters, digits,
dot, underscore or hyphen, start with a letter/digit and are at most 80 characters. Titles and
authors are at most 512 UTF-8 bytes each; descriptions at most 32 KiB. Nested app folders and
Git LFS pointer files are not interpreted as APKs. Prefer release-asset URLs for larger APKs.

The catalog is cached locally. Automatic refresh occurs when the cache is more than one
hour old; manual refresh is available. Network/permission/rate-limit errors retain the last
successful list. Empty and unpublished catalogs have explicit UI states.

Validate a local checkout before publishing:

```bash
python3 scripts/validate-catalog.py /path/to/appstore-checkout
```

No appstore branch has been created or published by this build task. Metadata-only sample
files are provided under `examples/appstore/example-app/`; they are documentation, not a
real catalog entry or an installable package.
