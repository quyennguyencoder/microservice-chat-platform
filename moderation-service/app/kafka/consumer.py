import asyncio
import json
import logging
from aiokafka import AIOKafkaConsumer
from app.config import settings
from app.models.schemas import ChatEvent, ModerationEvent
from app.ml.classifier import ToxicityClassifier
from app.kafka.producer import ModerationProducer

logger = logging.getLogger(__name__)

class ChatEventConsumer:
    def __init__(self, classifier: ToxicityClassifier, producer: ModerationProducer):
        self.classifier = classifier
        self.producer = producer
        self.consumer = None
        self._task = None

    async def start(self):
        logger.info(f"Starting Kafka Consumer on {settings.KAFKA_TOPIC_CHAT_EVENTS}")
        self.consumer = AIOKafkaConsumer(
            settings.KAFKA_TOPIC_CHAT_EVENTS,
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            group_id=settings.KAFKA_CONSUMER_GROUP,
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )
        await self.consumer.start()
        self._task = asyncio.create_task(self.consume_loop())

    async def stop(self):
        if self._task:
            self._task.cancel()
        if self.consumer:
            await self.consumer.stop()
            logger.info("Kafka Consumer stopped.")

    async def consume_loop(self):
        try:
            async for msg in self.consumer:
                try:
                    event = ChatEvent(**msg.value)
                    await self.process_event(event)
                except Exception as e:
                    logger.error(f"Error processing message {msg.value}: {e}")
        except asyncio.CancelledError:
            pass
        except Exception as e:
            logger.error(f"Consumer loop error: {e}")

    async def process_event(self, event: ChatEvent):
        # Chỉ xử lý NEW_MESSAGE broadcast (recipientId = null)
        if event.type != "NEW_MESSAGE" or event.recipientId is not None:
            return
            
        # Chỉ kiểm duyệt tin nhắn TEXT có nội dung
        if event.messageType != "TEXT" or not event.previewText:
            return

        if not event.messageId or not event.chatId:
            return

        logger.info(f"Moderating message {event.messageId} from {event.actorId}: '{event.previewText}'")
        
        # Chạy model inference
        result = self.classifier.predict(event.previewText)
        clean_score = result.scores.get("CLEAN", 0.0)
        logger.info(f"Model result for '{event.previewText}': label={result.label} ({result.confidence*100:.1f}%), CLEAN={clean_score*100:.1f}%")
        
        # Kiểm tra ngưỡng (Thresholds)
        is_violation = False
        action = "NONE"
        
        if result.label == "HATE" and result.confidence >= settings.CONFIDENCE_THRESHOLD_HATE:
            is_violation = True
            action = "HIDDEN"
        elif result.label == "OFFENSIVE" and result.confidence >= settings.CONFIDENCE_THRESHOLD_OFFENSIVE:
            is_violation = True
            action = "FLAGGED"
        elif clean_score < 0.35: # Tổng điểm độc hại (OFFENSIVE + HATE) > 65%
            is_violation = True
            action = "FLAGGED"
            
        if is_violation:
            logger.info(f"🚨 VIOLATION DETECTED! Message {event.messageId} -> {result.label} (Action: {action})")
            mod_event = ModerationEvent(
                messageId=event.messageId,
                chatId=event.chatId,
                senderId=event.actorId,
                label=result.label,
                confidence=result.confidence,
                action=action
            )
            await self.producer.publish_moderation_event(mod_event)
        else:
            logger.info(f"✅ Message {event.messageId} PASSED moderation.")
