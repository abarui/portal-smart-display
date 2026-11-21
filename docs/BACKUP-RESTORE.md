# Private backup and restore

The public repository contains source and generic instructions, **not anyone's device backup**. Keep backups outside Git. Calendar subscriptions, account tokens and keys can be included in the app's configuration export even though they are not human-readable at first glance: its `PORTALHUB1:` payload is base64, not encryption.

## What the configuration export includes

- The app's `config` preferences: calendar feeds, display/layout settings, weather location, optional integration credentials and other main settings.
- Existing family-data JSON files such as members, lists, chores, routines, recipes and meal plans.
- No photos or camera-gallery files.

It does **not** include Frame or Frame Resume's private configuration/uploads/bookmarks, Transistor's station collection, radio favorites/volume, the separate shopping-link token, third-party sign-ins, Android settings, signing keys, or a firmware image. Short-lived cached access tokens are also skipped. A restore after a reset may require new phone QR links and account sign-ins.

## Export over USB

With the app running and the intended device authorized:

```bash
python3 scripts/config-backup.py export --serial YOUR_SERIAL --file private/portal-config.txt
```

The helper creates a temporary ADB forward to the loopback-only server, reads `/api/export`, validates the bundle, saves it with owner-only permissions and removes the forward. It refuses to overwrite an existing file. It never downloads photos. Use a new filename for each checkpoint.

Save your own signed installer, release signing key (if you build the app) and a separate note of the Home/screensaver settings. Use Transistor's own collection export for stations. Keep copies on a private backup device as appropriate.

## Restore deliberately

Inspect the restore plan first; this does not contact the device:

```bash
python3 scripts/config-backup.py restore --file private/portal-config.txt
```

To apply to an already installed/running compatible build:

```bash
python3 scripts/config-backup.py restore --serial YOUR_SERIAL --file private/portal-config.txt --apply
```

The helper first exports the current configuration into a new sibling safety-backup file. If that fails, it stops. It then imports the requested bundle and compares the saved settings and family files to a fresh export. The import replaces main preferences and the data files contained in the bundle. It does not delete unrelated files, install APKs or change firmware.

Afterwards verify calendar refresh, family data, theme/orientation, phone links, Radio, Frame and Home selection. A revoked calendar link cannot be repaired by restoring an old copy of that link. New firmware that removes ADB or sideloading may prevent restoration entirely. This is app configuration recovery, not a guaranteed device recovery mechanism.

## Frame Resume

Keep shared-album links and slideshow settings in a separate private file if needed; [the Frame Resume guide](FRAME-RESUME.md) includes a placeholder configuration and restore helper. The dashboard export does not capture those settings, uploads or the current resume queue. A normal APK update preserves app data; a fresh installation starts a new rotation. Public release downloads contain only generic source/installers and no personal recovery files.

## Sharing source changes

Keep `private/`, `backups/`, APK signing material, exported bundles, `.env` files and personal screenshots out of commits. The included `.gitignore` helps with common filenames but cannot recognize a secret pasted into a source file or renamed document. Review the actual staged diff before publishing.
