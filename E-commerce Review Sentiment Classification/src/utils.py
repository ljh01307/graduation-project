import os
from typing import Set


def load_stopwords(stopword_file: str = None) -> Set[str]:
    """
    加载中文停用词
    
    Args:
        stopword_file: 停用词文件路径，默认为None时自动查找
    
    Returns:
        停用词集合
    """
    if stopword_file is None:
        stopword_file = os.path.join(get_project_root(), "data", "stopwords.txt")
    
    stopwords = set()
    
    try:
        with open(stopword_file, "r", encoding="utf-8") as f:
            for line in f:
                word = line.strip()
                if word:
                    stopwords.add(word)
        print(f"停用词加载成功: {len(stopwords)}个")
    except FileNotFoundError:
        print(f"停用词文件未找到: {stopword_file}，使用默认停用词")
        stopwords = {"的", "了", "是", "在", "我", "有", "和", "就", "不", "也", "这", "你"}
    except Exception as e:
        print(f"加载停用词失败: {e}，使用默认停用词")
        stopwords = {"的", "了", "是", "在", "我", "有", "和", "就", "不", "也", "这", "你"}
    
    return stopwords


def get_project_root() -> str:
    """
    获取项目根目录
    
    Returns:
        项目根目录路径
    """
    current_file = os.path.abspath(__file__)
    src_dir = os.path.dirname(current_file)
    return os.path.dirname(src_dir)


def get_model_path(model_name: str) -> str:
    """
    获取模型文件路径
    
    Args:
        model_name: 模型名称
    
    Returns:
        模型文件路径
    """
    return os.path.join(get_project_root(), "models", model_name)


def get_data_path(data_name: str) -> str:
    """
    获取数据文件路径
    
    Args:
        data_name: 数据文件名
    
    Returns:
        数据文件路径
    """
    return os.path.join(get_project_root(), "data", data_name)
