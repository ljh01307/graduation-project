import os
import argparse
from typing import Tuple, Dict, Any

import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB, GaussianNB
from sklearn.svm import LinearSVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix
from gensim.models import Word2Vec

from src.preprocess import clean_data, preprocess_single
from src.utils import get_data_path, get_model_path, get_project_root


def load_data(data_path: str) -> Tuple[np.ndarray, np.ndarray]:
    """
    加载训练数据

    Args:
        data_path: 数据文件路径

    Returns:
        文本数组和标签数组
    """
    if not os.path.exists(data_path):
        raise FileNotFoundError(f"数据文件不存在: {data_path}")

    df = pd.read_csv(data_path)

    if 'review' not in df.columns or 'label' not in df.columns:
        raise ValueError("数据文件必须包含 'review' 和 'label' 列")

    if 'cat' in df.columns:
        df = df[df['cat'] != '书籍']

    df = clean_data(df, review_col='review', label_col='label')

    print("开始文本预处理...")
    df['seg_review'] = df['review'].apply(preprocess_single)

    df = df[df['seg_review'] != '']

    print(f"总样本数: {len(df)}")
    print(f"好评比例: {(df['label'] == 1).sum() / len(df):.2%}")
    print(f"差评比例: {(df['label'] == 0).sum() / len(df):.2%}")

    return df['seg_review'].values, df['label'].values


def extract_features(X_train: np.ndarray, X_test: np.ndarray,
                     max_features: int = 8000) -> Tuple[Any, Any, TfidfVectorizer]:
    """
    TF-IDF特征提取

    Args:
        X_train: 训练集文本
        X_test: 测试集文本
        max_features: 最大特征数

    Returns:
        训练集特征、测试集特征、向量化器
    """
    print(f"提取特征，最大特征数: {max_features}")

    vectorizer = TfidfVectorizer(
        max_features=max_features,
        ngram_range=(1, 2),
        min_df=2,
        max_df=0.8,
        sublinear_tf=True,
        use_idf=True
    )

    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)

    print(f"特征提取完成！")
    print(f"特征维度: {X_train_vec.shape[1]}")
    print(f"训练集特征矩阵形状: {X_train_vec.shape}")
    print(f"测试集特征矩阵形状: {X_test_vec.shape}")

    return X_train_vec, X_test_vec, vectorizer


def extract_features_w2v(X_train: np.ndarray, X_test: np.ndarray,
                          vector_size: int = 200, window: int = 5,
                          min_count: int = 3) -> Tuple[Any, Any, Word2Vec]:
    """
    Word2Vec特征提取

    Args:
        X_train: 训练集文本（分词后的字符串，空格分隔）
        X_test: 测试集文本
        vector_size: 词向量维度
        window: 上下文窗口大小
        min_count: 最小词频阈值

    Returns:
        训练集特征、测试集特征、Word2Vec模型
    """
    print(f"\n{'='*60}")
    print("Word2Vec 特征提取")
    print(f"{'='*60}")
    print(f"向量维度: {vector_size}, 窗口大小: {window}, 最小词频: {min_count}")

    sentences = [text.split() for text in X_train]

    print("训练 Word2Vec 模型...")
    w2v_model = Word2Vec(
        sentences=sentences,
        vector_size=vector_size,
        window=window,
        min_count=min_count,
        workers=-1,
        epochs=10,
        seed=42
    )
    print(f"词汇表大小: {len(w2v_model.wv)}")

    def text_to_vector(text):
        words = text.split()
        vectors = [w2v_model.wv[w] for w in words if w in w2v_model.wv]
        if vectors:
            return np.mean(vectors, axis=0)
        return np.zeros(vector_size)

    print("转换训练集文本为向量...")
    X_train_vec = np.array([text_to_vector(text) for text in X_train])
    print("转换测试集文本为向量...")
    X_test_vec = np.array([text_to_vector(text) for text in X_test])

    print(f"特征提取完成！")
    print(f"训练集特征矩阵形状: {X_train_vec.shape}")
    print(f"测试集特征矩阵形状: {X_test_vec.shape}")

    return X_train_vec, X_test_vec, w2v_model


