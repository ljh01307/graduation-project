import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

cm = np.array([
    [4116, 714],
    [1374, 3528]
])

fig, ax = plt.subplots(figsize=(8, 6))

sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=['预测差评', '预测好评'],
            yticklabels=['实际差评', '实际好评'],
            annot_kws={'size': 16},
            ax=ax)

ax.set_xlabel('预测标签', fontsize=14)
ax.set_ylabel('实际标签', fontsize=14)
ax.set_title('混淆矩阵', fontsize=16)

plt.tight_layout()
plt.savefig('confusion_matrix1.png', dpi=300, bbox_inches='tight')
plt.savefig('confusion_matrix1.pdf', bbox_inches='tight')
print("图片已保存: confusion_matrix.png, confusion_matrix.pdf")
plt.show()