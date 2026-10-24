from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.config import settings
from app.ml.classifier import ToxicityClassifier
from app.kafka.producer import ModerationProducer
from app.kafka.consumer import ChatEventConsumer
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Global instances
classifier = ToxicityClassifier()
producer = ModerationProducer()
consumer = ChatEventConsumer(classifier, producer)

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(f"Starting {settings.APP_NAME}")
    
    # 1. Load ML Model
    classifier.load()
    
    # 2. Start Kafka Producer & Consumer
    await producer.start()
    await consumer.start()
    
    yield
    
    logger.info(f"Shutting down {settings.APP_NAME}")
    
    # 3. Stop Kafka Producer & Consumer
    await consumer.stop()
    await producer.stop()

app = FastAPI(title=settings.APP_NAME, lifespan=lifespan)

@app.get("/health")
async def health_check():
    return {"status": "up", "service": settings.APP_NAME, "model_loaded": classifier.model is not None}

