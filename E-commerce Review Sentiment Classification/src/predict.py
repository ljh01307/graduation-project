import argparse
import os
from typing import List, Optional

import joblib
import pandas as pd

from preprocess import batch_clean


def load_model(model_path: str):
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"模型文件不存在: {model_path}，请先运行 train.py 训练模型。")
    bundle = joblib.load(model_path)
    vectorizer = bundle["vectorizer"]
    classifier = bundle["classifier"]
    return vectorizer, classifier


def predict_texts(
    texts: List[str],
    model_path: str,
) -> List[str]:
    vectorizer, classifier = load_model(model_path)
    texts_clean = batch_clean(texts)
    X_vec = vectorizer.transform(texts_clean)
    preds = classifier.predict(X_vec)
    return preds.tolist()


def main(
    model_path: str,
    text: Optional[str] = None,
    input_csv: Optional[str] = None,
    output_csv: Optional[str] = None,
):
    if text is None and input_csv is None:
        raise ValueError("必须提供 --text 或 --input_csv 之一。")

    if text is not None:
        preds = predict_texts([text], model_path)
        print(f"文本: {text}")
        print(f"预测情感: {preds[0]}")
    else:
        if not os.path.exists(input_csv):
            raise FileNotFoundError(f"输入 CSV 不存在: {input_csv}")

        df = pd.read_csv(input_csv)
        if "text" not in df.columns:
            raise ValueError("输入 CSV 必须包含 'text' 列。")

        preds = predict_texts(df["text"].astype(str).tolist(), model_path)
        df["pred_label"] = preds

        if output_csv:
            df.to_csv(output_csv, index=False)
            print(f"预测结果已保存到: {output_csv}")
        else:
            print(df[["text", "pred_label"]].head())


def parse_args():
    parser = argparse.ArgumentParser(description="电商评论情感分类 - 预测脚本（机器学习）")
    parser.add_argument(
        "--model_path",
        type=str,
        default="model.joblib",
        help="训练好的模型路径（train.py 保存的文件）",
    )
    parser.add_argument(
        "--text",
        type=str,
        default=None,
        help="单条待预测文本",
    )
    parser.add_argument(
        "--input_csv",
        type=str,
        default=None,
        help="批量预测时的输入 CSV 文件路径（需包含 text 列）",
    )
    parser.add_argument(
        "--output_csv",
        type=str,
        default=None,
        help="批量预测结果保存路径（可选）",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    main(
        model_path=args.model_path,
        text=args.text,
        input_csv=args.input_csv,
        output_csv=args.output_csv,
    )

