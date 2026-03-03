import re
from typing import List


def clean_text(text: str) -> str:
    """
    简单清洗文本：
    - 去掉多余空白
    - 统一大小写（对英文有用，对中文无影响）
    - 去掉 URL、@、# 等噪声
    - 将表情符号转为文字描述（如: 😄 -> :smiling_face_with_open_mouth:)
    """
    if not isinstance(text, str):
        text = str(text)

    text = text.strip()

    # 替换 URL
    text = re.sub(r"http[s]?://\\S+", " URL ", text)
    # 去掉 @和#
    text = re.sub(r"[@#]\\S+", " ", text)

    # 去掉大部分表情符号（简单规则版，不依赖第三方库）
    # 许多 emoji 位于基本多文种平面以外的 Unicode 区间，这里直接删除这些字符
    text = re.sub(r"[\\U00010000-\\U0010ffff]", "", text)

    # 多个空白合并
    text = re.sub(r"\\s+", " ", text)

    # 英文统一小写
    text = text.lower()

    return text


def batch_clean(texts: List[str]) -> List[str]:
    """批量清洗文本"""
    return [clean_text(t) for t in texts]


if __name__ == "__main__":
    demo = "这个手机👍，非常好用！物流也很快～ https://example.com"
    print(clean_text(demo))

