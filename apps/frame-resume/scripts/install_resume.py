#!/usr/bin/env python3
"""Install Frame Resume alongside Frame. Default: display a plan without modifying a device."""
import argparse
import json
import shlex
import subprocess
from pathlib import Path

PACKAGE = 'com.portalhacks.frame.resume'
CLASS = 'com.portalhacks.frame.'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--apk', required=True, type=Path)
    parser.add_argument('--config', type=Path, help='Private JSON with albums and broadcast_booleans')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    config = json.loads(args.config.read_text()) if args.config else {}
    albums = config.get('albums', [])
    for album in albums:
        if not album['url'].startswith(('https://photos.app.goo.gl/', 'https://photos.google.com/share/',
                                         'https://www.icloud.com/sharedalbum/')):
            raise ValueError('Unrecognized album link; configuration not applied')
    print(f'Plan: install/update Frame Resume alongside Frame; connect {len(albums)} album links; '
          'select Frame Resume as the screensaver. No photos are copied or deleted.')
    if not args.apply:
        return
    if not args.apk.is_file():
        raise FileNotFoundError(args.apk)
    adb = [args.adb, '-s', args.serial]

    def command(*parts):
        result = subprocess.run(adb + list(parts), text=True, capture_output=True)
        if result.returncode:
            # Never echo commands or private album URLs on failure.
            raise RuntimeError('ADB operation failed; stopped before the next change')
        return result.stdout

    def shell(*parts):
        return command('shell', shlex.join(str(x) for x in parts))

    def broadcast(package=PACKAGE, *extras):
        output = shell('am', 'broadcast', '-n', package + '/' + CLASS + 'ConfigReceiver', *extras)
        if 'Broadcast completed:' not in output:
            raise RuntimeError('Configuration broadcast did not complete')

    output = command('install', '--no-incremental', '-r', str(args.apk.resolve()))
    if 'Success' not in output:
        raise RuntimeError('APK installation failed; original app preserved')
    shell('pm', 'grant', PACKAGE, 'android.permission.WRITE_SECURE_SETTINGS')
    shell('pm', 'grant', PACKAGE, 'android.permission.CAMERA')
    for album in albums:
        broadcast(PACKAGE, '--es', 'url', album['url'])
        broadcast(PACKAGE, '--es', 'enable_url' if album.get('enabled', True) else 'disable_url', album['url'])
    switches = {**config.get('broadcast_booleans', {}), 'resume': True}
    extras = []
    for key, value in switches.items():
        if key != 'guard':
            extras += ['--ez', key, str(bool(value)).lower()]
    extras += ['--el', 'delay_ms', str(config.get('delay_ms', 6000)),
               '--el', 'fade_ms', str(config.get('fade_ms', 500))]
    broadcast(PACKAGE, *extras)
    # Stop the original guard before enabling the new one; two guards would compete.
    if 'package:com.portalhacks.frame' in shell('pm', 'list', 'packages', 'com.portalhacks.frame').splitlines():
        broadcast('com.portalhacks.frame', '--ez', 'guard', 'false')
    broadcast(PACKAGE, '--ez', 'guard', 'true')
    component = shell('settings', 'get', 'secure', 'screensaver_components').strip()
    if component != PACKAGE + '/' + CLASS + 'FrameDreamService':
        raise RuntimeError('Screensaver selection did not match Frame Resume')
    shell('am', 'start', '-n', PACKAGE + '/' + CLASS + 'SlideshowComposeActivity')
    print('Frame Resume installed, album links configured, and screensaver selected.')


if __name__ == '__main__':
    main()
