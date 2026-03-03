# 商品评论情感分类及可视化系统

基于机器学习（TF-IDF + 逻辑回归/SVM）的电商评论情感分析系统，支持商品管理、评论批量分析、情感可视化展示。适合作为毕业设计或课程项目。

## 📋 项目结构

```
graduation-project/
├── E-commerce Review Sentiment Classification/   # 机器学习模型模块
│   ├── data/                                     # 数据集
│   ├── models/                                   # 训练好的模型
│   ├── scripts/                                  # 模型服务脚本
│   └── src/                                      # 训练与预测代码
├── sentiment-analysis-backend/                   # Java 后端服务
│   └── src/main/java/com/graduation/            # SpringBoot 业务代码
├── sentiment-analysis-frontend/                  # Vue3 前端界面
│   └── src/views/                               # 页面组件
└── README.md                                     # 本文件
```

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + ECharts |
| 后端 | Java 17 + Spring Boot 3.1 + MySQL |
| 机器学习 | Python + scikit-learn + jieba + TF-IDF |
| 可视化 | ECharts 图表 + CSS 词云 |

## 🚀 快速启动

### 1. 启动机器学习模型服务

```bash
cd "E-commerce Review Sentiment Classification"
pip install -r requirements.txt
python scripts/model_service.py
```

模型服务默认运行在 `http://localhost:5000`

### 2. 启动 Java 后端

```bash
cd sentiment-analysis-backend
# 使用 Maven 运行
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

### 3. 启动前端

```bash
cd sentiment-analysis-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

## 📊 功能模块

### 商品管理
- 添加、编辑、删除商品
- 按名称/时间筛选商品
- 查看商品评论统计

### 评论管理
- 批量导入评论（CSV 格式）
- 单条添加评论
- 按商品/时间/情感筛选评论
- 触发情感分析

### 数据看板
- **情感占比饼图** - 展示正负评论比例
- **正负数量柱状图** - 直观对比数量
- **情感趋势折线图** - 8周情感走势
- **词云展示** - 全部/正面/负面评论词云
- **关键词归因** - 提取高频情感关键词

## 📁 数据格式

### 训练数据格式（CSV）
```csv
text,label
"商品质量很好，物流很快",pos
"太差了，完全不值这个价",neg
```

### 评论导入格式（CSV）
```csv
content,reviewTime
"这个手机很好用","2024-01-15 10:30:00"
"物流太慢了","2024-01-16 14:20:00"
```

## 🔧 模型训练

```bash
cd "E-commerce Review Sentiment Classification"
python src/train.py --data_path data/sample_reviews.csv --model_path models/best_lr_model.pkl
```

支持的模型：
- 逻辑回归（Logistic Regression）
- 支持向量机（SVM）

## 📈 API 接口

### 后端 API（Java）

| 接口 | 说明 |
|------|------|
| `GET /api/product/list` | 获取商品列表 |
| `POST /api/product` | 添加商品 |
| `GET /api/review/list/{productId}` | 获取商品评论 |
| `POST /api/review/batch` | 批量导入评论 |
| `POST /api/review/analyze/{productId}` | 执行情感分析 |
| `GET /api/review/overview/{productId}` | 获取概览统计 |
| `GET /api/review/weekly/{productId}` | 获取周度趋势 |
| `GET /api/review/wordcloud/{productId}` | 获取词云数据 |

### 模型服务 API（Python）

| 接口 | 说明 |
|------|------|
| `POST /predict` | 单条预测 |
| `POST /predict_batch` | 批量预测 |

## 💡 系统亮点

1. **完整的端到端流程** - 从数据上传到可视化展示的全流程
2. **可插拔的机器学习模型** - 支持多种分类器对比
3. **丰富的可视化** - ECharts 图表 + 词云 + 关键词归因
4. **按周分析** - 贴近实际电商运营场景
5. **前后端分离** - 现代化的 Web 架构

## 📝 开发计划

- [x] 基础情感分类模型
- [x] 商品管理功能
- [x] 评论管理功能
- [x] 数据可视化看板
- [x] 词云展示
- [x] 关键词归因
- [ ] 用户认证系统
- [ ] 多模型对比功能
- [ ] 数据导出功能

## 📄 许可证

MIT License

---

如有问题或建议，欢迎提交 Issue 或 PR。
