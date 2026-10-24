from pydantic import BaseModel, Field
from typing import Optional, Dict, Any
from uuid import UUID
from datetime import datetime, timezone

class ChatEvent(BaseModel):
    type: str
    actorId: str
    recipientId: Optional[str] = None
    previewText: Optional[str] = None
    previewAttachmentId: Optional[str] = None
    chatId: Optional[UUID] = None
    messageId: Optional[UUID] = None
    messageType: Optional[str] = None
    timestamp: Optional[Any] = None

class ModerationResult(BaseModel):
    label: str
    confidence: float
    scores: Dict[str, float]

class ModerationEvent(BaseModel):
    messageId: UUID
    chatId: UUID
    senderId: str
    label: str
    confidence: float
    action: str
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ"))