def train_models(X_train: Any, Y_train: np.ndarray) -> Dict[str, Dict[str, Any]]:
    """
    训练多个模型并交叉验证

    Args:
        X_train: 训练集特征
        Y_train: 训练集标签

    Returns:
        模型结果字典
    """
    models = {
        'LogisticRegression': LogisticRegression(
            C=2.0,
            max_iter=1000,
            solver='liblinear',
            random_state=42
        ),
        'NaiveBayes': MultinomialNB(alpha=0.1),
        'LinearSVC': LinearSVC(C=1.0, max_iter=2000, random_state=42),
        'RandomForest': RandomForestClassifier(
            n_estimators=100,
            max_depth=20,
            random_state=42,
            n_jobs=-1
        ),
    }

    results = {}

    print("="*60)
    print("开始5折交叉验证训练")
    print("="*60)

    for name, model in models.items():
        print(f"\n训练模型: {name}")

        model.fit(X_train, Y_train)

        cv_scores = cross_val_score(model, X_train, Y_train, cv=5, scoring='f1')

        results[name] = {
            'model': model,
            'cv_mean': cv_scores.mean(),
            'cv_std': cv_scores.std()
        }

        print(f"CV F1分数: {cv_scores.mean():.4f} (±{cv_scores.std():.4f})")

    return results


def train_models_w2v(X_train: Any, Y_train: np.ndarray) -> Dict[str, Dict[str, Any]]:
    """
    训练多个模型并交叉验证（Word2Vec特征专用）
    使用GaussianNB替代MultinomialNB（因为Word2Vec特征可能包含负值）
    """
    models = {
        'LogisticRegression': LogisticRegression(
            C=2.0,
            max_iter=1000,
            solver='liblinear',
            random_state=42
        ),
        'GaussianNB': GaussianNB(),
        'LinearSVC': LinearSVC(C=1.0, max_iter=2000, random_state=42),
        'RandomForest': RandomForestClassifier(
            n_estimators=100,
            max_depth=20,
            random_state=42,
            n_jobs=-1
        ),
    }

    results = {}

    print("="*60)
    print("开始5折交叉验证训练")
    print("="*60)

    for name, model in models.items():
        print(f"\n训练模型: {name}")

        model.fit(X_train, Y_train)

        cv_scores = cross_val_score(model, X_train, Y_train, cv=5, scoring='f1')

        results[name] = {
            'model': model,
            'cv_mean': cv_scores.mean(),
            'cv_std': cv_scores.std()
        }

        print(f"CV F1分数: {cv_scores.mean():.4f} (±{cv_scores.std():.4f})")

    return results


def optimize_lr(X_train: Any, Y_train: np.ndarray) -> LogisticRegression:
    """
    逻辑回归超参数优化

    Args:
        X_train: 训练集特征
        Y_train: 训练集标签

    Returns:
        最佳逻辑回归模型
    """
    print("\n" + "="*60)
    print("逻辑回归超参数优化")
    print("="*60)

    param_grid = {
        'C': [0.1, 0.5, 1.0, 2.0, 5.0],
        'solver': ['liblinear', 'saga'],
        'penalty': ['l2']
    }

    lr = LogisticRegression(max_iter=1000, random_state=42)

    grid = GridSearchCV(
        lr,
        param_grid,
        cv=5,
        scoring='f1',
        n_jobs=-1,
        verbose=1
    )

    print("正在搜索最佳参数...")
    grid.fit(X_train, Y_train)

    print(f"\n优化完成！")
    print(f"最佳参数: {grid.best_params_}")
    print(f"最佳F1分数: {grid.best_score_:.4f}")

    return grid.best_estimator_


def optimize_svc(X_train: Any, Y_train: np.ndarray) -> LinearSVC:
    """
    SVM超参数优化

    Args:
        X_train: 训练集特征
        Y_train: 训练集标签

    Returns:
        最佳SVM模型
    """
    print("\n" + "="*60)
    print("线性SVM超参数优化")
    print("="*60)

    param_grid = {
        'C': [0.1, 0.5, 1.0, 2.0],
        'loss': ['hinge', 'squared_hinge'],
        'penalty': ['l2']
    }

    svc = LinearSVC(max_iter=2000, random_state=42)

    grid = GridSearchCV(
        svc,
        param_grid,
        cv=5,
        scoring='f1',
        n_jobs=-1,
        verbose=1
    )

    print("正在搜索最佳参数...")
    grid.fit(X_train, Y_train)

    print(f"\n优化完成！")
    print(f"最佳参数: {grid.best_params_}")
    print(f"最佳F1分数: {grid.best_score_:.4f}")

    return grid.best_estimator_


