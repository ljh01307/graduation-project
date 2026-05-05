import os
import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB, GaussianNB
from sklearn.svm import LinearSVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.metrics import accuracy_score, f1_score, precision_score, recall_score, confusion_matrix, classification_report
from gensim.models import Word2Vec

from src.preprocess import clean_data, preprocess_single
from src.utils import get_data_path


def load_data(data_path: str):
    df = pd.read_csv(data_path)
    if 'cat' in df.columns:
        df = df[~df['cat'].isin(['书籍', '酒店'])]
    df = clean_data(df, review_col='review', label_col='label')
    df['seg_review'] = df['review'].apply(preprocess_single)
    df = df[df['seg_review'] != '']
    print(f"总样本数: {len(df)}")
    print(f"好评: {(df['label'] == 1).sum()} 条, 差评: {(df['label'] == 0).sum()} 条")
    return df['seg_review'].values, df['label'].values


def extract_tfidf_features(X_train, X_test, max_features=8000):
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
    return X_train_vec, X_test_vec, vectorizer


def extract_w2v_features(X_train, X_test, vector_size=200, window=5, min_count=3):
    sentences = [text.split() for text in X_train]
    w2v_model = Word2Vec(
        sentences=sentences,
        vector_size=vector_size,
        window=window,
        min_count=min_count,
        workers=-1,
        epochs=10,
        sg=1,
        seed=42
    )
    print(f"Word2Vec词汇表大小: {len(w2v_model.wv)}")

    def text_to_vector(text):
        words = text.split()
        vectors = [w2v_model.wv[w] for w in words if w in w2v_model.wv]
        if vectors:
            return np.mean(vectors, axis=0)
        return np.zeros(vector_size)

    X_train_vec = np.array([text_to_vector(text) for text in X_train])
    X_test_vec = np.array([text_to_vector(text) for text in X_test])
    return X_train_vec, X_test_vec, w2v_model


def evaluate_and_print(model, X_test, Y_test, model_name):
    y_pred = model.predict(X_test)
    acc = accuracy_score(Y_test, y_pred)
    precision = precision_score(Y_test, y_pred)
    recall = recall_score(Y_test, y_pred)
    f1 = f1_score(Y_test, y_pred)
    print(f"\n{model_name}:")
    print(f"  准确率: {acc:.4f}, 精确率: {precision:.4f}, 召回率: {recall:.4f}, F1分数: {f1:.4f}")
    cm = confusion_matrix(Y_test, y_pred)
    print(f"\n  混淆矩阵:")
    print(f"              预测差评  预测好评")
    print(f"  实际差评     {cm[0,0]:5d}   {cm[0,1]:5d}")
    print(f"  实际好评     {cm[1,0]:5d}   {cm[1,1]:5d}")
    return acc, precision, recall, f1


