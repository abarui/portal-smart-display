#!/usr/bin/env python3
"""Export or deliberately restore PortalHub configuration; never copies photos."""
import argparse
import base64
import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import uuid


def decode(data):
    prefix = b'PORTALHUB1:'
    data = data.strip()
    if not data.startswith(prefix):
        raise ValueError('Not a PortalHub configuration export.')
    root = json.loads(base64.b64decode(data[len(prefix):], validate=True))
    if root.get('v') != 1 or not isinstance(root.get('prefs'), dict) or not isinstance(root.get('files'), dict):
        raise ValueError('Unsupported or incomplete configuration export.')
    for entry in root['prefs'].values():
        if not isinstance(entry, dict) or 't' not in entry or 'v' not in entry:
            raise ValueError('Malformed preference in export.')
    for value in root['files'].values():
        json.loads(value)
    return root


def save_private(path, data):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    # O_EXCL prevents replacing an earlier restore point, including through a symlink.
    fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(fd, 'wb') as f:
        f.write(data)


class Portal:
    def __init__(self, serial, adb):
        self.command = [adb, '-s', serial]

    def call(self, *args):
        result = subprocess.run(self.command + list(args), capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError('ADB command failed. Check the device connection and authorization.')
        return result.stdout.strip()

    def request(self, route, data=None):
        port = self.call('forward', 'tcp:0', 'tcp:8090')
        try:
            req = urllib.request.Request('http://127.0.0.1:' + port + route, data=data,
                                         headers={'Content-Type': 'text/plain; charset=utf-8'})
            with urllib.request.urlopen(req, timeout=30) as response:
                return response.read()
        finally:
            self.call('forward', '--remove', 'tcp:' + port)


def restore(portal, path):
    target = path.read_bytes().strip()
    expected = decode(target)
    current = portal.request('/api/export')
    decode(current)
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    safety = path.with_name('before-restore-' + stamp + '-' + uuid.uuid4().hex[:8] + '.txt')
    save_private(safety, current)
    reply = json.loads(portal.request('/api/import', target))
    if reply.get('ok') is not True:
        raise RuntimeError('Import was not confirmed; keep the safety backup and inspect the device.')
    actual = decode(portal.request('/api/export'))
    for section in ('prefs', 'files'):
        if any(actual[section].get(k) != v for k, v in expected[section].items()):
            raise RuntimeError('Readback differs after import; inspect the device and keep the safety backup.')
    return safety


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['export', 'restore'])
    parser.add_argument('--serial')
    parser.add_argument('--adb', default=os.environ.get('ADB') or shutil.which('adb'))
    parser.add_argument('--file', type=Path, required=True)
    parser.add_argument('--apply', action='store_true', help='Actually import; restore otherwise prints a plan')
    args = parser.parse_args()
    if args.action == 'restore':
        bundle = decode(args.file.read_bytes())
        print(f"Restore payload: {len(bundle['prefs'])} settings and {len(bundle['files'])} family-data files.")
        if not args.apply:
            print('Plan only: an applied restore saves current configuration first, then replaces the captured settings/data.')
            print('No device contacted. No photos or third-party sign-ins are included.')
            return
    if not args.serial or not args.adb:
        parser.error('A device operation requires --serial and adb on PATH (or --adb).')
    portal = Portal(args.serial, args.adb)
    if args.action == 'export':
        if args.file.exists():
            parser.error('Output already exists. Choose a new backup filename.')
        data = portal.request('/api/export')
        bundle = decode(data)
        save_private(args.file, data)
        print(f"Saved {len(bundle['prefs'])} settings and {len(bundle['files'])} family-data files. No photos copied.")
    else:
        safety = restore(portal, args.file)
        print('Configuration restored and readback matched. Safety backup: ' + safety.name)
        print('Check the dashboard and reconnect phone links/accounts as needed.')


if __name__ == '__main__':
    main()
