"""One-shot Gemini calls on Vertex AI that return structured data (e.g. call summaries)."""

from google.genai import types
from pydantic import BaseModel

from project001_api.adapters.vertex import LazyGenaiClient


class GeminiStructured:
    def __init__(self, client: LazyGenaiClient, model: str) -> None:
        self._client = client
        self._model = model

    async def generate[T: BaseModel](self, instructions: str, content: str, schema: type[T]) -> T:
        response = await self._client.get().aio.models.generate_content(
            model=self._model,
            contents=content,
            config=types.GenerateContentConfig(
                system_instruction=instructions,
                response_mime_type="application/json",
                response_schema=schema,
                temperature=0.2,
            ),
        )
        if isinstance(response.parsed, schema):
            return response.parsed
        return schema.model_validate_json(response.text or "{}")
