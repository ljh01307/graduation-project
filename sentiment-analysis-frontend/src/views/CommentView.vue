<template>
  <div class="comment-management">
    <h2 class="page-title">评论管理</h2>

    <div class="card">
      <h3 class="card-title">筛选条件</h3>
      <div class="filter-row">
        <div class="filter-item">
          <label>选择商品</label>
          <select v-model="filters.productId" @change="onFilterChange" class="select-field">
            <option value="">全部商品</option>
            <option v-for="p in products" :key="p.id" :value="p.id">
              {{ p.category }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>情感标签</label>
          <select v-model="filters.sentimentLabel" @change="onFilterChange" class="select-field">
            <option value="">全部</option>
            <option :value="1">正面</option>
            <option :value="0">负面</option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间</label>
          <input v-model="filters.startDate" type="date" @change="onFilterChange" class="input-field" />
        </div>
        <div class="filter-item">
          <label>结束时间</label>
          <input v-model="filters.endDate" type="date" @change="onFilterChange" class="input-field" />
        </div>
        <div class="filter-item">
          <label>排序</label>
          <select v-model="filters.sortBy" @change="onFilterChange" class="select-field">
            <option value="uploadTime">上传时间</option>
            <option value="confidence">置信度</option>
          </select>
        </div>
        <div class="filter-item">
          <label>顺序</label>
          <select v-model="filters.sortDir" @change="onFilterChange" class="select-field">
            <option value="DESC">降序</option>
            <option value="ASC">升序</option>
          </select>
        </div>
        <div class="filter-item filter-actions">
          <button @click="resetFilters" class="btn btn-secondary"><IconRefresh /> 重置</button>
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
            <IconChart v-if="!analyzing" /> {{ analyzing ? '分析中...' : '启动分类' }}
          </button>
          <button @click="showUploadModal = true" class="btn btn-outline btn-sm"><IconUpload /> 上传评论</button>
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
            <th class="col-actions">编辑</th>
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
            <td class="actions-cell">
              <div class="dropdown" :class="{ 'dropdown-open': openDropdownId === review.id }">
                <button @click="toggleDropdown(review.id)" class="btn btn-outline btn-sm dropdown-btn icon-only" title="^_^">
                  <IconEdit />
                </button>
                <div class="dropdown-menu">
                  <button @click="openEditModal(review); closeDropdown()" class="dropdown-item">
                    <IconEdit /> 修正标签
                  </button>
                  <div class="dropdown-divider"></div>
                  <button @click="deleteReview(review.id); closeDropdown()" class="dropdown-item danger">
                    <IconTrash /> 删除
                  </button>
                </div>
              </div>
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

    <div v-if="error" class="alert alert-error">
      {{ error }}
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
            <label>选择商品</label>
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
import { getToken } from '../utils/auth.js';
import IconRefresh from '../components/icons/IconRefresh.vue';
import IconChart from '../components/icons/IconChart.vue';
import IconUpload from '../components/icons/IconUpload.vue';
import IconEdit from '../components/icons/IconEdit.vue';
import IconTrash from '../components/icons/IconTrash.vue';

const API_BASE = '/api';

async function apiRequest(url, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...options.headers
  };

  if (options.body instanceof FormData) {
    delete headers['Content-Type'];
  }

  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers
  });

  const contentType = response.headers.get('content-type');
  let data;
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    const text = await response.text();
    if (!response.ok) {
      throw new Error(text || '请求失败');
    }
    return text;
  }

  if (!response.ok) {
    throw new Error(data.message || '请求失败');
  }

  if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
    if (data.code !== 200) {
      throw new Error(data.message || '请求失败');
    }
    return data.data;
  }

  return data;
}

