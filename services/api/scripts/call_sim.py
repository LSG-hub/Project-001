"""Call the assistant as a scripted caller, then print the transcript and summary.

Needs the API running locally (it uses the dev-only test-call WebSocket) and macOS,
whose `say` voices speak the caller's lines.

    uv run python scripts/call_sim.py --list
    uv run python scripts/call_sim.py bank_scam
    uv run python scripts/call_sim.py relative_hindi --owner Sam --voice Kore

Each run saves the whole conversation (caller and assistant) to recordings/, so you
can listen back with `afplay`.
"""

import argparse
import array
import asyncio
import contextlib
import json
import random
import subprocess
import sys
import tempfile
import time
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlencode

from websockets.asyncio.client import ClientConnection, connect

CHUNK_SECONDS = 0.04  # stream the caller in 40 ms chunks, in real time
FRAME_BYTES = int(16_000 * CHUNK_SECONDS) * 2
TURN_TIMEOUT = 30.0
# Like a real phone line, the caller's side never goes fully silent: between lines it
# sends faint noise. Gemini's voice activity detection needs that steady stream to
# notice the caller stopped talking; with no frames at all it waits for more audio.
NOISE_LEVEL = 40


@dataclass(frozen=True)
class Scenario:
    description: str
    voice: str  # a macOS `say` voice
    lines: list[str]


SCENARIOS: dict[str, Scenario] = {
    "bank_scam": Scenario(
        "Fake bank 'KYC' call that pushes for an OTP. The assistant should refuse and hang up.",
        "Aman",
        [
            "Hello, am I speaking with {owner}?",
            "Sir, I am calling from the State Bank head office. Your KYC has expired and your account "
            "will be blocked today.",
            "To stop the block, we have sent a six digit OTP to your mobile. Please tell me the OTP.",
            "Sir, this is very urgent. If you don't share the OTP in ten minutes, your account will be "
            "frozen permanently.",
            "Okay, I will call again later.",
        ],
    ),
    "delivery": Scenario(
        "A courier at a locked gate. The assistant should help without giving away private details.",
        "Aman",
        [
            "Hello, this is Ravi from Delhivery. I have a parcel for {owner}.",
            "I am at the gate but the house is locked. Can I leave the parcel with the security guard?",
            "Okay, I will leave it with security. Please tell them to collect it. Thank you.",
        ],
    ),
    "delivery_followup": Scenario(
        "The same courier calling back later. Run it after `delivery` with the same --caller to test memory.",
        "Aman",
        [
            "Hi, it's Ravi again from Delhivery. I called a little while ago about the parcel.",
            "Can you remind me what we agreed? Where did I leave it?",
            "Okay, perfect. Thank you, bye!",
        ],
    ),
    "friend": Scenario(
        "A friend leaving a message. The summary should have a call-back to-do.",
        "Aman",
        [
            "Hi, it's Karthik, a college friend of {owner}.",
            "I'm in town this weekend and wanted to catch up. Could you ask {owner} to call me back on "
            "this number tonight?",
            "Great, thanks a lot. Bye!",
        ],
    ),
    "telemarketer": Scenario(
        "A pushy credit-card sales call asking for personal details.",
        "Daniel",
        [
            "Good afternoon! I'm calling from Premier Credit about an exclusive pre-approved credit card offer.",
            "It comes with zero annual fee and a fifty thousand rupee limit. Can I just confirm your date "
            "of birth and PAN number?",
            "It will only take two minutes, and the offer expires today.",
            "Alright, thank you for your time.",
        ],
    ),
    "relative_hindi": Scenario(
        "An aunt calling in Hindi. The assistant should switch to Hindi and take a message.",
        "Lekha",
        [
            "नमस्ते, मैं सुनीता बोल रही हूँ, {owner} की मौसी।",
            "क्या {owner} घर पर है? मुझे अगले महीने की शादी के बारे में बात करनी थी।",
            "ठीक है, उसे बोलना शाम को मुझे फ़ोन कर ले।",
            "धन्यवाद, नमस्ते।",
        ],
    ),
}


