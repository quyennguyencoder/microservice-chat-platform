from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "Moderation Service"
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_TOPIC_CHAT_EVENTS: str = "chat-events"
    KAFKA_TOPIC_MODERATION_EVENTS: str = "moderation-events"
    KAFKA_CONSUMER_GROUP: str = "moderation-service-group"
    
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5433/moderation_db"
    
    MODEL_PATH: str = "./model"
    CONFIDENCE_THRESHOLD_OFFENSIVE: float = 0.50
    CONFIDENCE_THRESHOLD_HATE: float = 0.50
    
    class Config:
        env_file = ".env"

settings = Settings()
