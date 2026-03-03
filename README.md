# 电商评论情感分类（机器学习版）

本项目使用 **传统机器学习方法（TF-IDF + 逻辑回归）** 对电商商品评论进行情感分类（正面 / 负面），适合作为毕业设计或课程项目基础代码。

### 1. 环境准备

```bash
cd "E-commerce Review Sentiment Classification"
pip install -r requirements.txt
```

> Windows 下在 PowerShell 中同样使用 `pip` 或 `python -m pip` 安装即可。

### 2. 数据格式

默认训练数据路径：`data/sample_reviews.csv`  
CSV 文件需包含两列：

- `text`：评论文本
- `label`：情感标签（示例中使用 `pos` / `neg`，你也可以改成 0/1）

### 3. 训练模型

```bash
python src/train.py --data_path data/sample_reviews.csv --model_path model.joblib
```

脚本会完成：

1. 读取 CSV 数据
2. 文本清洗和分词（简单规则版，支持中英文混合）
3. 使用 TF-IDF 提取特征
4. 训练逻辑回归分类器
5. 将「向量化器 + 模型」一起保存到 `model.joblib`

### 4. 预测情感

对单条文本预测：

```bash
python src/predict.py --model_path model.joblib --text "这个手机质量很好，物流也很快"
```

对批量数据（CSV 文件）预测：

```bash
python src/predict.py --model_path model.joblib --input_csv data/sample_reviews.csv --output_csv data/predicted.csv
```

### 5. 你可以扩展的方向

- 替换为你自己的真实电商评论数据集  
- 将标签扩展为三分类：负面 / 中性 / 正面  
- 对比不同分类器（SVM、随机森林、XGBoost 等）  
- 增加更复杂的中文分词和停用词处理  
- 加入简单的 Web 界面或 API 服务进行在线预测

如果你告诉我你的具体毕业设计要求（比如：准确率指标、需要对比几种算法、是否需要可视化等），我可以在这个基础上继续帮你完善结构和报告思路。