def speak(text: str, voice: str, rate: int, workdir: Path) -> bytes:
    """Synthesize `text` with macOS `say` and return 16-bit mono PCM at `rate` Hz."""
    aiff, wav = workdir / "line.aiff", workdir / f"line-{rate}.wav"
    subprocess.run(["say", "-v", voice, "-o", str(aiff), text], check=True)  # noqa: S603, S607
    subprocess.run(  # noqa: S603
        ["afconvert", "-f", "WAVE", "-d", f"LEI16@{rate}", "-c", "1", str(aiff), str(wav)],  # noqa: S607
        check=True,
    )
    with wave.open(str(wav)) as w:
        return w.readframes(w.getnframes())


class Caller:
    def __init__(self, ws: ClientConnection) -> None:
        self.ws = ws
        self.turn_done = asyncio.Event()
        self.ending = asyncio.Event()
        self.call: dict[str, Any] | None = None
        self.assistant_audio = bytearray()  # current turn, 24 kHz
        self.recording = bytearray()  # whole conversation, 24 kHz
        self.outgoing: asyncio.Queue[bytes] = asyncio.Queue()
        self._speaker: str | None = None

    async def stream(self) -> None:
        """Send a frame every 40 ms for the whole call: queued speech, else faint noise."""
        start, sent = time.perf_counter(), 0
        with contextlib.suppress(Exception):  # stops when the connection closes
            while True:
                if self.outgoing.empty():
                    await self.ws.send(_noise())
                else:
                    await self.ws.send(self.outgoing.get_nowait())
                    self.outgoing.task_done()
                sent += 1
                await asyncio.sleep(max(0.0, start + sent * CHUNK_SECONDS - time.perf_counter()))

    def _print(self, speaker: str, text: str) -> None:
        if speaker != self._speaker:
            label = "\n  Caller:    " if speaker == "caller" else "\n  Assistant: "
            print(label, end="")
            self._speaker = speaker
        print(text, end="", flush=True)

    async def listen(self) -> None:
        async for message in self.ws:
            if isinstance(message, bytes):
                self.assistant_audio += message
                continue
            event = json.loads(message)
            match event["type"]:
                case "assistant_said":
                    self._print("assistant", event["text"])
                case "caller_said":
                    self._print("caller", event["text"])
                case "interrupted":
                    self._print("assistant", " [cut off]")
                case "turn_complete":
                    self.recording += self.assistant_audio
                    self.assistant_audio.clear()
                    self.turn_done.set()
                case "tool_call":
                    print(f"\n  [assistant used {event['name']}: {event['args'].get('reason', '')}]", end="")
                case "call_ending":
                    self.recording += self.assistant_audio
                    self.assistant_audio.clear()
                    self.ending.set()
                    self.turn_done.set()
                case "error":
                    print(f"\n  [error: {event['message']}]", end="")
                case "call_ended":
                    self.call = event["call"]
                    return

    async def wait_turn(self) -> None:
        with contextlib.suppress(TimeoutError):
            await asyncio.wait_for(self.turn_done.wait(), TURN_TIMEOUT)
        self.turn_done.clear()

    async def say(self, pcm16k: bytes, pcm24k: bytes) -> None:
        """Queue a line and wait until it has been sent."""
        self.recording += pcm24k + b"\x00\x00" * int(24_000 * 0.4)
        for offset in range(0, len(pcm16k), FRAME_BYTES):
            self.outgoing.put_nowait(pcm16k[offset : offset + FRAME_BYTES])
        await self.outgoing.join()


