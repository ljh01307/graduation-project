<template>
  <div class="comment-management">
    <h2 class="page-title">评论管理</h2>

    <div class="card">
      <h3 class="card-title">筛选条件</h3>
      <div class="filter-row">
        <div class="filter-item">
          <label>商品</label>
          <select v-model="filters.productId" @change="loadReviews" class="select-field">
            <option value="">全部商品</option>
            <option v-for="p in products" :key="p.id" :value="p.id">
              {{ p.category }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>情感标签</label>
          <select v-model="filters.sentimentLabel" @change="loadReviews" class="select-field">
            <option value="">全部</option>
            <option :value="1">正面</option>
            <option :value="0">负面</option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间</label>
          <input v-model="filters.startDate" type="date" @change="loadReviews" class="input-field" />
        </div>
        <div class="filter-item">
          <label>结束时间</label>
          <input v-model="filters.endDate" type="date" @change="loadReviews" class="input-field" />
        </div>
        <div class="filter-item">
          <label>排序</label>
          <select v-model="filters.sortBy" @change="loadReviews" class="select-field">
            <option value="uploadTime">上传时间</option>
            <option value="confidence">置信度</option>
          </select>
        </div>
        <div class="filter-item">
          <label>顺序</label>
          <select v-model="filters.sortDir" @change="loadReviews" class="select-field">
            <option value="DESC">降序</option>
            <option value="ASC">升序</option>
          </select>
        </div>
        <div class="filter-item filter-actions">
          <button @click="resetFilters" class="btn btn-secondary btn-sm">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">
          评论列表（{{ pagination.totalElements }} 条）
          <span v-if="unanalyzedCount > 0" class="unanalyzed-badge">待分析 {{ unanalyzedCount }} 条</span>
        </h3>
        <div class="card-actions">
          <button
            v-if="unanalyzedCount > 0"
            @click="analyzeReviews"
            class="btn btn-primary btn-sm"
            :disabled="analyzing"
          >
            {{ analyzing ? '分析中...' : '分析待审核' }}
          </button>
          <button @click="showUploadModal = true" class="btn btn-outline btn-sm">上传评论</button>
        </div>
      </div>

      <div v-if="loading" class="center-text">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="reviews.length === 0" class="empty-state">
        <p>暂无评论数据</p>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>商品</th>
            <th>评论内容</th>
            <th>情感标签</th>
            <th>置信度</th>
            <th>上传时间</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="review in reviews" :key="review.id">
            <td>{{ review.productCategory }}</td>
            <td class="content-cell">{{ review.content }}</td>
            <td>
              <span v-if="review.analyzed" :class="['tag', review.sentimentLabel === 1 ? 'tag-positive' : 'tag-negative']">
                {{ review.sentimentLabel === 1 ? '正面' : '负面' }}
              </span>
              <span v-else class="tag tag-pending">未分析</span>
            </td>
            <td>
              <span v-if="review.confidence !== null">
                {{ (review.confidence * 100).toFixed(1) }}%
              </span>
              <span v-else>-</span>
            </td>
            <td>{{ formatDate(review.uploadTime) }}</td>
            <td>
              <button @click="openEditModal(review)" class="btn btn-outline btn-sm" title="修正标签">
                修正
              </button>
              <button @click="deleteReview(review.id)" class="btn btn-danger btn-sm" title="删除">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="pagination.totalPages > 1" class="pagination">
        <button 
          @click="goToPage(pagination.currentPage - 1)" 
          :disabled="pagination.currentPage === 0"
          class="btn btn-secondary btn-sm"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ pagination.currentPage + 1 }} 页 / 共 {{ pagination.totalPages }} 页
        </span>
        <button 
          @click="goToPage(pagination.currentPage + 1)" 
          :disabled="pagination.currentPage >= pagination.totalPages - 1"
          class="btn btn-secondary btn-sm"
        >
          下一页
        </button>
      </div>
    </div>

    <div v-if="message" :class="['alert', messageType === 'error' ? 'alert-error' : 'alert-success']">
      {{ message }}
    </div>

    <div v-if="showUploadModal" class="modal-overlay" @click.self="closeUploadModal">
      <div class="modal modal-large">
        <div class="modal-header">
          <h3 class="modal-title">上传评论</h3>
          <button @click="closeUploadModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="upload-section">
            <label>选择商品 *</label>
            <select v-model="uploadForm.productId" class="select-field">
              <option value="">请选择商品</option>
              <option v-for="p in products" :key="p.id" :value="p.id">
                {{ p.category }}
              </option>
            </select>
          </div>
          <div class="upload-tabs">
            <button 
              @click="uploadTab = 'text'" 
              :class="['tab-btn', uploadTab === 'text' ? 'active' : '']"
            >
              文本输入
            </button>
            <button 
              @click="uploadTab = 'csv'" 
              :class="['tab-btn', uploadTab === 'csv' ? 'active' : '']"
            >
              CSV上传
            </button>
            <button 
              @click="uploadTab = 'json'" 
              :class="['tab-btn', uploadTab === 'json' ? 'active' : '']"
            >
              JSON上传
            </button>
          </div>

          <div v-if="uploadTab === 'text'" class="upload-section">
            <label>评论内容（每行一条）</label>
            <textarea v-model="uploadForm.reviewText" rows="8" class="textarea-field" placeholder="请输入评论，每行一条..."></textarea>
            <button @click="uploadText" class="btn btn-primary" :disabled="!uploadForm.productId || !uploadForm.reviewText.trim()">
              上传评论
            </button>
          </div>

          <div v-if="uploadTab === 'csv'" class="upload-section">
            <label>选择CSV文件</label>
            <input type="file" accept=".csv" @change="handleCSVSelect" class="file-input" />
            <p v-if="uploadForm.csvFile" class="file-name">已选择: {{ uploadForm.csvFile.name }}</p>
            <button @click="uploadCSV" class="btn btn-primary" :disabled="!uploadForm.productId || !uploadForm.csvFile">
              上传CSV
            </button>
          </div>

          <div v-if="uploadTab === 'json'" class="upload-section">
            <label>选择JSON文件</label>
            <input type="file" accept=".json" @change="handleJSONSelect" class="file-input" />
            <p v-if="uploadForm.jsonFile" class="file-name">已选择: {{ uploadForm.jsonFile.name }}</p>
            <button @click="uploadJSON" class="btn btn-primary" :disabled="!uploadForm.productId || !uploadForm.jsonFile">
              上传JSON
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <h3 class="modal-title">修正</h3>
        <p class="review-preview">{{ editingReview.content }}</p>
        <div class="sentiment-options">
          <label :class="['option-btn', editingSentiment === 1 ? 'active positive' : '']">
            <input type="radio" v-model="editingSentiment" :value="1" />
            <span>正面</span>
          </label>
          <label :class="['option-btn', editingSentiment === 0 ? 'active negative' : '']">
            <input type="radio" v-model="editingSentiment" :value="0" />
            <span>负面</span>
          </label>
        </div>
        <div class="modal-actions">
          <button @click="closeEditModal" class="btn btn-secondary">取消</button>
          <button @click="saveSentimentEdit" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CommentView',
  data() {
    return {
      products: [],
      reviews: [],
      loading: false,
      message: '',
      messageType: 'success',
      filters: {
        productId: '',
        sentimentLabel: '',
        startDate: '',
        endDate: '',
        sortBy: 'uploadTime',
        sortDir: 'DESC'
      },
      pagination: {
        currentPage: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0
      },
      showUploadModal: false,
      showEditModal: false,
      uploadTab: 'text',
      uploadForm: {
        productId: '',
        reviewText: '',
        csvFile: null,
        jsonFile: null
      },
      editingReview: { id: null, content: '' },
      editingSentiment: 1,
      unanalyzedCount: 0,
      analyzing: false
    };
  },
  async mounted() {
    await this.loadProducts();
    const productId = this.$route.query.productId;
    if (productId) {
      this.filters.productId = parseInt(productId);
    }
    await this.loadReviews();
    await this.loadUnanalyzedCount();
  },
  watch: {
    '$route.query.productId'(newVal) {
      if (newVal) {
        this.filters.productId = parseInt(newVal);
        this.loadReviews();
      }
    }
  },
  methods: {
    formatDate(dateStr) {
      if (!dateStr) return '-';
      const date = new Date(dateStr);
      return date.toLocaleString('zh-CN');
    },

    showMessage(msg, type = 'success') {
      this.message = msg;
      this.messageType = type;
      setTimeout(() => this.message = '', 3000);
    },

    async loadProducts() {
      try {
        const res = await fetch('http://localhost:8080/api/product/list');
        this.products = await res.json();
      } catch (err) {
        this.showMessage('加载商品失败', 'error');
      }
    },

    resetFilters() {
      this.filters = {
        productId: '',
        sentimentLabel: '',
        startDate: '',
        endDate: '',
        sortBy: 'uploadTime',
        sortDir: 'DESC'
      };
      this.pagination.currentPage = 0;
      this.loadReviews();
      this.loadUnanalyzedCount();
    },

    async loadReviews() {
      this.loading = true;
      try {
        let url = `http://localhost:8080/api/review/list-all?`;
        url += `page=${this.pagination.currentPage}`;
        url += `&size=${this.pagination.size}`;
        url += `&sortBy=${this.filters.sortBy}`;
        url += `&sortDir=${this.filters.sortDir}`;
        
        if (this.filters.productId) {
          url += `&productId=${this.filters.productId}`;
        }
        if (this.filters.sentimentLabel !== '') {
          url += `&sentimentLabel=${this.filters.sentimentLabel}`;
        }
        if (this.filters.startDate) {
          url += `&startTime=${this.filters.startDate}T00:00:00`;
        }
        if (this.filters.endDate) {
          url += `&endTime=${this.filters.endDate}T23:59:59`;
        }

        const res = await fetch(url);
        const data = await res.json();
        
        this.reviews = data.reviews || [];
        this.pagination.totalElements = data.totalElements || 0;
        this.pagination.totalPages = data.totalPages || 0;
        this.pagination.currentPage = data.currentPage || 0;
        this.loadUnanalyzedCount();
      } catch (err) {
        this.showMessage('加载评论失败', 'error');
      } finally {
        this.loading = false;
      }
    },

    goToPage(page) {
      this.pagination.currentPage = page;
      this.loadReviews();
    },

    async loadUnanalyzedCount() {
      try {
        let url = 'http://localhost:8080/api/review/unanalyzed-count';
        if (this.filters.productId) {
          url += `/${this.filters.productId}`;
        }
        const res = await fetch(url);
        const data = await res.json();
        this.unanalyzedCount = data.count || 0;
      } catch (err) {
        console.error('加载未分析数量失败:', err);
      }
    },

    async analyzeReviews() {
      if (this.analyzing) return;
      
      this.analyzing = true;
      try {
        let url = 'http://localhost:8080/api/review/analyze-all';
        if (this.filters.productId) {
          url = `http://localhost:8080/api/review/analyze/${this.filters.productId}`;
        }
        const res = await fetch(url, { method: 'POST' });
        const result = await res.text();
        this.showMessage(result, 'success');
        await this.loadReviews();
        await this.loadUnanalyzedCount();
      } catch (err) {
        this.showMessage('分析失败：' + err.message, 'error');
      } finally {
        this.analyzing = false;
      }
    },

    closeUploadModal() {
      this.showUploadModal = false;
      this.uploadForm = {
        productId: '',
        reviewText: '',
        csvFile: null,
        jsonFile: null
      };
    },

    handleCSVSelect(event) {
      this.uploadForm.csvFile = event.target.files[0];
    },

    handleJSONSelect(event) {
      this.uploadForm.jsonFile = event.target.files[0];
    },

    async uploadText() {
      const lines = this.uploadForm.reviewText.split('\n')
        .map(line => line.trim())
        .filter(line => line);
        
      if (!lines.length) {
        this.showMessage('请输入评论', 'error');
        return;
      }

      try {
        const res = await fetch('http://localhost:8080/api/review/upload', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            productId: this.uploadForm.productId,
            contents: lines
          })
        });
        
        if (res.ok) {
          this.showMessage(`上传成功，共 ${lines.length} 条评论`);
          this.closeUploadModal();
          this.loadReviews();
        } else {
          throw new Error('上传失败');
        }
      } catch (err) {
        this.showMessage('上传失败', 'error');
      }
    },

    async uploadCSV() {
      const formData = new FormData();
      formData.append('file', this.uploadForm.csvFile);
      formData.append('productId', this.uploadForm.productId);

      try {
        const res = await fetch('http://localhost:8080/api/review/upload/csv', {
          method: 'POST',
          body: formData
        });

        const data = await res.json();
        
        if (data.code === 200) {
          this.showMessage(`上传成功，共导入 ${data.count || 0} 条评论`);
          this.closeUploadModal();
          this.loadReviews();
        } else {
          throw new Error(data.message || '上传失败');
        }
      } catch (err) {
        this.showMessage('上传CSV失败', 'error');
      }
    },

    async uploadJSON() {
      const reader = new FileReader();
      reader.onload = async (e) => {
        try {
          const jsonData = JSON.parse(e.target.result);
          
          let contents = [];
          if (Array.isArray(jsonData)) {
            contents = jsonData;
          } else if (jsonData.comments && Array.isArray(jsonData.comments)) {
            contents = jsonData.comments;
          } else if (jsonData.data && Array.isArray(jsonData.data)) {
            contents = jsonData.data;
          } else {
            contents = Object.values(jsonData).filter(v => typeof v === 'string');
          }

          contents = contents.filter(c => c && typeof c === 'string' && c.trim());

          if (contents.length === 0) {
            this.showMessage('JSON文件中没有找到有效的评论', 'error');
            return;
          }

          const res = await fetch('http://localhost:8080/api/review/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              productId: this.uploadForm.productId,
              contents: contents
            })
          });

          if (res.ok) {
            this.showMessage(`上传成功，共 ${contents.length} 条评论`);
            this.closeUploadModal();
            this.loadReviews();
          } else {
            throw new Error('上传失败');
          }
        } catch (err) {
          this.showMessage('解析JSON失败', 'error');
        }
      };
      reader.readAsText(this.uploadForm.jsonFile);
    },

    openEditModal(review) {
      this.editingReview = { id: review.id, content: review.content };
      this.editingSentiment = review.sentimentLabel;
      this.showEditModal = true;
    },

    closeEditModal() {
      this.showEditModal = false;
      this.editingReview = { id: null, content: '' };
    },

    async saveSentimentEdit() {
      try {
        const res = await fetch(`http://localhost:8080/api/review/update-sentiment/${this.editingReview.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sentimentLabel: this.editingSentiment })
        });

        if (res.ok) {
          this.showMessage('修正成功');
          this.closeEditModal();
          this.loadReviews();
        } else {
          throw new Error('修正失败');
        }
      } catch (err) {
        this.showMessage('修正失败', 'error');
      }
    },

    deleteReview(id) {
      if (!confirm('确定删除这条评论？')) return;

      (async () => {
        try {
          const res = await fetch(`http://localhost:8080/api/review/delete/${id}`, {
            method: 'DELETE'
          });

          if (res.ok) {
            this.showMessage('删除成功');
            this.loadReviews();
          } else {
            throw new Error('删除失败');
          }
        } catch (err) {
          this.showMessage('删除失败', 'error');
        }
      })();
    }
  }
};
</script>

