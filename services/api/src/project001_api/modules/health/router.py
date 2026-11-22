from typing import Any

from fastapi import APIRouter


def build_router(*, version: str, env: str) -> APIRouter:
    router = APIRouter(tags=["health"])

    @router.get("/health")
    async def health() -> dict[str, Any]:
        """Liveness. (Not /healthz: Cloud Run reserves paths ending in "z".) Touches nothing external."""
        return {"status": "ok", "version": version, "env": env}

    return router