export default {
  name: 'CommentView',
  components: {
    IconRefresh,
    IconChart,
    IconUpload,
    IconEdit,
    IconTrash
  },
  props: {
    manageUserId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      products: [],
      reviews: [],
      loading: false,
      error: null,
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
      analyzing: false,
      openDropdownId: null
    };
  },
  watch: {
    manageUserId: {
      handler() {
        this.loadProducts();
        this.loadReviews();
        this.loadUnanalyzedCount();
      }
    },
    '$route.query.productId'(newVal) {
      if (newVal) {
        this.filters.productId = parseInt(newVal);
        this.loadReviews();
      }
    }
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

    buildQueryString() {
      const params = new URLSearchParams();
      if (this.manageUserId) params.append('manageUserId', this.manageUserId);
      params.append('page', this.pagination.currentPage);
      params.append('size', this.pagination.size);
      params.append('sortBy', this.filters.sortBy);
      params.append('sortDir', this.filters.sortDir);
      if (this.filters.productId) params.append('productId', this.filters.productId);
      if (this.filters.sentimentLabel !== '') params.append('sentimentLabel', this.filters.sentimentLabel);
      if (this.filters.startDate) params.append('startTime', `${this.filters.startDate}T00:00:00`);
      if (this.filters.endDate) params.append('endTime', `${this.filters.endDate}T23:59:59`);
      return params.toString();
    },

    async loadProducts() {
      try {
        const params = new URLSearchParams();
        if (this.manageUserId) params.append('manageUserId', this.manageUserId);
        const query = params.toString() ? '?' + params.toString() : '';
        const data = await apiRequest(`/product/list${query}`);
        this.products = data || [];
      } catch (err) {
        this.error = '加载商品失败：' + err.message;
      }
    },

    onFilterChange() {
      this.pagination.currentPage = 0;
      this.loadReviews();
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
      this.error = null;
      try {
        const queryString = this.buildQueryString();
        const data = await apiRequest(`/review/list-all?${queryString}`);

        this.reviews = data.reviews || [];
        this.pagination.totalElements = data.totalElements || 0;
        this.pagination.totalPages = data.totalPages || 0;
        this.pagination.currentPage = data.currentPage || 0;
        this.loadUnanalyzedCount();
      } catch (err) {
        this.error = '加载评论失败：' + err.message;
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
        let url = '/review/unanalyzed-count';
        if (this.filters.productId) {
          url += `/${this.filters.productId}`;
        }
        const params = new URLSearchParams();
        if (this.manageUserId) params.append('manageUserId', this.manageUserId);
        const query = params.toString() ? '?' + params.toString() : '';
        const data = await apiRequest(`${url}${query}`);
        this.unanalyzedCount = typeof data === 'number' ? data : (data.count || 0);
      } catch (err) {
        console.error('加载未分析数量失败:', err);
      }
    },

    async analyzeReviews() {
      if (this.analyzing) return;

      this.analyzing = true;
      try {
        let url = '/review/analyze-all';
        if (this.filters.productId) {
          url = `/review/analyze/${this.filters.productId}`;
        }
        const params = new URLSearchParams();
        if (this.manageUserId) params.append('manageUserId', this.manageUserId);
        const query = params.toString() ? '?' + params.toString() : '';
        await apiRequest(`${url}${query}`, { method: 'POST' });
        this.showMessage('分析任务已启动，请稍后查看结果', 'success');
        await this.loadReviews();
        await this.loadUnanalyzedCount();
      } catch (err) {
        this.error = '分析失败：' + err.message;
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
        this.error = '请输入评论';
        return;
      }

      try {
        const params = new URLSearchParams();
        if (this.manageUserId) params.append('manageUserId', this.manageUserId);
        const query = params.toString() ? '?' + params.toString() : '';
        await apiRequest(`/review/upload${query}`, {
          method: 'POST',
          body: JSON.stringify({
            productId: this.uploadForm.productId,
            contents: lines
          })
        });
        this.showMessage(`上传成功，共 ${lines.length} 条评论`);
        this.closeUploadModal();
        this.loadReviews();
      } catch (err) {
        this.error = '上传失败：' + err.message;
      }
    },

    async uploadCSV() {
      const formData = new FormData();
      formData.append('file', this.uploadForm.csvFile);
      formData.append('productId', this.uploadForm.productId);
      if (this.manageUserId) formData.append('manageUserId', this.manageUserId);

      try {
        const token = getToken();
        const response = await fetch(`${API_BASE}/review/upload/csv`, {
          method: 'POST',
          headers: {
            ...(token && { 'Authorization': `Bearer ${token}` })
          },
          body: formData
        });

        const data = await response.json();

        if (data.code === 200) {
          this.showMessage(`上传成功，共导入 ${data.data || 0} 条评论`);
          this.closeUploadModal();
          this.loadReviews();
        } else {
          throw new Error(data.message || '上传失败');
        }
      } catch (err) {
        this.error = '上传CSV失败：' + err.message;
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
            this.error = 'JSON文件中没有找到有效的评论';
            return;
          }

          const params = new URLSearchParams();
          if (this.manageUserId) params.append('manageUserId', this.manageUserId);
          const query = params.toString() ? '?' + params.toString() : '';
          await apiRequest(`/review/upload${query}`, {
            method: 'POST',
            body: JSON.stringify({
              productId: this.uploadForm.productId,
              contents: contents
            })
          });

          this.showMessage(`上传成功，共 ${contents.length} 条评论`);
          this.closeUploadModal();
          this.loadReviews();
        } catch (err) {
          this.error = '解析JSON失败：' + err.message;
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
        const params = new URLSearchParams();
        if (this.manageUserId) params.append('manageUserId', this.manageUserId);
        const query = params.toString() ? '?' + params.toString() : '';
        await apiRequest(`/review/update-sentiment/${this.editingReview.id}${query}`, {
          method: 'PUT',
          body: JSON.stringify({ sentimentLabel: this.editingSentiment })
        });
        this.showMessage('修正成功');
        this.closeEditModal();
        this.loadReviews();
      } catch (err) {
        this.error = '修正失败：' + err.message;
      }
    },

    async deleteReview(id) {
      if (!confirm('确定删除这条评论？')) return;

      try {
        const params = new URLSearchParams();
        if (this.manageUserId) params.append('manageUserId', this.manageUserId);
        const query = params.toString() ? '?' + params.toString() : '';
        await apiRequest(`/review/delete/${id}${query}`, { method: 'DELETE' });
        this.showMessage('删除成功');
        this.loadReviews();
      } catch (err) {
        this.error = '删除失败：' + err.message;
      }
    },

    toggleDropdown(id) {
      this.openDropdownId = this.openDropdownId === id ? null : id;
    },

    closeDropdown() {
      this.openDropdownId = null;
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
  background-color: var(--primary-color);
  color: white;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.tag-negative {
  background-color: var(--danger-color);
  color: white;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.tag-pending {
  background-color: var(--input-bg);
  color: var(--text-secondary);
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 500;
  border: 1px solid var(--border-color);
}

.review-preview {
  background-color: var(--input-bg);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-4);
  max-height: 100px;
  overflow-y: auto;
  color: var(--text-secondary);
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
  padding: var(--space-3);
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.option-btn input {
  display: none;
}

.option-btn.active.positive {
  border-color: var(--primary-color);
  background-color: rgba(76, 175, 80, 0.1);
  color: var(--primary-color);
}

.option-btn.active.negative {
  border-color: var(--danger-color);
  background-color: rgba(244, 67, 54, 0.1);
  color: var(--danger-color);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-muted);
}

.close-btn:hover {
  color: var(--text-primary);
}

.modal-body {
  max-height: 60vh;
  overflow-y: auto;
}

.upload-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.tab-btn {
  flex: 1;
  padding: var(--space-2) var(--space-4);
  background-color: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-btn.active {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.upload-section {
  margin-bottom: var(--space-4);
}

.textarea-field {
  width: 100%;
  padding: var(--space-3);
  background-color: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-family: inherit;
  resize: vertical;
  margin-bottom: var(--space-3);
}

.file-input {
  display: block;
  margin-bottom: var(--space-2);
}

.file-name {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
}

.select-field {
  min-width: 120px;
}

.filter-actions {
  display: flex;
  gap: var(--space-2);
}

.card-actions {
  display: flex;
  gap: var(--space-2);
}

.dropdown-btn.icon-only {
  padding: 6px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dropdown-btn.icon-only svg {
  width: 16px;
  height: 16px;
}
</style>