import os
import argparse
from typing import Tuple, Dict, Any

import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB
from sklearn.svm import LinearSVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix

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
    
    # 检查必要的列
    if 'review' not in df.columns or 'label' not in df.columns:
        raise ValueError("数据文件必须包含 'review' 和 'label' 列")
    
    # 保留非书籍评论（根据原始notebook）
    if 'cat' in df.columns:
        df = df[df['cat'] != '书籍']
    
    # 清洗数据
    df = clean_data(df, review_col='review', label_col='label')
    
    # 预处理文本
    print("开始文本预处理...")
    df['seg_review'] = df['review'].apply(preprocess_single)
    
    # 过滤空评论
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
        ngram_range=(1, 2),      # 一元+二元词组
        min_df=2,                # 至少在2个文档中出现
        max_df=0.8,              # 忽略在80%以上文档出现的词
        sublinear_tf=True,       # 使用1+log(tf)替代tf
        use_idf=True
    )
    
    # 拟合并转换训练集
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)
    
    print(f"特征提取完成！")
    print(f"特征维度: {X_train_vec.shape[1]}")
    print(f"训练集特征矩阵形状: {X_train_vec.shape}")
    print(f"测试集特征矩阵形状: {X_test_vec.shape}")
    
    return X_train_vec, X_test_vec, vectorizer


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
        
        # 训练模型
        model.fit(X_train, Y_train)
        
        # 5折交叉验证
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
    # 预测
    y_pred = model.predict(X_test)
    
    # 计算指标
    acc = accuracy_score(Y_test, y_pred)
    f1 = f1_score(Y_test, y_pred)
    
    print("\n" + "="*60)
    print(f"{model_name} 评估结果")
    print("="*60)
    print(f"准确率: {acc:.4f}")
    print(f"F1分数: {f1:.4f}")
    
    print("\n分类报告:")
    print(classification_report(Y_test, y_pred, target_names=['差评', '好评']))
    
    # 混淆矩阵
    cm = confusion_matrix(Y_test, y_pred)
    print("\n混淆矩阵:")
    print(f"          预测差评  预测好评")
    print(f"实际差评   {cm[0,0]:6d}   {cm[0,1]:6d}")
    print(f"实际好评   {cm[1,0]:6d}   {cm[1,1]:6d}")
    
    # 计算各类错误率
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
    
    # 保存模型和向量化器
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
    
    # 划分训练集和测试集
    X_train, X_test, Y_train, Y_test = train_test_split(
        X, Y, 
        test_size=test_size, 
        random_state=random_state, 
        stratify=Y
    )
    
    print(f"\n数据集划分完成")
    print(f"训练集: {len(X_train)} 条")
    print(f"测试集: {len(X_test)} 条")
    
    # 特征提取
    X_train_vec, X_test_vec, vectorizer = extract_features(
        X_train, X_test, max_features=max_features
    )
    
    # 训练多个模型
    model_results = train_models(X_train_vec, Y_train)
    
    # 显示对比结果
    print("\n" + "="*60)
    print("模型对比结果")
    print("="*60)
    results_df = pd.DataFrame([
        {'模型': k, '平均F1': v['cv_mean'], '标准差': v['cv_std']} 
        for k, v in model_results.items()
    ]).sort_values('平均F1', ascending=False)
    print(results_df.to_string(index=False))
    
    # 超参数优化
    print("\n评估优化后的逻辑回归模型")
    best_lr = optimize_lr(X_train_vec, Y_train)
    lr_acc, lr_f1 = evaluate_model(best_lr, X_test_vec, Y_test, "优化后逻辑回归")
    
    print("\n评估优化后的SVM模型")
    best_svc = optimize_svc(X_train_vec, Y_train)
    svc_acc, svc_f1 = evaluate_model(best_svc, X_test_vec, Y_test, "优化后线性SVM")
    
    # 保存模型
    save_models(best_lr, best_svc, vectorizer)
    
    print(f"\nSVM测试集F1分数: {svc_f1:.4f}")
    print(f"逻辑回归测试集F1分数: {lr_f1:.4f}")


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
