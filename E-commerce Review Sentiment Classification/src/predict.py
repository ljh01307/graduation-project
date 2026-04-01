import os
import argparse
from typing import List, Optional, Tuple

import joblib
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer

from src.preprocess import preprocess_single
from src.utils import get_model_path


class SentimentPredictor:
    """情感分析预测器类"""
    
    def __init__(self, model_path: str = None, vectorizer_path: str = None):
        """
        初始化预测器
        
        Args:
            model_path: 模型文件路径，默认为None时使用默认路径
            vectorizer_path: 向量化器文件路径，默认为None时使用默认路径
        """
        if model_path is None:
            model_path = get_model_path("best_svm_model.pkl")
        if vectorizer_path is None:
            vectorizer_path = get_model_path("tfidf_vectorizer.pkl")
        
        self.model = self._load_model(model_path)
        self.vectorizer = self._load_vectorizer(vectorizer_path)
    
    def _load_model(self, model_path: str):
        """加载模型"""
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"模型文件不存在: {model_path}")
        return joblib.load(model_path)
    
    def _load_vectorizer(self, vectorizer_path: str) -> TfidfVectorizer:
        """加载向量化器"""
        if not os.path.exists(vectorizer_path):
            raise FileNotFoundError(f"向量化器文件不存在: {vectorizer_path}")
        return joblib.load(vectorizer_path)
    
    def predict(self, text: str) -> Tuple[str, int, float]:
        """
        预测单条文本的情感
        
        Args:
            text: 待预测的文本
        
        Returns:
            (情感标签, 标签数值, 置信度)
        """
        # 预处理
        seg_text = preprocess_single(text)
        
        # 向量化
        vec = self.vectorizer.transform([seg_text])
        
        # 预测
        pred = self.model.predict(vec)[0]
        
        # 置信度（SVM需要用decision_function）
        if hasattr(self.model, 'decision_function'):
            confidence = self.model.decision_function(vec)[0]
            # 归一化到0-1之间
            confidence = 1 / (1 + np.exp(-confidence))
        else:
            confidence = 0.5
        
        sentiment = "好评" if pred == 1 else "差评"
        return sentiment, int(pred), float(confidence)
    
    def predict_batch(self, texts: List[str]) -> List[dict]:
        """
        批量预测文本情感
        
        Args:
            texts: 待预测的文本列表
        
        Returns:
            预测结果列表，每个元素包含text, sentiment, label, confidence
        """
        results = []
        for text in texts:
            sentiment, label, confidence = self.predict(text)
            results.append({
                'text': text,
                'sentiment': sentiment,
                'label': label,
                'confidence': confidence
            })
        return results


def predict_text(text: str, model_path: str = None, vectorizer_path: str = None) -> dict:
    """
    预测单条文本的情感（便捷函数）
    
    Args:
        text: 待预测的文本
        model_path: 模型文件路径
        vectorizer_path: 向量化器文件路径
    
    Returns:
        预测结果字典
    """
    predictor = SentimentPredictor(model_path, vectorizer_path)
    sentiment, label, confidence = predictor.predict(text)
    
    return {
        'text': text,
        'sentiment': sentiment,
        'label': label,
        'confidence': confidence
    }


def predict_texts(texts: List[str], model_path: str = None, vectorizer_path: str = None) -> List[dict]:
    """
    批量预测文本情感（便捷函数）
    
    Args:
        texts: 待预测的文本列表
        model_path: 模型文件路径
        vectorizer_path: 向量化器文件路径
    
    Returns:
        预测结果列表
    """
    predictor = SentimentPredictor(model_path, vectorizer_path)
    return predictor.predict_batch(texts)


def main():
    """主函数，用于命令行预测"""
    parser = argparse.ArgumentParser(description="电商评论情感分类 - 预测脚本")
    parser.add_argument(
        "--text",
        type=str,
        default=None,
        help="待预测的文本",
    )
    parser.add_argument(
        "--model_path",
        type=str,
        default=None,
        help="模型文件路径",
    )
    parser.add_argument(
        "--vectorizer_path",
        type=str,
        default=None,
        help="向量化器文件路径",
    )
    
    args = parser.parse_args()
    
    if args.text:
        result = predict_text(args.text, args.model_path, args.vectorizer_path)
        print(f"\n预测结果:")
        print(f"文本: {result['text']}")
        print(f"情感: {result['sentiment']}")
        print(f"标签: {result['label']}")
        print(f"置信度: {result['confidence']:.4f}")
    else:
        # 测试几个例子
        test_reviews = [
            "这个产品质量很好，非常满意！",
            "东西太差了，用了一次就坏了",
            "性价比一般，没有想象中好",
            "发货很快，包装完好",
            "客服态度很差，再也不来了"
        ]
        
        print("="*60)
        print("批量预测测试")
        print("="*60)
        
        results = predict_texts(test_reviews, args.model_path, args.vectorizer_path)
        for result in results:
            print(f"\n文本: {result['text']}")
            print(f"情感: {result['sentiment']}")
            print(f"置信度: {result['confidence']:.4f}")


if __name__ == "__main__":
    main()
