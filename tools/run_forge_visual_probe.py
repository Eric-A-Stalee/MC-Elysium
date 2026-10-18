#!/usr/bin/env python3
"""Run a local-only disposable server and software-rendered Minecraft client for visual review."""
from pathlib import Path
import os
import signal
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / 'run-visual-server'
CLIENT = ROOT / 'run-visual-client'


def stop(process):
    if process is not None and process.poll() is None:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()


def main():
    for directory in (SERVER, CLIENT):
        directory.mkdir(exist_ok=True)
    if (SERVER / 'world').exists():
        raise RuntimeError('Use fresh run-visual-* directories for a repeatable dark-start review')
    (SERVER / 'eula.txt').write_text('eula=true\n')
    (SERVER / 'server.properties').write_text(
        'online-mode=false\nserver-ip=127.0.0.1\nserver-port=25565\n'
        'level-seed=0\ngenerate-structures=true\nview-distance=10\nsimulation-distance=4\n'
        'max-tick-time=-1\nspawn-protection=0\nenforce-secure-profile=false\n'
    )
    (CLIENT / 'options.txt').write_text(
        'version:3955\nonboardAccessibility:false\nshowMultiplayerWarning:false\n'
        'pauseOnLostFocus:false\nrenderDistance:10\nsimulationDistance:5\n'
        'maxFps:30\nvsync:false\ngamma:0.5\n'
    )
    args = ['./gradlew', '--no-daemon', '--console=plain', '--max-workers=2', '-Dorg.gradle.jvmargs=-Xmx512m']
    env = {**os.environ, 'LIBGL_ALWAYS_SOFTWARE': '1', 'JAVA_TOOL_OPTIONS': '-Xmx2G'}
    server = client = None
    try:
        with (SERVER / 'probe.log').open('w') as log:
            server = subprocess.Popen([*args, 'runForgeVisualServer'], cwd=ROOT, env=env,
                                      stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
            deadline = time.monotonic() + 480
            while time.monotonic() < deadline:
                if server.poll() is not None:
                    raise RuntimeError('Visual server stopped; inspect run-visual-server/probe.log')
                if 'ELYSIUM_FORGE_VISUAL_READY' in (SERVER / 'probe.log').read_text():
                    break
                time.sleep(2)
            else:
                raise TimeoutError('Visual server never completed its mountain placement')
            print('Natural generation and routes verified; starting real Minecraft client', flush=True)
            with (CLIENT / 'probe.log').open('w') as client_log:
                client = subprocess.Popen(['xvfb-run', '-a', '-s', '-screen 0 1280x720x24', *args, 'runForgeVisualClient'],
                                          cwd=ROOT, env=env, stdout=client_log, stderr=subprocess.STDOUT,
                                          start_new_session=True)
                result = client.wait(timeout=900)
            images = sorted((CLIENT / 'screenshots').glob('*.png'))
            expected = {'inhabitants': 9, 'cinder': 1, 'underworld': 6, 'prison': 4, 'bindings': 8}.get(os.environ.get('ELYSIUM_VISUAL_FOCUS'), 26)
            if result or len(images) != expected or 'ELYSIUM_FORGE_VISUAL_COMPLETE' not in (CLIENT / 'probe.log').read_text():
                raise RuntimeError(f'Visual client exit={result}, screenshots={len(images)}; inspect client probe.log')
            print(f'Captured {expected} actual vanilla-rendered review views:', *(p.name for p in images), sep='\n')
    finally:
        stop(client)
        stop(server)


if __name__ == '__main__':
    main()
