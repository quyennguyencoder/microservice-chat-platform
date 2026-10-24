from aiokafka import AIOKafkaProducer
import json
import logging
from app.config import settings
from app.models.schemas import ModerationEvent

logger = logging.getLogger(__name__)

class ModerationProducer:
    def __init__(self):
        self.producer = None

    async def start(self):
        logger.info(f"Starting Kafka Producer at {settings.KAFKA_BOOTSTRAP_SERVERS}")
        self.producer = AIOKafkaProducer(
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        await self.producer.start()

    async def stop(self):
        if self.producer:
            await self.producer.stop()
            logger.info("Kafka Producer stopped.")

    async def publish_moderation_event(self, event: ModerationEvent):
        if not self.producer:
            raise RuntimeError("Producer not started")
        
        try:
            # Dùng chatId làm partition key để đảm bảo thứ tự
            key = str(event.chatId).encode('utf-8')
            await self.producer.send_and_wait(
                settings.KAFKA_TOPIC_MODERATION_EVENTS,
                value=event.model_dump(mode='json'),
                key=key
            )
            logger.info(f"Published moderation event for message {event.messageId}: {event.label}")
        except Exception as e:
            logger.error(f"Failed to publish moderation event: {e}")
