<template>
  <div class="home">
    <div class="header-section">
      <div class="logo">
        <span class="logo-icon"><!-- logo --></span>
        <h1 class="logo-text">SentimentAI</h1>
      </div>
      <p class="subtitle">基于深度学习的商品评论情感分析系统</p>
    </div>

    <div class="card">
      <h2 class="card-title">情感分析</h2>
      
      <div class="input-group">
        <input
          v-model="comment"
          type="text"
          placeholder="请输入商品评论，例如：'这个耳机音质很棒！'"
          @keypress.enter="analyze"
          class="input-field"
        />
        <button @click="analyze" class="btn btn-primary">分析情感</button>
      </div>

      <div class="examples">
        <button 
          v-for="(example, index) in examples" 
          :key="index"
          @click="useExample(example)"
          class="example-btn"
        >
          {{ example }}
        </button>
      </div>

      <div v-if="result" class="result-card">
        <h3>分析结果</h3>
        <div class="result-item">
          <span class="label">情感倾向：</span>
          <span 
            class="value" 
            :class="{ positive: result.sentiment === '正面', negative: result.sentiment === '负面' }"
          >
            {{ result.sentiment }}
            <span v-if="result.sentiment === '正面'">✅</span>
            <span v-if="result.sentiment === '负面'">❌</span>
          </span>
        </div>
        <div class="result-item">
          <span class="label">置信度：</span>
          <span class="value">{{ (result.confidence * 100).toFixed(2) }}%</span>
        </div>
        <!-- <div class="result-item">
          <span class="label">标签：</span>
          <span class="value">{{ result.label }}</span>
        </div> -->
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomeView',
  data() {
    return {
      comment: '',
      result: null,
      error: null,
      examples: [
        '这个手机续航太差了，一天要充三次电',
        '衣服质量很好，穿着很舒服，值得购买',
        '快递很快，包装完好，客服态度也很好'
      ]
    };
  },
  methods: {
    async analyze() {
      this.result = null;
      this.error = null;
      const text = this.comment.trim();
      if (!text) {
        this.error = '请输入评论内容！';
        return;
      }
      try {
        const response = await fetch('http://localhost:8080/api/test/predict', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text })
        });
        const data = await response.json();
        if (data.code === 200 && data.data) {
          this.result = data.data;
        } else {
          this.error = '分析失败：' + (data.msg || '未知错误');
        }
      } catch (err) {
        this.error = '请求失败，请确保后端正在运行！';
        console.error('Error:', err);
      }
    },
    useExample(text) {
      this.comment = text;
      this.analyze();
    }
  }
};
</script>

<style scoped>
.home {
  padding: var(--space-6);
  margin: 0 auto;
}

.header-section {
  text-align: center;
  margin-bottom: var(--space-8);
}
.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}
.logo-icon {
  font-size: var(--font-size-2xl);
}
.logo-text {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  line-height: var(--line-height-tight);
  color: var(--primary-color);
  margin: 0;
}
.subtitle {
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  line-height: var(--line-height-normal);
  margin-top: var(--space-2);
}

.card h2 {
  color: var(--text-primary);
  margin: 0 0 var(--space-5) 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.input-group {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.input-field {
  flex: 1;
  font-size: var(--font-size-base);
  line-height: var(--line-height-normal);
  outline: none;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.input-field::placeholder {
  color: var(--text-muted);
}

.input-field:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.example-btn {
  padding: var(--space-2) var(--space-3);
  background-color: var(--input-bg);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-normal);
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
}

.example-btn:hover {
  background-color: #3a3a3a;
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.btn {
  padding: var(--space-3) var(--space-5);
  font-size: var(--font-size-base);
  line-height: var(--line-height-normal);
  font-weight: 500;
}

.result-card {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--divider-color);
}

.result-card h3 {
  color: var(--primary-color);
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-md);
  font-weight: 600;
}

.result-item {
  display: flex;
  margin-bottom: var(--space-3);
}

.label {
  color: var(--text-secondary);
  width: 100px;
  font-size: var(--font-size-sm);
}

.value {
  color: var(--text-primary);
  font-weight: 500;
  font-size: var(--font-size-base);
}

.positive {
  color: var(--primary-color) !important;
}

.negative {
  color: var(--danger-color) !important;
}
</style>