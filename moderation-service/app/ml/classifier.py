import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from pyvi import ViTokenizer
from app.models.schemas import ModerationResult
from app.config import settings
import logging

logger = logging.getLogger(__name__)

class ToxicityClassifier:
    LABELS = ["CLEAN", "OFFENSIVE", "HATE"]
    
    def __init__(self, model_path: str = settings.MODEL_PATH):
        self.model_path = model_path
        self.tokenizer = None
        self.model = None
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        
    def load(self):
        logger.info(f"Loading model from {self.model_path} on {self.device}")
        try:
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_path)
            self.model = AutoModelForSequenceClassification.from_pretrained(self.model_path)
            self.model.to(self.device)
            self.model.eval()
            logger.info("Model loaded successfully.")
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise e

    def predict(self, text: str) -> ModerationResult:
        if not self.model or not self.tokenizer:
            raise RuntimeError("Model is not loaded. Call load() first.")
            
        # Tiền xử lý: Tách từ bằng pyvi
        segmented_text = ViTokenizer.tokenize(text)
        
        inputs = self.tokenizer(
            segmented_text, 
            return_tensors="pt", 
            truncation=True, 
            max_length=256
        ).to(self.device)
        
        with torch.no_grad():
            outputs = self.model(**inputs)
            
        probs = torch.softmax(outputs.logits, dim=-1)[0]
        label_idx = torch.argmax(probs).item()
        
        scores = {self.LABELS[i]: probs[i].item() for i in range(len(self.LABELS))}
        
        return ModerationResult(
            label=self.LABELS[label_idx],
            confidence=probs[label_idx].item(),
            scores=scores
        )
