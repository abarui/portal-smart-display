import base64
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('config_backup', Path(__file__).resolve().parents[1] / 'config-backup.py')
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)


def bundle(label):
    data = {'v': 1, 'prefs': {'theme': {'t': 's', 'v': label}}, 'files': {'lists.json': '[]'}}
    return b'PORTALHUB1:' + base64.b64encode(json.dumps(data).encode())


class BackupSafetyTests(unittest.TestCase):
    def test_never_overwrites_a_restore_point(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'backup.txt'
            backup.save_private(p, b'first')
            with self.assertRaises(FileExistsError):
                backup.save_private(p, b'second')
            self.assertEqual(p.read_bytes(), b'first')

    def test_invalid_bundle_stops_before_device_access(self):
        class UnavailableDevice:
            def request(self, *args):
                raise AssertionError('Invalid input must not contact the device')
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'bad.txt'
            p.write_bytes(b'invalid')
            with self.assertRaises(ValueError):
                backup.restore(UnavailableDevice(), p)

    def test_current_data_is_saved_before_import(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'target.txt'
            p.write_bytes(bundle('dark'))
            class Device:
                imported = False
                def request(self, route, data=None):
                    if route == '/api/import':
                        files = list(p.parent.glob('before-restore-*.txt'))
                        self_test.assertEqual(len(files), 1)
                        self_test.assertEqual(files[0].read_bytes(), bundle('light'))
                        self.imported = True
                        return b'{"ok":true}'
                    return bundle('dark' if self.imported else 'light')
            self_test = self
            safety = backup.restore(Device(), p)
            self.assertEqual(safety.read_bytes(), bundle('light'))

    def test_failed_safety_export_prevents_import(self):
        class Device:
            def request(self, route, data=None):
                if route != '/api/export':
                    raise AssertionError('Import must not happen after a failed safety export')
                raise RuntimeError('Device unavailable')
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'target.txt'
            p.write_bytes(bundle('dark'))
            with self.assertRaises(RuntimeError):
                backup.restore(Device(), p)


if __name__ == '__main__':
    unittest.main()
