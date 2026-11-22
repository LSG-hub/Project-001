from typing import Annotated

from fastapi import APIRouter, Depends, Response, status

from project001_api.core.security import CurrentUid
from project001_api.modules.context.models import ExpectedCall, ExpectedCallIn, Profile, ProfileIn
from project001_api.modules.context.service import ContextService


def build_router(service: ContextService, current_owner: CurrentUid) -> APIRouter:
    router = APIRouter(prefix="/v1/context", tags=["context"])
    Owner = Annotated[str, Depends(current_owner)]

    @router.get("/profile")
    async def get_profile(owner: Owner) -> Profile:
        """What the assistant knows about you."""
        return await service.profile(owner)

    @router.put("/profile")
    async def put_profile(owner: Owner, body: ProfileIn) -> Profile:
        return await service.save_profile(owner, body)

    @router.get("/expected")
    async def list_expected(owner: Owner) -> list[ExpectedCall]:
        """Calls you're expecting that haven't happened yet."""
        return await service.expected(owner)

    @router.post("/expected", status_code=status.HTTP_201_CREATED)
    async def add_expected(owner: Owner, body: ExpectedCallIn) -> ExpectedCall:
        return await service.add_expected(owner, body)

    @router.delete("/expected/{expected_id}", status_code=status.HTTP_204_NO_CONTENT)
    async def remove_expected(owner: Owner, expected_id: str) -> Response:
        await service.remove_expected(owner, expected_id)
        return Response(status_code=status.HTTP_204_NO_CONTENT)

    return router