def evaluate_model(model, X_test: Any, Y_test: np.ndarray, model_name: str = 'Model') -> Tuple[float, float]:
    """
    评估模型性能

    Args:
        model: 模型对象
        X_test: 测试集特征
        Y_test: 测试集标签
        model_name: 模型名称

    Returns:
        准确率和F1分数
    """
    y_pred = model.predict(X_test)

    acc = accuracy_score(Y_test, y_pred)
    f1 = f1_score(Y_test, y_pred)

    print("\n" + "="*60)
    print(f"{model_name} 评估结果")
    print("="*60)
    print(f"准确率: {acc:.4f}")
    print(f"F1分数: {f1:.4f}")

    print("\n分类报告:")
    print(classification_report(Y_test, y_pred, target_names=['差评', '好评']))

    cm = confusion_matrix(Y_test, y_pred)
    print("\n混淆矩阵:")
    print(f"          预测差评  预测好评")
    print(f"实际差评   {cm[0,0]:6d}   {cm[0,1]:6d}")
    print(f"实际好评   {cm[1,0]:6d}   {cm[1,1]:6d}")

    tn, fp, fn, tp = cm.ravel()
    print(f"\n详细指标:")
    print(f"  特异度 (TNR): {tn/(tn+fp):.4f}")
    print(f"  灵敏度 (TPR): {tp/(tp+fn):.4f}")
    print(f"  精确率 (PPV): {tp/(tp+fp):.4f}")

    return acc, f1


def save_models(best_lr: LogisticRegression, best_svc: LinearSVC,
                vectorizer: TfidfVectorizer, model_dir: str = None):
    """
    保存模型和向量化器

    Args:
        best_lr: 最佳逻辑回归模型
        best_svc: 最佳SVM模型
        vectorizer: 向量化器
        model_dir: 模型保存目录
    """
    if model_dir is None:
        model_dir = os.path.join(get_project_root(), "models")

    os.makedirs(model_dir, exist_ok=True)

    joblib.dump(best_svc, os.path.join(model_dir, "best_svm_model.pkl"))
    joblib.dump(vectorizer, os.path.join(model_dir, "tfidf_vectorizer.pkl"))
    joblib.dump(best_lr, os.path.join(model_dir, "best_lr_model.pkl"))

    print(f"\n模型已保存至: {model_dir}")


