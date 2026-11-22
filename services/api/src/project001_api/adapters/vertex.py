"""Gemini on Vertex AI (billed to the Google Cloud project, so the credits apply).

On Cloud Run the client signs in as the service's runtime account. Locally it uses
Application Default Credentials (`gcloud auth application-default login`), or
credentials passed in (see scripts/live_smoke.py).
"""

from google import genai
from google.auth.credentials import Credentials


def make_genai_client(project: str, region: str, credentials: Credentials | None = None) -> genai.Client:
    # `enterprise=True` is the SDK's name for Vertex AI (formerly `vertexai=True`).
    return genai.Client(enterprise=True, project=project, location=region, credentials=credentials)


class LazyGenaiClient:
    """Creates the client on first use, so the app starts (and tests run) without credentials."""

    def __init__(self, project: str, region: str, credentials: Credentials | None = None) -> None:
        self._args = (project, region, credentials)
        self._client: genai.Client | None = None

    def get(self) -> genai.Client:
        if self._client is None:
            self._client = make_genai_client(*self._args)
        return self._client
