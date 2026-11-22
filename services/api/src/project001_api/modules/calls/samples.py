"""Voice samples: each voice performing the same short script, to compare voices fairly.

Gemini speaks in real time, so the first recording of a voice takes about as long as the
script; after that it comes from memory or (locally) disk instantly.
"""

import asyncio
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path

from project001_api.core.errors import AppError
from project001_api.core.voice import VOICES, EventKind, Voice
from project001_api.modules.calls import prompts
from project001_api.modules.calls.ports import VoiceAgent


@dataclass(frozen=True)
class VoiceSample:
    voice: str
    script: str
    # What Gemini actually said (its transcript), to check it kept to the script.
    said: str
    pcm24k: bytes
    language: str = "en"


class VoiceSamples:
    def __init__(self, agent: VoiceAgent, *, sample_dir: Path | None = None, default_voice: str | None = None) -> None:
        self._agent = agent
        self._default_voice = default_voice
        self._sample_dir = sample_dir
        self._samples: dict[str, VoiceSample] = {}
        self._sample_locks: dict[str, asyncio.Lock] = {}

    @staticmethod
    def voices() -> list[Voice]:
        return sorted(VOICES.values(), key=lambda v: (v.gender, v.name))

    @property
    def default_voice(self) -> str | None:
        return self._default_voice

    @staticmethod
    def check_voice(voice: str | None) -> str | None:
        if voice and voice not in VOICES:
            raise AppError("unknown_voice", f"No voice called {voice}.", status=422)
        return voice or None

    @staticmethod
    def sample_languages() -> list[prompts.SampleLanguage]:
        return list(prompts.SAMPLE_LANGUAGES.values())

    @staticmethod
    def check_language(language: str) -> str:
        if language not in prompts.SAMPLE_LANGUAGES:
            raise AppError("unknown_language", f"No sample script in {language}.", status=422)
        return language

    def sample_script(self, owner_name: str | None, language: str = "en", voice: str | None = None) -> str:
        return prompts.sample_script(owner_name, self.check_language(language), self._gender(voice))

    def _gender(self, voice: str | None) -> str:
        found = VOICES.get(voice or self._default_voice or "")
        return found.gender if found else "female"

    async def voice_sample(self, voice: str, owner_name: str | None, language: str = "en") -> VoiceSample:
        """`voice` performing the ~30 s sample script in `language`. Recorded once per voice and script.

        Gemini speaks in real time, so the first recording of a voice takes about as long
        as the script; after that it comes from memory or disk instantly.
        """
        self.check_voice(voice)
        script = self.sample_script(owner_name, language, voice)
        direction = prompts.voice_actor(language)
        key = f"{voice}-{language}-{hashlib.sha256((direction + script).encode()).hexdigest()[:10]}"
        async with self._sample_locks.setdefault(key, asyncio.Lock()):
            if key not in self._samples:
                sample = await asyncio.to_thread(self._read_sample, key)
                if sample is None:
                    sample = await self._record_sample(voice, language, direction, script)
                    await asyncio.to_thread(self._write_sample, key, sample)
                self._samples[key] = sample
        return self._samples[key]

    async def _record_sample(self, voice: str, language: str, direction: str, script: str) -> VoiceSample:
        pcm, said = bytearray(), []
        async with self._agent.connect(direction, [], voice=voice) as session:
            await session.send_text(f"Perform this script now:\n\n{script}")
            async for event in session.events():
                if event.kind is EventKind.AUDIO and event.audio:
                    pcm += event.audio
                elif event.kind is EventKind.ASSISTANT_SAID and event.text:
                    said.append(event.text)
                elif event.kind is EventKind.TURN_COMPLETE:
                    break
        return VoiceSample(voice, script, " ".join("".join(said).split()), bytes(pcm), language)

    def _read_sample(self, key: str) -> VoiceSample | None:
        if self._sample_dir is None:
            return None
        meta, audio = self._sample_dir / f"{key}.json", self._sample_dir / f"{key}.pcm"
        if not (meta.exists() and audio.exists()):
            return None
        info = json.loads(meta.read_text("utf-8"))
        return VoiceSample(info["voice"], info["script"], info["said"], audio.read_bytes(), info.get("language", "en"))

    def _write_sample(self, key: str, sample: VoiceSample) -> None:
        if self._sample_dir is None:
            return
        self._sample_dir.mkdir(parents=True, exist_ok=True)
        (self._sample_dir / f"{key}.pcm").write_bytes(sample.pcm24k)
        info = {"voice": sample.voice, "language": sample.language, "script": sample.script, "said": sample.said}
        (self._sample_dir / f"{key}.json").write_text(json.dumps(info, ensure_ascii=False, indent=1), "utf-8")
