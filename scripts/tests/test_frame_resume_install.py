"""Exercise the install helper without contacting or changing an Android device."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('install_resume', ROOT / 'apps/frame-resume/scripts/install_resume.py')
INSTALL = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(INSTALL)


class FrameResumeInstallTests(unittest.TestCase):
    def invoke(self, installed, apply=True, config=None, installation_succeeds=True):
        commands = []

        def fake_run(args, **kwargs):
            commands.append(args)
            remote = args[-1]
            if 'install' in args:
                output = 'Success' if installation_succeeds else 'Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]'
            elif remote.startswith('pm list packages'):
                output = '\n'.join('package:' + x for x in installed) + '\n'
            elif remote.startswith('settings get secure screensaver_components'):
                output = INSTALL.PACKAGE + '/' + INSTALL.CLASS + 'FrameDreamService\n'
            elif remote.startswith('am broadcast'):
                output = 'Broadcast completed: result=0\n'
            else:
                output = ''
            return subprocess.CompletedProcess(args, 0, output, '')

        with tempfile.TemporaryDirectory() as temp:
            apk = Path(temp) / 'Frame-Resume.apk'
            apk.write_bytes(b'test fixture, never installed')
            args = ['install_resume.py', '--serial', 'TEST_DEVICE', '--apk', str(apk)]
            if apply:
                args.append('--apply')
            if config is not None:
                cfg = Path(temp) / 'config.json'
                cfg.write_text(json.dumps(config))
                args += ['--config', str(cfg)]
            with patch.object(sys, 'argv', args), patch.object(INSTALL.subprocess, 'run', side_effect=fake_run), contextlib.redirect_stdout(io.StringIO()):
                INSTALL.main()
        return commands

    def test_plan_does_not_contact_device(self):
        self.assertEqual([], self.invoke([], apply=False))

    def test_new_install_does_not_confuse_new_package_with_original(self):
        commands = self.invoke([INSTALL.PACKAGE])
        remote = [c[-1] for c in commands if 'shell' in c]
        self.assertFalse(any('com.portalhacks.frame/com.portalhacks.frame.ConfigReceiver' in c for c in remote))
        self.assertTrue(any(INSTALL.PACKAGE + '/' + INSTALL.CLASS + 'ConfigReceiver --ez guard true' in c for c in remote))

    def test_existing_guard_stops_before_new_guard_starts(self):
        commands = self.invoke(['com.portalhacks.frame', INSTALL.PACKAGE])
        remote = [c[-1] for c in commands if 'shell' in c]
        old = next(i for i, c in enumerate(remote) if 'com.portalhacks.frame/com.portalhacks.frame.ConfigReceiver --ez guard false' in c)
        new = next(i for i, c in enumerate(remote) if INSTALL.PACKAGE + '/' + INSTALL.CLASS + 'ConfigReceiver --ez guard true' in c)
        self.assertLess(old, new)
        self.assertFalse(any('uninstall' in c or 'pm clear' in c for c in remote))

    def test_unrecognized_album_is_rejected_before_device_access(self):
        with self.assertRaises(ValueError):
            self.invoke([], config={'albums': [{'url': 'https://example.invalid/private'}]})

    def test_failed_install_stops_configuration(self):
        with self.assertRaisesRegex(RuntimeError, 'APK installation failed'):
            self.invoke([], installation_succeeds=False)


if __name__ == '__main__':
    unittest.main()
