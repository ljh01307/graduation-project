import os
import sys
from collections import Counter
from typing import List, Dict, Any

from flask import Flask, request, jsonify
from flask_cors import CORS

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.predict import SentimentPredictor
from src.preprocess import preprocess_for_wordcloud
from src.utils import get_model_path

# 创建Flask应用
app = Flask(__name__)
CORS(app)  # 允许跨域请求

# 全局预测器实例
predictor = None


def init_predictor():
    """初始化预测器"""
    global predictor
    print("正在加载模型...")
    try:
        predictor = SentimentPredictor()
        print("模型加载成功!")
    except Exception as e:
        print(f"模型加载失败: {e}")
        raise


@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'model': 'SVM',
        'message': '模型服务正常运行'
    })


@app.route('/predict', methods=['POST'])
def predict():
    """单条评论预测接口"""
    try:
        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({'error': '请提供text参数'}), 400
        
        text = data['text']
        
        # 使用预测器进行预测
        sentiment, label, confidence = predictor.predict(text)
        
        return jsonify({
            'code': 200,
            'text': text,
            'sentiment': sentiment,
            'label': label,
            'confidence': confidence
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/batch_predict', methods=['POST'])
def batch_predict():
    """批量评论预测接口"""
    try:
        data = request.get_json()
        if not data or 'texts' not in data:
            return jsonify({'error': '请提供texts参数'}), 400
        
        texts = data['texts']
        if not isinstance(texts, list):
            return jsonify({'error': 'texts必须为列表'}), 400
        
        # 批量预测
        results = predictor.predict_batch(texts)
        
        return jsonify({
            'code': 200,
            'total': len(results),
            'results': results
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/wordcloud', methods=['POST'])
def generate_wordcloud():
    """
    生成词云数据接口
    请求体格式:
    {
        "positive_texts": ["好评1", "好评2", ...],
        "negative_texts": ["差评1", "差评2", ...],
        "top_n": 50  # 可选，默认50
    }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请提供数据'}), 400
        
        # 获取参数
        positive_texts = data.get('positive_texts', [])
        negative_texts = data.get('negative_texts', [])
        top_n = data.get('top_n', 50)
        
        # 确保是列表类型
        if not isinstance(positive_texts, list) or not isinstance(negative_texts, list):
            return jsonify({'error': 'positive_texts和negative_texts必须为列表'}), 400
        
        # 处理正面评论
        positive_words = []
        for text in positive_texts:
            positive_words.extend(preprocess_for_wordcloud(text))
        
        # 处理负面评论
        negative_words = []
        for text in negative_texts:
            negative_words.extend(preprocess_for_wordcloud(text))
        
        # 统计词频
        positive_counter = Counter(positive_words)
        negative_counter = Counter(negative_words)
        
        # 转换为前端ECharts词云需要的格式
        positive_result = [
            {"name": word, "value": count}
            for word, count in positive_counter.most_common(top_n)
        ]
        
        negative_result = [
            {"name": word, "value": count}
            for word, count in negative_counter.most_common(top_n)
        ]
        
        return jsonify({
            'code': 200,
            'positive': positive_result,
            'negative': negative_result,
            'total_positive_words': len(positive_words),
            'total_negative_words': len(negative_words)
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/keyword_attribution', methods=['POST'])
def keyword_attribution():
    """
    关键词情感归因接口
    请求体格式:
    {
        "positive_texts": ["好评1", "好评2", ...],
        "negative_texts": ["差评1", "差评2", ...],
        "top_n": 15  # 可选，默认15
    }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请提供数据'}), 400
        
        positive_texts = data.get('positive_texts', [])
        negative_texts = data.get('negative_texts', [])
        top_n = data.get('top_n', 15)
        
        if not isinstance(positive_texts, list) or not isinstance(negative_texts, list):
            return jsonify({'error': 'positive_texts和negative_texts必须为列表'}), 400
        
        def extract_keywords_with_examples(texts: List[str]) -> List[Dict[str, Any]]:
            """提取关键词并找到包含该关键词的例句"""
            keyword_counter = Counter()
            keyword_examples = {}
            
            for text in texts:
                words = preprocess_for_wordcloud(text)
                keyword_counter.update(words)
                
                for word in words:
                    if word not in keyword_examples:
                        keyword_examples[word] = []
                    if len(keyword_examples[word]) < 3 and text not in keyword_examples[word]:
                        keyword_examples[word].append(text)
            
            result = []
            for keyword, count in keyword_counter.most_common(top_n):
                result.append({
                    'keyword': keyword,
                    'count': count,
                    'examples': keyword_examples.get(keyword, [])[:3]
                })
            return result
        
        positive_keywords = extract_keywords_with_examples(positive_texts)
        negative_keywords = extract_keywords_with_examples(negative_texts)
        
        return jsonify({
            'code': 200,
            'positive_keywords': positive_keywords,
            'negative_keywords': negative_keywords
        })
        
    except Exception as e:
        import traceback
        print(f"关键词归因错误: {e}")
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500


def test_service():
    """测试服务功能"""
    print("\n" + "="*60)
    print("模型服务本地测试")
    print("="*60)
    
    # 测试预测功能
    test_cases = [
        "这件衣服质量很好，非常满意！",
        "东西太差了，用了一次就坏了",
        "性价比一般，没有想象中好",
        "发货很快，包装完好",
        "客服态度很差，再也不来了"
    ]
    
    for text in test_cases:
        sentiment, label, confidence = predictor.predict(text)
        print(f"评论: {text}")
        print(f"预测: {sentiment}")
        print()
    
    # 词云功能测试
    print("\n--- 词云功能测试 ---")
    test_positive = [
        "质量很好，面料舒适",
        "发货很快，包装完好",
        "性价比很高",
        "客服态度很好"
    ]
    test_negative = [
        "质量太差，洗一次就坏",
        "发货很慢，等了一周",
        "客服态度恶劣",
        "尺码偏小"
    ]
    
    # 处理正面评论
    positive_words = []
    for text in test_positive:
        positive_words.extend(preprocess_for_wordcloud(text))
    
    # 处理负面评论
    negative_words = []
    for text in test_negative:
        negative_words.extend(preprocess_for_wordcloud(text))
    
    positive_counter = Counter(positive_words)
    negative_counter = Counter(negative_words)
    
    print("\n正面评论词频统计:")
    for word, count in positive_counter.most_common(5):
        print(f"  {word}: {count}")
    
    print("\n负面评论词频统计:")
    for word, count in negative_counter.most_common(5):
        print(f"  {word}: {count}")


if __name__ == '__main__':
    # 初始化预测器
    init_predictor()
    
    # 测试服务
    test_service()
    
    # 启动服务
    print("\n" + "="*60)
    print("启动情感分析模型服务")
    print("="*60)
    print("接口地址: http://127.0.0.1:5000")
    print("健康检查: http://127.0.0.1:5000/health")
    print("单条预测: POST http://127.0.0.1:5000/predict")
    print("批量预测: POST http://127.0.0.1:5000/batch_predict")
    print("词云生成: POST http://127.0.0.1:5000/wordcloud")
    print("关键词归因: POST http://127.0.0.1:5000/keyword_attribution")
    print("="*60 + "\n")
    
    app.run(host='0.0.0.0', port=5000, debug=False)
