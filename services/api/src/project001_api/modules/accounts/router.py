from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends
from pydantic import BaseModel

from project001_api.core.security import CurrentUid
from project001_api.modules.accounts.service import AccountService


class Me(BaseModel):
    uid: str
    phone: str
    created_at: datetime


def build_router(service: AccountService, current_uid: CurrentUid) -> APIRouter:
    router = APIRouter(prefix="/v1", tags=["accounts"])

    @router.get("/me")
    async def me(uid: Annotated[str, Depends(current_uid)]) -> Me:
        account = await service.get(uid)
        return Me(uid=account.uid, phone=account.phone, created_at=account.created_at)

    return router