<style scoped>
.comment-management {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.card-title {
  margin: 0;
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.unanalyzed-badge {
  display: inline-block;
  margin-left: 10px;
  padding: 2px 8px;
  background-color: #fff3e0;
  color: #e65100;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
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
  min-width: 140px;
}

.filter-item label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.filter-actions {
  margin-left: auto;
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

.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: var(--space-4);
}

.data-table th,
.data-table td {
  padding: var(--space-3);
  text-align: left;
  border-bottom: 1px solid var(--divider-color);
  font-size: var(--font-size-sm);
}

.data-table th {
  color: var(--text-secondary);
  font-weight: 600;
}

.data-table .col-actions {
  width: 120px;
}

.content-cell {
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.tag-positive {
  background: rgba(76, 175, 80, 0.15);
  color: var(--primary-color);
}

.tag-negative {
  background: rgba(244, 67, 54, 0.15);
  color: var(--danger-color);
}

.tag-pending {
  background: rgba(158, 158, 158, 0.15);
  color: #9e9e9e;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--divider-color);
}

.page-info {
  color: var(--text-secondary);
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

.btn-primary:hover:not(:disabled) {
  background: var(--primary-hover);
}

.btn-secondary {
  background: var(--input-bg);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--border-color);
}

.btn-outline {
  background: transparent;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
}

.btn-outline:hover {
  background: var(--primary-color);
  color: white;
}

.btn-danger {
  background: var(--danger-color);
  color: white;
}

.btn-danger:hover {
  background: var(--danger-hover);
}

.btn-sm {
  padding: var(--space-1) var(--space-2);
  font-size: 12px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.center-text {
  text-align: center;
  padding: var(--space-10) 0;
  color: var(--text-secondary);
}

.empty-state {
  text-align: center;
  padding: var(--space-10) 0;
  color: var(--text-muted);
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(76, 175, 80, 0.3);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto var(--space-3);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.alert {
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  position: fixed;
  bottom: 20px;
  right: 20px;
  min-width: 200px;
  z-index: 1001;
}

.alert-error {
  background: rgba(244, 67, 54, 0.9);
  color: white;
}

.alert-success {
  background: rgba(76, 175, 80, 0.9);
  color: white;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  min-width: 400px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-large {
  min-width: 600px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.modal-title {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-secondary);
  cursor: pointer;
}

.close-btn:hover {
  color: var(--text-primary);
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.upload-tabs {
  display: flex;
  gap: var(--space-2);
}

.tab-btn {
  flex: 1;
  padding: var(--space-2);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--input-bg);
  color: var(--text-secondary);
  cursor: pointer;
}

.tab-btn.active {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.upload-section > label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.textarea-field {
  width: 100%;
  padding: var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--input-bg);
  color: var(--text-primary);
  resize: vertical;
  box-sizing: border-box;
}

.file-input {
  padding: var(--space-2);
  color: var(--text-primary);
}

.file-name {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin: 0;
}

.review-preview {
  background: var(--input-bg);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-4);
  word-break: break-all;
}

.sentiment-options {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.option-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 2px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.option-btn input {
  display: none;
}

.option-btn.active.positive {
  border-color: var(--primary-color);
  background: rgba(76, 175, 80, 0.1);
  color: var(--primary-color);
}

.option-btn.active.negative {
  border-color: var(--danger-color);
  background: rgba(244, 67, 54, 0.1);
  color: var(--danger-color);
}

.modal-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}
</style>
