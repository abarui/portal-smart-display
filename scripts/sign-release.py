#!/usr/bin/env python3
"""Sign an already-built release without embedding or printing private credentials."""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--keystore', type=Path, required=True)
    parser.add_argument('--password-file', type=Path, required=True)
    parser.add_argument('--alias', required=True)
    args = parser.parse_args()
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        parser.error('Set ANDROID_HOME to the Android SDK directory.')
    suffix = '.exe' if os.name == 'nt' else ''
    signer_suffix = '.bat' if os.name == 'nt' else ''
    tools = Path(sdk) / 'build-tools/36.0.0'
    unsigned = root / 'app/build/outputs/apk/release/app-release-unsigned.apk'
    if not all(p.is_file() for p in [unsigned, args.keystore, args.password_file]):
        parser.error('Build the release and provide existing private key/password files.')
    destination = root / 'dist'
    destination.mkdir(exist_ok=True)
    aligned = destination / 'aligned.apk'
    apk = destination / 'Portal-Family-Home.apk'
    subprocess.run([str(tools / ('zipalign' + suffix)), '-f', '4', str(unsigned), str(aligned)], check=True)
    subprocess.run([str(tools / ('apksigner' + signer_suffix)), 'sign', '--ks', str(args.keystore.resolve()),
                    '--ks-key-alias', args.alias, '--ks-pass', 'file:' + str(args.password_file.resolve()),
                    '--out', str(apk), str(aligned)], check=True)
    subprocess.run([str(tools / ('apksigner' + signer_suffix)), 'verify', '--verbose', str(apk)], check=True)
    with apk.open('rb') as f:
        sha = hashlib.file_digest(f, 'sha256').hexdigest()
    (destination / 'SHA256SUMS').write_text(sha + '  ' + apk.name + '\n')
    print('Signed APK and checksum saved in dist/. No device was modified.')


if __name__ == '__main__':
    main()
