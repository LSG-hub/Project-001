"""Talk to Gemini Live on Vertex AI once, through the same wrapper the service uses.

    uv run python scripts/live_smoke.py "Say hello in Hindi."

Uses Application Default Credentials. `--gcloud-config personal` signs in with that
gcloud configuration's account instead (handy when ADC is a different account).
"""

import argparse
import asyncio
import os
import subprocess
import time

from google.oauth2.credentials import Credentials

from project001_api.adapters.gemini_live import GeminiLive
from project001_api.adapters.vertex import LazyGenaiClient
from project001_api.core.settings import Settings
from project001_api.core.voice import OUTPUT_RATE, EventKind


def gcloud_credentials(config: str) -> Credentials:
    token = subprocess.run(
        ["gcloud", "auth", "print-access-token"],  # noqa: S607
        env={**os.environ, "CLOUDSDK_ACTIVE_CONFIG_NAME": config},
        capture_output=True,
        text=True,
        check=True,
    ).stdout.strip()
    return Credentials(token)  # type: ignore[no-untyped-call]


async def main(prompt: str, config: str | None) -> None:
    settings = Settings()
    credentials = gcloud_credentials(config) if config else None
    client = LazyGenaiClient(settings.gcp_project, settings.gcp_region, credentials)
    assistant = GeminiLive(client, settings.live_model, settings.live_voice)

    started = time.perf_counter()
    first_audio: float | None = None
    audio_bytes = 0
    said: list[str] = []
    instructions = "You answer phone calls for the owner of this phone. Keep replies short."
    async with assistant.connect(instructions, tools=[]) as call:
        await call.send_text(prompt)
        async for event in call.events():
            if event.kind is EventKind.AUDIO and event.audio:
                first_audio = first_audio or time.perf_counter()
                audio_bytes += len(event.audio)
            elif event.kind is EventKind.ASSISTANT_SAID and event.text:
                said.append(event.text)
            elif event.kind is EventKind.TURN_COMPLETE:
                break

    seconds_of_audio = audio_bytes / 2 / OUTPUT_RATE
    print(f"model: {settings.live_model} in {settings.gcp_region}")
    if first_audio:
        print(f"first audio after {first_audio - started:.2f}s, {seconds_of_audio:.1f}s of speech")
    print("assistant said:", "".join(said).strip())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("prompt", nargs="?", default="Hello? Who is this?")
    parser.add_argument("--gcloud-config")
    args = parser.parse_args()
    asyncio.run(main(args.prompt, args.gcloud_config))