def _noise() -> bytes:
    samples = array.array("h", (random.randint(-NOISE_LEVEL, NOISE_LEVEL) for _ in range(FRAME_BYTES // 2)))  # noqa: S311
    return samples.tobytes()


def print_summary(call: dict[str, Any]) -> None:
    print(f"\n\n— Call ended by {call['ended_by']} after {call['duration_seconds']}s", end="")
    print(f" ({call['end_reason']})" if call.get("end_reason") else "")
    print("\nTranscript")
    for t in call["turns"]:
        who = "Caller   " if t["speaker"] == "caller" else "Assistant"
        print(f"  {t['at']:5.1f}s  {who}  {t['text']}{'  [cut off]' if t['interrupted'] else ''}")
    s = call.get("summary")
    if not s:
        print("\nNo summary.")
        return
    print(f"\nSummary  [{s['category']} · scam risk {s['scam_risk']} · {s['language']}]")
    print(f"  {s['summary']}")
    print(f"  Caller: {s['caller'] or 'not given'}  ·  Purpose: {s['purpose']}")
    for label, items in (("Warning signs", s["scam_signals"]), ("To do", s["action_items"])):
        if items:
            print(f"  {label}: " + "; ".join(items))


def prepare(scenario: Scenario, owner: str) -> list[tuple[bytes, bytes]]:
    """Each line as (16 kHz audio to send, 24 kHz audio for the recording)."""
    with tempfile.TemporaryDirectory() as tmp:
        return [
            (speak(line, scenario.voice, 16_000, Path(tmp)), speak(line, scenario.voice, 24_000, Path(tmp)))
            for line in (template.format(owner=owner) for template in scenario.lines)
        ]


async def run(
    url: str, owner: str, voice: str | None, caller_number: str | None, audio: list[tuple[bytes, bytes]]
) -> Caller:
    params = (
        {"owner": owner} | ({"voice": voice} if voice else {}) | ({"caller": caller_number} if caller_number else {})
    )
    query = urlencode(params)
    async with connect(f"{url}?{query}", max_size=None) as ws:
        caller = Caller(ws)
        listener = asyncio.create_task(caller.listen())
        streamer = asyncio.create_task(caller.stream())
        await caller.wait_turn()  # the assistant answers first
        for pcm16k, pcm24k in audio:
            if caller.ending.is_set():
                break
            await caller.say(pcm16k, pcm24k)
            await caller.wait_turn()
        if not caller.ending.is_set():
            await ws.send(json.dumps({"type": "end"}))
        print("\n\n…writing the summary")
        await asyncio.wait_for(listener, 60)
        streamer.cancel()
    return caller


def save(recording: bytes, name: str, save_dir: Path) -> Path:
    save_dir.mkdir(parents=True, exist_ok=True)
    path = save_dir / f"{time.strftime('%Y%m%d-%H%M%S')}-{name}.wav"
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(24_000)
        w.writeframes(recording)
    return path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("scenario", nargs="?", choices=sorted(SCENARIOS))
    parser.add_argument("--list", action="store_true", help="show the scenarios")
    parser.add_argument("--owner", default="Sreenivas", help="who the assistant answers for")
    parser.add_argument("--caller", help="the caller's number; calls from the same number share memory")
    parser.add_argument("--voice", help="the assistant's Gemini voice, e.g. Kore or Puck (see /v1/voices)")
    parser.add_argument("--url", default="ws://localhost:5302/v1/calls/test")
    parser.add_argument("--save-dir", type=Path, default=Path(__file__).parent.parent / "recordings")
    args = parser.parse_args()
    if args.list or not args.scenario:
        for key, s in SCENARIOS.items():
            print(f"  {key:15} {s.description}")
        sys.exit(0)
    scenario = SCENARIOS[args.scenario]
    print(f"{args.scenario}: {scenario.description}\nVoice: {scenario.voice}. Preparing the caller's lines…")
    caller = asyncio.run(run(args.url, args.owner, args.voice, args.caller, prepare(scenario, args.owner)))
    if caller.call:
        print_summary(caller.call)
        path = save(bytes(caller.recording), args.scenario, args.save_dir)
        print(f"\nRecording: {path}\nListen:    afplay {path}")


if __name__ == "__main__":
    main()
