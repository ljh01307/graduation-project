<template>
  <div class="dashboard">
    <h2 class="page-title">数据看板</h2>

    <div class="card">
      <div class="filter-row">
        <div class="filter-item">
          <label>选择商品</label>
          <select v-model="selectedProductId" @change="loadProductData" class="select-field">
            <option value="">请选择商品</option>
            <option v-for="p in products" :key="p.id" :value="p.id">
              {{ p.category }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间</label>
          <input v-model="startDate" type="date" class="input-field" />
        </div>
        <div class="filter-item">
          <label>结束时间</label>
          <input v-model="endDate" type="date" class="input-field" />
        </div>
        <div class="filter-item filter-actions">
          <button @click="loadProductData" class="btn btn-primary">查询</button>
        </div>
        <div class="filter-item filter-actions">
          <button @click="resetFilters" class="btn btn-secondary">重置</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载数据中...</p>
    </div>

    <div v-else-if="selectedProductId && overview" class="charts-container">
      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-label">总评论数</span>
          <span class="stat-value">{{ overview.total }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">好评数</span>
          <span class="stat-value positive">{{ overview.positiveCount }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">差评数</span>
          <span class="stat-value negative">{{ overview.negativeCount }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">好评率</span>
          <span class="stat-value">{{ positiveRate }}%</span>
        </div>
      </div>

      <div class="charts-section">
        <div class="chart-row">
          <div class="chart-card">
            <h4>情感占比</h4>
            <div class="pie-chart-wrapper">
              <div ref="pieChart" class="pie-chart-container"></div>
            </div>
          </div>

          <div class="chart-card">
            <h4>正负数量</h4>
            <div class="bar-chart-wrapper">
              <div ref="barChart" class="bar-chart-container"></div>
            </div>
          </div>
        </div>

        <div class="chart-card full-width">
          <h4>情感趋势（按周）</h4>
          <div class="trend-chart-wrapper">
            <div ref="trendChart" class="trend-chart-container"></div>
          </div>
        </div>

        <div class="wordcloud-section">
          <div class="chart-card" v-if="wordCloud.all.length">
            <h4>全部评论词云</h4>
            <div class="wordcloud-container">
              <span
                v-for="(word, index) in wordCloud.all"
                :key="index"
                class="wordcloud-item"
                :style="{ fontSize: word.size + 'px', color: getWordColor(index, 'all') }"
              >
                {{ word.text }}
              </span>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-card" v-if="wordCloud.positive.length">
              <h4>正面评论词云</h4>
              <div class="wordcloud-container small">
                <span
                  v-for="(word, index) in wordCloud.positive"
                  :key="index"
                  class="wordcloud-item"
                  :style="{ fontSize: word.size + 'px', color: '#4CAF50' }"
                >
                  {{ word.text }}
                </span>
              </div>
            </div>

            <div class="chart-card" v-if="wordCloud.negative.length">
              <h4>负面评论词云</h4>
              <div class="wordcloud-container small">
                <span
                  v-for="(word, index) in wordCloud.negative"
                  :key="index"
                  class="wordcloud-item"
                  :style="{ fontSize: word.size + 'px', color: '#f44336' }"
                >
                  {{ word.text }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card full-width" v-if="keywordAttribution.positive.length || keywordAttribution.negative.length">
          <h4>关键词情感归因</h4>
          <div class="keyword-section">
            <div class="keyword-column" v-if="keywordAttribution.positive.length">
              <h5 class="keyword-title positive">正面关键词</h5>
              <div class="keyword-list">
                <div
                  v-for="(item, index) in keywordAttribution.positive"
                  :key="index"
                  class="keyword-item"
                >
                  <span class="keyword-text">{{ item.keyword }}</span>
                  <span class="keyword-count">{{ item.count }}次</span>
                  <div v-if="item.examples && item.examples.length" class="keyword-examples">
                    <span
                      v-for="(example, i) in item.examples.slice(0, 2)"
                      :key="i"
                      class="example-tag"
                      :title="example"
                    >{{ example.length > 20 ? example.slice(0, 20) + '...' : example }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="keyword-column" v-if="keywordAttribution.negative.length">
              <h5 class="keyword-title negative">负面关键词</h5>
              <div class="keyword-list">
                <div
                  v-for="(item, index) in keywordAttribution.negative"
                  :key="index"
                  class="keyword-item"
                >
                  <span class="keyword-text">{{ item.keyword }}</span>
                  <span class="keyword-count">{{ item.count }}次</span>
                  <div v-if="item.examples && item.examples.length" class="keyword-examples">
                    <span
                      v-for="(example, i) in item.examples.slice(0, 2)"
                      :key="i"
                      class="example-tag"
                      :title="example"
                    >{{ example.length > 20 ? example.slice(0, 20) + '...' : example }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!selectedProductId" class="empty-state">
      <p>请先选择一个商品查看数据</p>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

const allColors = ['#4CAF50', '#f44336', '#2196F3', '#FF9800', '#9C27B0', '#00BCD4', '#FF5722', '#607D8B'];

export default {
  name: 'DashboardView',
  data() {
    return {
      products: [],
      selectedProductId: '',
      startDate: '',
      endDate: '',
      loading: false,
      error: null,
      overview: null,
      weeklyData: [],
      wordCloud: {
        all: [],
        positive: [],
        negative: []
      },
      keywordAttribution: {
        positive: [],
        negative: []
      },
      pieChartInstance: null,
      barChartInstance: null,
      trendChartInstance: null,
      resizeHandler: null,
      retryCount: 0
    };
  },
  computed: {
    positivePercentage() {
      if (!this.overview || this.overview.total === 0) return 0;
      const analyzed = (this.overview.positiveCount || 0) + (this.overview.negativeCount || 0);
      return analyzed > 0 ? (this.overview.positiveCount / analyzed) * 100 : 0;
    },
    positiveRate() {
      if (!this.overview) return 0;
      return Number(this.overview.positiveRate || 0).toFixed(1);
    }
  },
  mounted() {
    this.resizeHandler = () => {
      this.handleResize();
    };
    window.addEventListener('resize', this.resizeHandler);
    this.loadProducts();
    const productId = this.$route.query.productId;
    if (productId) {
      this.selectedProductId = parseInt(productId);
      this.loadProductData();
    }
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeHandler);
    this.disposeCharts();
  },
  watch: {
    '$route.query.productId'(newVal) {
      if (newVal) {
        this.selectedProductId = parseInt(newVal);
        this.loadProductData();
      }
    }
  },
  methods: {
    getWordColor(index, type) {
      if (type === 'positive') return '#4CAF50';
      if (type === 'negative') return '#f44336';
      return allColors[index % allColors.length];
    },

    handleResize() {
      try {
        if (this.pieChartInstance) this.pieChartInstance.resize();
        if (this.barChartInstance) this.barChartInstance.resize();
        if (this.trendChartInstance) this.trendChartInstance.resize();
      } catch (e) {
        console.warn('resize error', e);
      }
    },

    disposeCharts() {
      try {
        if (this.pieChartInstance) {
          this.pieChartInstance.dispose();
          this.pieChartInstance = null;
        }
        if (this.barChartInstance) {
          this.barChartInstance.dispose();
          this.barChartInstance = null;
        }
        if (this.trendChartInstance) {
          this.trendChartInstance.dispose();
          this.trendChartInstance = null;
        }
      } catch (e) {
        console.warn('dispose error', e);
      }
    },

    resetFilters() {
      this.startDate = '';
      this.endDate = '';
      this.loadProductData();
    },

    initAndUpdateCharts() {
      console.log('开始初始化图表...');
      console.log('pieChart ref:', this.$refs.pieChart);
      console.log('barChart ref:', this.$refs.barChart);
      console.log('trendChart ref:', this.$refs.trendChart);

      this.retryCount = 0;
      this.tryInitCharts();
    },

    tryInitCharts() {
      if (!this.$refs.pieChart || !this.$refs.barChart || !this.$refs.trendChart) {
        console.warn('ref 不存在，重试', this.retryCount);
        if (this.retryCount < 10) {
          this.retryCount++;
          setTimeout(() => this.tryInitCharts(), 100);
        }
        return;
      }

      try {
        if (!this.pieChartInstance) {
          this.pieChartInstance = echarts.init(this.$refs.pieChart);
          console.log('pieChart 初始化成功');
        }
        if (!this.barChartInstance) {
          this.barChartInstance = echarts.init(this.$refs.barChart);
          console.log('barChart 初始化成功');
        }
        if (!this.trendChartInstance) {
          this.trendChartInstance = echarts.init(this.$refs.trendChart);
          console.log('trendChart 初始化成功');
        }

        this.updatePieChart();
        this.updateBarChart();
        this.updateTrendChart();
      } catch (e) {
        console.error('初始化图表失败', e);
      }
    },

    updatePieChart() {
      if (!this.pieChartInstance) {
        console.warn('pieChartInstance 不存在');
        return;
      }
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [
          {
            name: '情感占比',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 6,
              borderColor: '#1e1e1e',
              borderWidth: 2
            },
            label: {
              show: true,
              position: 'center',
              formatter: `${this.positivePercentage.toFixed(1)}%\n好评率`,
              fontSize: 18,
              fontWeight: 'bold',
              color: '#fff',
              lineHeight: 25
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 20
              }
            },
            labelLine: {
              show: false
            },
            data: [
              { value: this.overview?.positiveCount || 0, name: '正面', itemStyle: { color: '#4CAF50' } },
              { value: this.overview?.negativeCount || 0, name: '负面', itemStyle: { color: '#f44336' } }
            ]
          }
        ]
      };
      this.pieChartInstance.setOption(option, true);
      console.log('饼图数据设置完成', option.series[0].data);
    },

    updateBarChart() {
      if (!this.barChartInstance) return;

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '10%',
          right: '10%',
          bottom: '10%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['正面', '负面'],
          axisLabel: {
            color: '#aaa',
            fontSize: 12
          },
          axisLine: {
            lineStyle: {
              color: '#333'
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            color: '#aaa'
          },
          splitLine: {
            lineStyle: {
              color: '#333'
            }
          }
        },
        series: [
          {
            name: '数量',
            type: 'bar',
            barWidth: '40%',
            data: [
              { value: this.overview?.positiveCount || 0, itemStyle: { color: '#4CAF50', borderRadius: [4, 4, 0, 0] } },
              { value: this.overview?.negativeCount || 0, itemStyle: { color: '#f44336', borderRadius: [4, 4, 0, 0] } }
            ]
          }
        ]
      };
      this.barChartInstance.setOption(option, true);
      console.log('柱状图数据设置完成', option.series[0].data);
    },

    updateTrendChart() {
      if (!this.trendChartInstance) return;

      const dates = this.weeklyData.map(d => d.date);
      const positiveData = this.weeklyData.map(d => d.positive);
      const negativeData = this.weeklyData.map(d => d.negative);

      console.log('趋势图数据:', dates, positiveData, negativeData);

      const option = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['正面', '负面'],
          textStyle: {
            color: '#aaa'
          },
          top: 5
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '40px',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLabel: {
            color: '#aaa',
            fontSize: 11
          },
          axisLine: {
            lineStyle: {
              color: '#333'
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            color: '#aaa'
          },
          splitLine: {
            lineStyle: {
              color: '#333'
            }
          }
        },
        series: [
          {
            name: '正面',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2,
              color: '#4CAF50'
            },
            itemStyle: {
              color: '#4CAF50'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(76, 175, 80, 0.3)' },
                { offset: 1, color: 'rgba(76, 175, 80, 0.05)' }
              ])
            },
            data: positiveData
          },
          {
            name: '负面',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2,
              color: '#f44336'
            },
            itemStyle: {
              color: '#f44336'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(244, 67, 54, 0.3)' },
                { offset: 1, color: 'rgba(244, 67, 54, 0.05)' }
              ])
            },
            data: negativeData
          }
        ]
      };
      this.trendChartInstance.setOption(option, true);
    },

    async loadProducts() {
      try {
        const res = await fetch('http://localhost:8080/api/product/list');
        if (!res.ok) throw new Error('加载失败');
        this.products = await res.json();
      } catch (err) {
        this.error = '加载商品列表失败';
      }
    },

    async loadProductData() {
      if (!this.selectedProductId) return;

      this.loading = true;
      this.error = null;
      this.disposeCharts();

      try {
        let url = `http://localhost:8080/api/review/overview/${this.selectedProductId}`;
        const params = [];
        if (this.startDate) {
          params.push(`startTime=${this.startDate}T00:00:00`);
        }
        if (this.endDate) {
          params.push(`endTime=${this.endDate}T23:59:59`);
        }
        if (params.length) {
          url += '?' + params.join('&');
        }

        const [overviewRes, weeklyRes, wordcloudRes, keywordRes] = await Promise.all([
          fetch(url),
          fetch(`http://localhost:8080/api/review/weekly/${this.selectedProductId}?weeks=8`),
          fetch(`http://localhost:8080/api/review/wordcloud/${this.selectedProductId}?topN=30`),
          fetch(`http://localhost:8080/api/review/keyword-attribution/${this.selectedProductId}?topN=15`)
        ]);

        if (!overviewRes.ok) throw new Error('加载概览失败');
        this.overview = await overviewRes.json();
        console.log('overview数据:', this.overview);

        if (weeklyRes.ok) {
          const raw = await weeklyRes.json();
          const weeks = raw.weeks || [];
          const posList = raw.positive || [];
          const negList = raw.negative || [];
          this.weeklyData = weeks.map((date, i) => ({
            date,
            positive: posList[i] ?? 0,
            negative: negList[i] ?? 0
          }));
          console.log('weeklyData:', this.weeklyData);
        } else {
          this.weeklyData = [];
        }

        if (wordcloudRes.ok) {
          const wordcloudData = await wordcloudRes.json();
          const positive = wordcloudData.positive || [];
          const negative = wordcloudData.negative || [];
          const all = (wordcloudData.all && wordcloudData.all.length)
            ? wordcloudData.all
            : this.mergeWordCloudAll(positive, negative);
          this.wordCloud = {
            all: this.processWordCloud(all),
            positive: this.processWordCloud(positive),
            negative: this.processWordCloud(negative)
          };
        } else {
          this.wordCloud = { all: [], positive: [], negative: [] };
        }

        if (keywordRes.ok) {
          const keywordData = await keywordRes.json();
          this.keywordAttribution = {
            positive: (keywordData.positive_keywords || []).slice(0, 5),
            negative: (keywordData.negative_keywords || []).slice(0, 5)
          };
        } else {
          this.keywordAttribution = { positive: [], negative: [] };
        }

        await this.$nextTick();
        console.log('DOM 更新完成，准备初始化图表');

        setTimeout(() => {
          this.initAndUpdateCharts();
        }, 200);

      } catch (err) {
        this.error = '加载数据失败：' + err.message;
        console.error(err);
      } finally {
        this.loading = false;
      }
    },

    mergeWordCloudAll(positive, negative) {
      const map = new Map();
      for (const w of positive) {
        map.set(w.word, (map.get(w.word) || 0) + w.frequency);
      }
      for (const w of negative) {
        map.set(w.word, (map.get(w.word) || 0) + w.frequency);
      }
      return Array.from(map.entries()).map(([word, frequency]) => ({ word, frequency }));
    },

    processWordCloud(words) {
      const list = (words || []).filter(w => w && w.word != null && Number(w.frequency) > 0);
      if (!list.length) return [];
      const maxFreq = Math.max(...list.map(w => Number(w.frequency)), 1);
      return list.map(w => ({
        text: String(w.word),
        size: 14 + (Number(w.frequency) / maxFreq) * 26,
        frequency: Number(w.frequency)
      }));
    }
  }
};
</script>