def demo_vectorization():
    print("\n" + "=" * 70)
    print("电商评论情感分类 - TF-IDF vs Word2Vec 向量化对比")
    print("=" * 70)

    data_path = get_data_path("online_shopping_10_cats.csv")
    print(f"\n加载数据: {data_path}")
    X, Y = load_data(data_path)

    X_train, X_test, Y_train, Y_test = train_test_split(
        X, Y, test_size=0.2, random_state=666, stratify=Y
    )
    print(f"训练集: {len(X_train)} 条, 测试集: {len(X_test)} 条")

    print("\n" + "=" * 70)
    print("TF-IDF 特征提取")
    print("=" * 70)
    X_train_tfidf, X_test_tfidf, tfidf_vec = extract_tfidf_features(X_train, X_test)
    print(f"特征维度: {X_train_tfidf.shape[1]}")
    feature_names = tfidf_vec.get_feature_names_out()
    print(f"词汇表大小: {len(feature_names)}")

    print("\n" + "-" * 70)
    print("TF-IDF 文档向量展示 (词: 向量值)")
    print("-" * 70)
    for i in range(3):
        label = "好评" if Y_train[i] == 1 else "差评"
        vec = X_train_tfidf[i].toarray().flatten()
        non_zero_indices = np.where(vec > 0)[0]
        print(f"\n文档{i+1} [{label}]: {X_train[i]}")
        print(f"  向量维度: {len(vec)}, 非零词数: {len(non_zero_indices)}")
        print(f"  {'词':<15} {'TF-IDF值':<10}")
        print(f"  {'-'*25}")
        for idx in non_zero_indices:
            print(f"  {feature_names[idx]:<15} {vec[idx]:.6f}")

    print("\n" + "=" * 70)
    print("Word2Vec 特征提取")
    print("=" * 70)
    X_train_w2v, X_test_w2v, w2v_model = extract_w2v_features(X_train, X_test)
    print(f"特征维度: {X_train_w2v.shape[1]}")

    print("\n" + "-" * 70)
    print("Word2Vec 词向量示例")
    print("-" * 70)
    sample_words = ["质量", "服务", "快递", "满意", "差", "好", "便宜", "喜欢", "物流", "快速"]
    for word in sample_words:
        if word in w2v_model.wv:
            vec = w2v_model.wv[word]
            print(f"\n'{word}' 词向量 (共{len(vec)}维，前10维):")
            print(f"  {vec[:10].round(4)}")

    print("\n" + "-" * 70)
    print("Word2Vec 相似词示例")
    print("-" * 70)
    for word in ["好", "差", "满意"]:
        if word in w2v_model.wv:
            similar = w2v_model.wv.most_similar(word, topn=3)
            print(f"'{word}' 最相似词: {[(w, f'{s:.4f}') for w, s in similar]}")

    print("\n" + "-" * 70)
    print("Word2Vec 文档向量展示")
    print("-" * 70)
    for i in range(3):
        label = "好评" if Y_train[i] == 1 else "差评"
        vec = X_train_w2v[i]
        print(f"\n文档{i+1} [{label}]: {X_train[i]}")
        print(f"  向量维度: {len(vec)}")
        print(f"  前20维: {vec[:20].round(4)}")
        print(f"  均值: {vec.mean():.4f}, 标准差: {vec.std():.4f}")

    print("\n" + "=" * 70)
    print("模型训练与评估")
    print("=" * 70)

    print("\n--- TF-IDF + 各模型 ---")
    tfidf_results = {}
    models_tfidf = {
        'LogisticRegression': LogisticRegression(C=2.0, max_iter=1000, solver='liblinear', random_state=42),
        'NaiveBayes': MultinomialNB(alpha=0.1),
        'LinearSVC': LinearSVC(C=1.0, max_iter=2000, random_state=42),
        'RandomForest': RandomForestClassifier(n_estimators=100, max_depth=20, random_state=42, n_jobs=-1),
    }
    for name, model in models_tfidf.items():
        model.fit(X_train_tfidf, Y_train)
        cv_scores = cross_val_score(model, X_train_tfidf, Y_train, cv=5, scoring='f1')
        acc, precision, recall, f1 = evaluate_and_print(model, X_test_tfidf, Y_test, f"TF-IDF + {name}")
        tfidf_results[name] = {'model': model, 'acc': acc, 'precision': precision, 'recall': recall, 'f1': f1, 'cv_mean': cv_scores.mean(), 'cv_std': cv_scores.std()}
        print(f"  5折CV F1: {cv_scores.mean():.4f} (±{cv_scores.std():.4f})")

    print("\n--- TF-IDF 参数优化 ---")
    print("逻辑回归参数优化...")
    lr_param_grid = {'C': [0.1, 0.5, 1.0, 2.0, 5.0], 'solver': ['liblinear', 'saga'], 'penalty': ['l2']}
    lr_grid = GridSearchCV(LogisticRegression(max_iter=1000, random_state=42), lr_param_grid, cv=5, scoring='f1', n_jobs=-1)
    lr_grid.fit(X_train_tfidf, Y_train)
    print(f"  最佳参数: {lr_grid.best_params_}")
    print(f"  最佳CV F1: {lr_grid.best_score_:.4f}")
    best_lr_tfidf = lr_grid.best_estimator_
    acc, precision, recall, f1 = evaluate_and_print(best_lr_tfidf, X_test_tfidf, Y_test, "TF-IDF + LR(优化)")
    tfidf_results['LR_Optimized'] = {'model': best_lr_tfidf, 'acc': acc, 'precision': precision, 'recall': recall, 'f1': f1, 'cv_mean': lr_grid.best_score_}

    print("\nSVM参数优化...")
    svc_param_grid = {'C': [0.1, 0.5, 1.0, 2.0], 'loss': ['hinge', 'squared_hinge'], 'penalty': ['l2']}
    svc_grid = GridSearchCV(LinearSVC(max_iter=2000, random_state=42), svc_param_grid, cv=5, scoring='f1', n_jobs=-1)
    svc_grid.fit(X_train_tfidf, Y_train)
    print(f"  最佳参数: {svc_grid.best_params_}")
    print(f"  最佳CV F1: {svc_grid.best_score_:.4f}")
    best_svc_tfidf = svc_grid.best_estimator_
    acc, precision, recall, f1 = evaluate_and_print(best_svc_tfidf, X_test_tfidf, Y_test, "TF-IDF + SVM(优化)")
    tfidf_results['SVM_Optimized'] = {'model': best_svc_tfidf, 'acc': acc, 'precision': precision, 'recall': recall, 'f1': f1, 'cv_mean': svc_grid.best_score_}

    print("\n--- Word2Vec + 各模型 ---")
    w2v_results = {}
    models_w2v = {
        'LogisticRegression': LogisticRegression(C=2.0, max_iter=1000, solver='liblinear', random_state=42),
        'GaussianNB': GaussianNB(),
        'LinearSVC': LinearSVC(C=1.0, max_iter=2000, random_state=42),
        'RandomForest': RandomForestClassifier(n_estimators=100, max_depth=20, random_state=42, n_jobs=-1),
    }
    for name, model in models_w2v.items():
        model.fit(X_train_w2v, Y_train)
        cv_scores = cross_val_score(model, X_train_w2v, Y_train, cv=5, scoring='f1')
        acc, precision, recall, f1 = evaluate_and_print(model, X_test_w2v, Y_test, f"Word2Vec + {name}")
        w2v_results[name] = {'model': model, 'acc': acc, 'precision': precision, 'recall': recall, 'f1': f1, 'cv_mean': cv_scores.mean(), 'cv_std': cv_scores.std()}
        print(f"  5折CV F1: {cv_scores.mean():.4f} (±{cv_scores.std():.4f})")

    print("\n--- Word2Vec 参数优化 ---")
    print("逻辑回归参数优化...")
    lr_grid_w2v = GridSearchCV(LogisticRegression(max_iter=1000, random_state=42), lr_param_grid, cv=5, scoring='f1', n_jobs=-1)
    lr_grid_w2v.fit(X_train_w2v, Y_train)
    print(f"  最佳参数: {lr_grid_w2v.best_params_}")
    print(f"  最佳CV F1: {lr_grid_w2v.best_score_:.4f}")
    best_lr_w2v = lr_grid_w2v.best_estimator_
    acc, precision, recall, f1 = evaluate_and_print(best_lr_w2v, X_test_w2v, Y_test, "Word2Vec + LR(优化)")
    w2v_results['LR_Optimized'] = {'model': best_lr_w2v, 'acc': acc, 'precision': precision, 'recall': recall, 'f1': f1, 'cv_mean': lr_grid_w2v.best_score_}

    print("\nSVM参数优化...")
    svc_grid_w2v = GridSearchCV(LinearSVC(max_iter=2000, random_state=42), svc_param_grid, cv=5, scoring='f1', n_jobs=-1)
    svc_grid_w2v.fit(X_train_w2v, Y_train)
    print(f"  最佳参数: {svc_grid_w2v.best_params_}")
    print(f"  最佳CV F1: {svc_grid_w2v.best_score_:.4f}")
    best_svc_w2v = svc_grid_w2v.best_estimator_
    acc, precision, recall, f1 = evaluate_and_print(best_svc_w2v, X_test_w2v, Y_test, "Word2Vec + SVM(优化)")
    w2v_results['SVM_Optimized'] = {'model': best_svc_w2v, 'acc': acc, 'precision': precision, 'recall': recall, 'f1': f1, 'cv_mean': svc_grid_w2v.best_score_}

    print("\n" + "=" * 70)
    print("TF-IDF vs Word2Vec 对比汇总 (优化后)")
    print("=" * 70)
    print(f"{'特征':<15} {'模型':<22} {'准确率':<10} {'精确率':<10} {'召回率':<10} {'F1分数':<10}")
    print("-" * 85)
    display_models = ['LogisticRegression', 'NaiveBayes', 'LinearSVC', 'RandomForest', 'LR_Optimized', 'SVM_Optimized']
    for name in display_models:
        if name in tfidf_results:
            r = tfidf_results[name]
            print(f"{'TF-IDF':<15} {name:<22} {r['acc']:<10.4f} {r['precision']:<10.4f} {r['recall']:<10.4f} {r['f1']:<10.4f}")
    for name in display_models:
        if name in w2v_results:
            r = w2v_results[name]
            print(f"{'Word2Vec':<15} {name:<22} {r['acc']:<10.4f} {r['precision']:<10.4f} {r['recall']:<10.4f} {r['f1']:<10.4f}")

    print("\n" + "=" * 70)
    print("向量特性对比")
    print("=" * 70)
    print(f"""
    | 特性         | TF-IDF                  | Word2Vec               |
    |--------------|-------------------------|------------------------|
    | 向量维度     | {X_train_tfidf.shape[1]:>23} | {X_train_w2v.shape[1]:>23} |
    | 向量类型     | 稀疏矩阵                | 稠密矩阵               |
    | 表示方式     | 词频统计                | 神经网络嵌入           |
    | 语义关系     | 不支持                  | 支持                   |
    """)
    print("=" * 70)


if __name__ == "__main__":
    demo_vectorization()