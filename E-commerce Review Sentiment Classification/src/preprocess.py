import re
import pandas as pd
import jieba
from typing import List, Set

from src.utils import load_stopwords

# 全局停用词集合
STOPWORDS = load_stopwords()


def clean_text(text: str) -> str:
    """
    文本清洗函数
    
    Args:
        text: 原始文本
    
    Returns:
        清洗后的文本
    """
    if pd.isna(text):
        return ''
    
    text = str(text)
    
    # 去除各类括号
    text = re.sub(r'[【\[\]（）()《》<>「」『』【】〔〕]', '', text)
    
    # 去除英文和数字
    text = re.sub(r'[a-zA-Z0-9]', '', text)
    
    # 去除特殊符号
    text = re.sub(r'[▼◆▶■●★☆◆◇○◎●△▲※→←↑↓]', '', text)
    
    # 去除空白字符
    text = re.sub(r'\s+', '', text)
    
    return text


def cut_words(text: str) -> str:
    """
    分词函数
    
    Args:
        text: 清洗后的文本
    
    Returns:
        分词后的文本（空格分隔）
    """
    # 结巴分词
    words = jieba.lcut(text)
    
    # 去除停用词和长度<2的词
    words = [w for w in words if w not in STOPWORDS and len(w) >= 2]
    
    return ' '.join(words)


def cut_words_to_list(text: str) -> List[str]:
    """
    分词函数，返回列表形式
    
    Args:
        text: 清洗后的文本
    
    Returns:
        分词后的词列表
    """
    if pd.isna(text) or not isinstance(text, str):
        return []
    
    # 结巴分词
    words = jieba.lcut(text)
    
    # 去除停用词和长度<2的词
    words = [w for w in words if w not in STOPWORDS and len(w) >= 2]
    
    return words


def preprocess_single(text: str) -> str:
    """
    单条评论预处理完整流程
    
    Args:
        text: 原始评论文本
    
    Returns:
        预处理后的文本（空格分隔的词）
    """
    cleaned = clean_text(text)
    seg_text = cut_words(cleaned)
    return seg_text


def preprocess_for_wordcloud(text: str) -> List[str]:
    """
    为词云生成预处理文本，返回词列表
    
    Args:
        text: 原始评论文本
    
    Returns:
        分词后的词列表
    """
    if pd.isna(text) or not isinstance(text, str):
        return []
    
    # 复用清洗函数
    cleaned = clean_text(text)
    
    # 复用分词和过滤逻辑，但返回列表
    return cut_words_to_list(cleaned)


def batch_clean(texts: List[str]) -> List[str]:
    """
    批量清洗文本
    
    Args:
        texts: 文本列表
    
    Returns:
        清洗后的文本列表
    """
    return [clean_text(t) for t in texts]


def batch_process(texts: List[str]) -> List[str]:
    """
    批量处理文本（清洗+分词）
    
    Args:
        texts: 原始文本列表
    
    Returns:
        处理后的文本列表
    """
    processed = []
    for text in texts:
        cleaned = clean_text(text)
        segmented = cut_words(cleaned)
        processed.append(segmented)
    return processed


def clean_data(df, review_col="review", label_col="label"):
    """
    清洗数据
    
    Args:
        df: 数据框
        review_col: 评论列名
        label_col: 标签列名
    
    Returns:
        清洗后的数据框
    """
    # 1. 过滤空文本
    df = df[df[review_col].notna() & (df[review_col] != "")]
    # 2. 去重（删除重复评论）
    df = df.drop_duplicates(subset=[review_col])
    # 3. 过滤广告/无关文本（比如“加微信”“领券”“刷单”）
    ad_pattern = r"加微信|领券|刷单|代购|红包|二维码|链接"
    df = df[~df[review_col].str.contains(ad_pattern, regex=True)]
    # 4. 过滤过短文本（比如少于3个字的无意义评论）
    df = df[df[review_col].str.len() >= 3]
    # 5. 重置索引
    df = df.reset_index(drop=True)
    return df


if __name__ == "__main__":
    # 测试清洗功能
    test_text = "【测试】这个产品很好！！★★★★★ 100分！"
    print(f"原始文本: {test_text}")
    print(f"清洗后: {clean_text(test_text)}")
    print(f"分词后: {cut_words(clean_text(test_text))}")