<style scoped>
.dashboard {
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  margin: 0 0 var(--space-6) 0;
  font-size: var(--font-size-xl);
  color: var(--primary-color);
}

.card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-6);
  border: 1px solid var(--border-color);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 150px;
}

.filter-item label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.filter-actions {
  display: flex;
  gap: var(--space-2);
}

.select-field,
.input-field {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--input-bg);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.btn {
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: all 0.2s;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-hover);
}

.btn-secondary {
  background: var(--input-bg);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background: var(--border-color);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  text-align: center;
}

.stat-label {
  display: block;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-2);
}

.stat-value {
  display: block;
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--text-primary);
}

.stat-value.positive {
  color: var(--primary-color);
}

.stat-value.negative {
  color: var(--danger-color);
}

.charts-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.chart-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-card h4 {
  margin: 0 0 var(--space-4) 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.pie-chart-wrapper,
.bar-chart-wrapper,
.trend-chart-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.pie-chart-container,
.bar-chart-container {
  width: 100%;
  height: 220px;
  background: #1a1a1a;
  border-radius: 8px;
}

.trend-chart-container {
  width: 100%;
  height: 280px;
  background: #1a1a1a;
  border-radius: 8px;
}

.wordcloud-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.wordcloud-container {
  min-height: 200px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--space-3) var(--space-4);
  padding: var(--space-4);
  background: var(--input-bg);
  border-radius: var(--radius-sm);
}

