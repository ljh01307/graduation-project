import argparse
import os

import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import train_test_split

from preprocess import batch_clean


def load_data(path: str):
    if not os.path.exists(path):
        raise FileNotFoundError(f"数据文件不存在: {path}")

    df = pd.read_csv(path)
    if "text" not in df.columns or "label" not in df.columns:
        raise ValueError("CSV 必须包含 'text' 和 'label' 两列")

    texts = df["text"].astype(str).tolist()
    labels = df["label"].astype(str).tolist()
    return texts, labels


def train_model(
    data_path: str,
    model_path: str = "model.joblib",
    test_size: float = 0.2,
    random_state: int = 42,
):
    print(f"读取数据: {data_path}")
    texts, labels = load_data(data_path)

    print(f"样本数量: {len(texts)}")

    # 清洗文本
    print("清洗文本...")
    texts_clean = batch_clean(texts)

    # 划分训练 / 验证
    X_train, X_test, y_train, y_test = train_test_split(
        texts_clean, labels, test_size=test_size, random_state=random_state, stratify=labels
    )

    # 构建 TF-IDF 特征
    print("构建 TF-IDF 特征...")
    vectorizer = TfidfVectorizer(
        max_features=5000,
        ngram_range=(1, 2),  # 一元 & 二元短语
        min_df=1,
    )
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)

    # 训练逻辑回归分类器
    print("训练逻辑回归模型...")
    clf = LogisticRegression(
        max_iter=200,
        n_jobs=-1,
    )
    clf.fit(X_train_vec, y_train)

    # 评估
    print("在验证集上评估...")
    y_pred = clf.predict(X_test_vec)
    print("分类报告：")
    print(classification_report(y_test, y_pred, digits=4))
    print("混淆矩阵：")
    print(confusion_matrix(y_test, y_pred))

    # 保存模型和向量化器
    print(f"保存模型到: {model_path}")
    joblib.dump(
        {
            "vectorizer": vectorizer,
            "classifier": clf,
        },
        model_path,
    )
    print("训练完成。")


def parse_args():
    parser = argparse.ArgumentParser(description="电商评论情感分类 - 训练脚本（机器学习）")
    parser.add_argument(
        "--data_path",
        type=str,
        default="data/sample_reviews.csv",
        help="训练数据 CSV 路径，需包含 text 和 label 列",
    )
    parser.add_argument(
        "--model_path",
        type=str,
        default="model.joblib",
        help="保存模型的路径（含向量化器和分类器）",
    )
    parser.add_argument(
        "--test_size",
        type=float,
        default=0.2,
        help="验证集比例，默认 0.2",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    train_model(
        data_path=args.data_path,
        model_path=args.model_path,
        test_size=args.test_size,
    )

