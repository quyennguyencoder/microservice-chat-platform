import sys
import os

# Đảm bảo in tiếng Việt không bị lỗi encoding trên Windows console
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

# Add parent directory to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.ml.classifier import ToxicityClassifier

def main():
    model_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "model"))
    print(f"Đang nạp mô hình từ: {model_dir}")
    
    classifier = ToxicityClassifier(model_path=model_dir)
    classifier.load()
    print("Nạp mô hình thành công!\n" + "="*50)
    
    # Danh sách câu test mẫu
    test_sentences = [
        "Chào bạn, hôm nay bạn thế nào?",
        "Dự án này làm tốt lắm, cảm ơn mọi người nhiều.",
        "Mày ngu như bò vậy, biến đi cho khuất mắt.",
        "Đồ rác rưởi hạ đẳng, cút đi.",
        "Thời tiết hôm nay đẹp thật, đi uống cà phê không?"
    ]
    
    for sentence in test_sentences:
        res = classifier.predict(sentence)
        print(f"Câu: \"{sentence}\"")
        print(f"  👉 Nhãn: {res.label} (Độ tin cậy: {res.confidence*100:.2f}%)")
        print(f"  📊 Chi tiết điểm: CLEAN: {res.scores.get('CLEAN',0)*100:.1f}%, OFFENSIVE: {res.scores.get('OFFENSIVE',0)*100:.1f}%, HATE: {res.scores.get('HATE',0)*100:.1f}%\n")

if __name__ == "__main__":
    main()