def main(data_path: str = None, test_size: float = 0.2,
         random_state: int = 666, max_features: int = 8000):
    """
    主训练流程

    Args:
        data_path: 数据文件路径
        test_size: 测试集比例
        random_state: 随机种子
        max_features: 最大特征数
    """
    if data_path is None:
        data_path = get_data_path("online_shopping_10_cats.csv")

    print(f"加载数据: {data_path}")
    X, Y = load_data(data_path)

    X_train, X_test, Y_train, Y_test = train_test_split(
        X, Y,
        test_size=test_size,
        random_state=random_state,
        stratify=Y
    )

    print(f"\n数据集划分完成")
    print(f"训练集: {len(X_train)} 条")
    print(f"测试集: {len(X_test)} 条")

    print("\n" + "="*60)
    print("实验一：TF-IDF 特征")
    print("="*60)

    X_train_vec, X_test_vec, vectorizer = extract_features(
        X_train, X_test, max_features=max_features
    )

    model_results = train_models(X_train_vec, Y_train)

    print("\n" + "="*60)
    print("TF-IDF 模型对比结果")
    print("="*60)
    results_df = pd.DataFrame([
        {'模型': k, '平均F1': v['cv_mean'], '标准差': v['cv_std']}
        for k, v in model_results.items()
    ]).sort_values('平均F1', ascending=False)
    print(results_df.to_string(index=False))

    print("\n评估优化后的逻辑回归模型 (TF-IDF)")
    best_lr = optimize_lr(X_train_vec, Y_train)
    lr_acc, lr_f1 = evaluate_model(best_lr, X_test_vec, Y_test, "TF-IDF 逻辑回归")

    print("\n评估优化后的SVM模型 (TF-IDF)")
    best_svc = optimize_svc(X_train_vec, Y_train)
    svc_acc, svc_f1 = evaluate_model(best_svc, X_test_vec, Y_test, "TF-IDF SVM")

    save_models(best_lr, best_svc, vectorizer)

    tfidf_results = {
        'lr_f1': lr_f1,
        'svc_f1': svc_f1,
        'lr_acc': lr_acc,
        'svc_acc': svc_acc
    }

    print("\n" + "="*60)
    print("实验二：Word2Vec 特征")
    print("="*60)

    X_train_w2v, X_test_w2v, w2v_model = extract_features_w2v(
        X_train, X_test, vector_size=200, window=5, min_count=3
    )

    print("\n训练 Word2Vec 特征下的模型...")
    w2v_model_results = train_models_w2v(X_train_w2v, Y_train)

    print("\n" + "="*60)
    print("Word2Vec 模型对比结果")
    print("="*60)
    w2v_results_df = pd.DataFrame([
        {'模型': k, '平均F1': v['cv_mean'], '标准差': v['cv_std']}
        for k, v in w2v_model_results.items()
    ]).sort_values('平均F1', ascending=False)
    print(w2v_results_df.to_string(index=False))

    print("\n评估优化后的逻辑回归模型 (Word2Vec)")
    best_lr_w2v = optimize_lr(X_train_w2v, Y_train)
    lr_acc_w2v, lr_f1_w2v = evaluate_model(best_lr_w2v, X_test_w2v, Y_test, "Word2Vec 逻辑回归")

    print("\n评估优化后的SVM模型 (Word2Vec)")
    best_svc_w2v = optimize_svc(X_train_w2v, Y_train)
    svc_acc_w2v, svc_f1_w2v = evaluate_model(best_svc_w2v, X_test_w2v, Y_test, "Word2Vec SVM")

    w2v_results = {
        'lr_f1': lr_f1_w2v,
        'svc_f1': svc_f1_w2v,
        'lr_acc': lr_acc_w2v,
        'svc_acc': svc_acc_w2v
    }

    print("\n" + "="*60)
    print("TF-IDF vs Word2Vec 最终对比")
    print("="*60)
    print(f"{'特征':<15} {'模型':<20} {'准确率':<10} {'F1分数':<10}")
    print("-" * 55)
    print(f"{'TF-IDF':<15} {'逻辑回归':<20} {tfidf_results['lr_acc']:<10.4f} {tfidf_results['lr_f1']:<10.4f}")
    print(f"{'TF-IDF':<15} {'SVM':<20} {tfidf_results['svc_acc']:<10.4f} {tfidf_results['svc_f1']:<10.4f}")
    print(f"{'Word2Vec':<15} {'逻辑回归':<20} {w2v_results['lr_acc']:<10.4f} {w2v_results['lr_f1']:<10.4f}")
    print(f"{'Word2Vec':<15} {'SVM':<20} {w2v_results['svc_acc']:<10.4f} {w2v_results['svc_f1']:<10.4f}")

    best_method = "TF-IDF" if tfidf_results['svc_f1'] >= w2v_results['svc_f1'] else "Word2Vec"
    print(f"\n最佳特征提取方法: {best_method}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="电商评论情感分类 - 训练脚本")
    parser.add_argument(
        "--data_path",
        type=str,
        default=None,
        help="训练数据CSV路径，默认为 data/online_shopping_10_cats.csv",
    )
    parser.add_argument(
        "--test_size",
        type=float,
        default=0.2,
        help="测试集比例，默认 0.2",
    )
    parser.add_argument(
        "--max_features",
        type=int,
        default=8000,
        help="TF-IDF最大特征数，默认 8000",
    )

    args = parser.parse_args()

    main(
        data_path=args.data_path,
        test_size=args.test_size,
        max_features=args.max_features
    )