.wordcloud-container.small {
  min-height: 150px;
}

.wordcloud-item {
  display: inline-block;
  transition: transform 0.2s ease;
  cursor: default;
}

.wordcloud-item:hover {
  transform: scale(1.1);
}

.keyword-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
}

.keyword-column {
  background: var(--input-bg);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
}

.keyword-title {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-base);
  font-weight: 600;
}

.keyword-title.positive {
  color: var(--primary-color);
}

.keyword-title.negative {
  color: var(--danger-color);
}

.keyword-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.keyword-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--card-bg);
  border-radius: var(--radius-sm);
}

.keyword-text {
  font-weight: 500;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.keyword-count {
  background: var(--border-color);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  color: var(--text-secondary);
}

.keyword-examples {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.example-tag {
  font-size: 11px;
  background: var(--input-bg);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  max-width: 150px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.example-tag:hover {
  background: var(--border-color);
}

.loading-state {
  text-align: center;
  padding: var(--space-10);
  color: var(--text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(76, 175, 80, 0.3);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto var(--space-4);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: var(--space-10);
  color: var(--text-muted);
}

.alert-error {
  background: rgba(244, 67, 54, 0.1);
  color: #ef5350;
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  margin-top: var(--space-4);
  border-left: 4px solid var(--danger-color);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-row {
    grid-template-columns: 1fr;
  }

  .keyword-section {
    grid-template-columns: 1fr;
  }
}
</style>
