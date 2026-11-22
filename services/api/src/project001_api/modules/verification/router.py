from datetime import datetime

from fastapi import APIRouter, status
from pydantic import BaseModel, Field

from project001_api.modules.verification.models import Method, Status, Verification
from project001_api.modules.verification.service import Outcome, VerificationService


class StartRequest(BaseModel):
    phone: str = Field(min_length=3, max_length=32, examples=["+919876543210"])
    # ISO country ("IN") when `phone` has no +country code.
    region: str | None = Field(default=None, min_length=2, max_length=2)
    method: Method = Method.MISSED_CALL
    # BCP 47 language for the voice code ("hi-IN"); the phone's language by default.
    language: str | None = Field(default=None, max_length=16)


class StartResponse(BaseModel):
    id: str
    method: Method
    status: Status
    expires_at: datetime
    # Missed call: the number the phone should call.
    call_to: str | None = None


class CodeRequest(BaseModel):
    code: str = Field(pattern=r"^\d{6}$")


class VerificationResponse(BaseModel):
    id: str
    status: Status
    # Firebase custom token; present once, when the number is verified.
    token: str | None = None


def _response(outcome: Outcome) -> VerificationResponse:
    return VerificationResponse(id=outcome.verification.id, status=outcome.verification.status, token=outcome.token)


def build_router(service: VerificationService) -> APIRouter:
    router = APIRouter(prefix="/v1/verifications", tags=["verification"])

    @router.post("", status_code=status.HTTP_201_CREATED)
    async def start(body: StartRequest) -> StartResponse:
        v: Verification = await service.start(body.phone, body.region, body.method, body.language)
        return StartResponse(
            id=v.id,
            method=v.method,
            status=v.status,
            expires_at=v.expires_at,
            call_to=service.inbound_number if v.method is Method.MISSED_CALL else None,
        )

    @router.post("/{verification_id}/code")
    async def check_code(verification_id: str, body: CodeRequest) -> VerificationResponse:
        return _response(await service.check_code(verification_id, body.code))

    @router.get("/{verification_id}")
    async def get_status(verification_id: str) -> VerificationResponse:
        return _response(await service.status(verification_id))

    return router
