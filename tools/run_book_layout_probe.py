#!/usr/bin/env python3
"""Check every authored book in the real client, without generating a world."""
from pathlib import Path
import os
import subprocess
from run_forge_visual_probe import stop

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / 'run-book-client'


def main():
    CLIENT.mkdir(exist_ok=True)
    report = CLIENT / 'book-layout.txt'
    image = CLIENT / 'screenshots/prometheus-page-5.png'
    report.unlink(missing_ok=True)
    image.unlink(missing_ok=True)
    (CLIENT / 'options.txt').write_text(
        'version:3955\nonboardAccessibility:false\nshowMultiplayerWarning:false\n'
        'pauseOnLostFocus:false\nmaxFps:30\nvsync:false\nlang:en_us\n'
        'forceUnicodeFont:false\n'
    )
    env = {**os.environ, 'LIBGL_ALWAYS_SOFTWARE': '1', 'JAVA_TOOL_OPTIONS': '-Xmx2G'}
    client = None
    try:
        with (CLIENT / 'probe.log').open('w') as log:
            client = subprocess.Popen(
                ['xvfb-run', '-a', '-s', '-screen 0 1280x720x24', './gradlew',
                 '--no-daemon', '--console=plain', '--max-workers=2',
                 '-Dorg.gradle.jvmargs=-Xmx512m', 'runBookLayoutClient'],
                cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT,
                start_new_session=True,
            )
            result = client.wait(timeout=480)
        if result or not report.exists() or not image.exists() or 'ELYSIUM_BOOK_LAYOUT_COMPLETE' not in (CLIENT / 'probe.log').read_text():
            raise RuntimeError('Book layout failed; inspect run-book-client/probe.log and book-layout.txt')
        print(report.read_text())
    finally:
        stop(client)


if __name__ == '__main__':
    main()
