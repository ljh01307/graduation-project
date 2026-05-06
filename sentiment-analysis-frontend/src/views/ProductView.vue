<template>
  <div class="product-management">
    <h2 class="page-title">商品管理</h2>

    <div class="card">
      <h3 class="card-title">筛选条件</h3>
      <div class="filter-row">
        <div class="filter-item">
          <label>选择商品</label>
          <select v-model="filters.productId" @change="applyFilters" class="select-field">
            <option value="">全部商品</option>
            <option v-for="p in products" :key="p.id" :value="p.id">
              {{ p.category }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间</label>
          <input v-model="filters.startDate" type="date" @change="applyFilters" class="input-field" />
        </div>
        <div class="filter-item">
          <label>结束时间</label>
          <input v-model="filters.endDate" type="date" @change="applyFilters" class="input-field" />
        </div>
        <div class="filter-item">
          <label>排序</label>
          <select v-model="filters.sortBy" @change="applyFilters" class="select-field">
            <option value="createTime">创建时间</option>
            <option value="reviewCount">评论数</option>
            <option value="positiveRate">好评率</option>
          </select>
        </div>
        <div class="filter-item">
          <label>顺序</label>
          <select v-model="filters.sortDir" @change="applyFilters" class="select-field">
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
        <h3 class="card-title">商品列表（{{ filteredProducts.length }} 项）</h3>
        <div class="card-actions">
          <button @click="showAddModal = true" class="btn btn-outline btn-sm"><IconPlus /> 添加商品</button>
        </div>
      </div>

      <div v-if="loading" class="center-text">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="filteredProducts.length === 0" class="empty-state">
        <p>暂无商品，请先添加一个商品类别。</p>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>商品名称</th>
            <th>创建时间</th>
            <th>评论数</th>
            <th>好评率</th>
            <th class="col-actions">编辑</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in filteredProducts" :key="product.id">
            <td>{{ product.id }}</td>
            <td><strong>{{ product.category }}</strong></td>
            <td>{{ formatDate(product.createTime) }}</td>
            <td>{{ product.reviewCount || 0 }}</td>
            <td>
              <span v-if="product.analyzedCount > 0" :class="['rate-tag', getRateClass(product.positiveRate)]">
                {{ product.positiveRate ? product.positiveRate.toFixed(1) : 0 }}%
              </span>
              <span v-else class="rate-tag rate-tag-pending">-</span>
            </td>
            <td class="actions-cell">
              <div class="dropdown" :class="{ 'dropdown-open': openDropdownId === product.id }">
                <button @click="toggleDropdown(product.id)" class="btn btn-outline btn-sm dropdown-btn icon-only" title=":D">
                  <IconEdit />
                </button>
                <div class="dropdown-menu">
                  <button @click="openEditModal(product); closeDropdown()" class="dropdown-item">
                    <IconEdit /> 编辑
                  </button>
                  <button @click="goToComments(product.id); closeDropdown()" class="dropdown-item">
                    <IconMessage /> 评论管理
                  </button>
                  <button @click="goToDashboard(product.id); closeDropdown()" class="dropdown-item">
                    <IconChart /> 数据看板
                  </button>
                  <div class="dropdown-divider"></div>
                  <button @click="deleteProduct(product.id, product.category); closeDropdown()" class="dropdown-item danger">
                    <IconTrash /> 删除
                  </button>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <div v-if="message" :class="['alert', messageType === 'error' ? 'alert-error' : 'alert-success']">
      {{ message }}
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">添加商品</h3>
          <button @click="closeAddModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <input
            v-model="newCategory"
            type="text"
            class="input-field"
            placeholder="请输入商品类别"
            @keypress.enter="addProduct"
          />
        </div>
        <div class="modal-actions">
          <button @click="closeAddModal" class="btn btn-secondary">取消</button>
          <button @click="addProduct" class="btn btn-primary">添加</button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">编辑商品名称</h3>
          <button @click="closeEditModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <input
            v-model="editCategory"
            type="text"
            class="input-field"
            placeholder="请输入新的商品名称"
            @keypress.enter="saveEdit"
          />
        </div>
        <div class="modal-actions">
          <button @click="closeEditModal" class="btn btn-secondary">取消</button>
          <button @click="saveEdit" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { fetchProducts, addProduct, updateProduct, deleteProduct } from '../utils/api.js';
import IconRefresh from '../components/icons/IconRefresh.vue';
import IconPlus from '../components/icons/IconPlus.vue';
import IconEdit from '../components/icons/IconEdit.vue';
import IconTrash from '../components/icons/IconTrash.vue';
import IconMessage from '../components/icons/IconMessage.vue';
import IconChart from '../components/icons/IconChart.vue';

export default {
  name: 'ProductView',
  components: {
    IconRefresh,
    IconPlus,
    IconEdit,
    IconTrash,
    IconMessage,
    IconChart
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
      newCategory: '',
      loading: false,
      error: null,
      message: '',
      messageType: 'success',
      showAddModal: false,
      showEditModal: false,
      editingProductId: null,
      editCategory: '',
      openDropdownId: null,
      filters: {
        productId: '',
        startDate: '',
        endDate: '',
        sortBy: 'createTime',
        sortDir: 'DESC'
      }
    };
  },
  computed: {
    filteredProducts() {
      let result = [...this.products];

      if (this.filters.productId) {
        result = result.filter(p => p.id === this.filters.productId);
      }

      if (this.filters.startDate) {
        const startDate = new Date(this.filters.startDate);
        result = result.filter(p => new Date(p.createTime) >= startDate);
      }

      if (this.filters.endDate) {
        const endDate = new Date(this.filters.endDate);
        endDate.setHours(23, 59, 59, 999);
        result = result.filter(p => new Date(p.createTime) <= endDate);
      }

      result.sort((a, b) => {
        let aVal, bVal;
        switch (this.filters.sortBy) {
          case 'reviewCount':
            aVal = a.reviewCount || 0;
            bVal = b.reviewCount || 0;
            break;
          case 'positiveRate':
            aVal = a.positiveRate || 0;
            bVal = b.positiveRate || 0;
            break;
          default:
            aVal = new Date(a.createTime);
            bVal = new Date(b.createTime);
        }
        if (this.filters.sortDir === 'DESC') {
          return bVal > aVal ? 1 : -1;
        }
        return aVal > bVal ? 1 : -1;
      });

      return result;
    }
  },
  watch: {
    manageUserId: {
      handler() {
        this.loadProducts();
      }
    }
  },
  async mounted() {
    await this.loadProducts();
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

    getRateClass(rate) {
      if (rate >= 80) return 'rate-tag-high';
      if (rate >= 50) return 'rate-tag-medium';
      return 'rate-tag-low';
    },

    async loadProducts() {
      this.loading = true;
      this.error = null;
      try {
        const data = await fetchProducts(this.manageUserId);
        this.products = data.products || [];
      } catch (err) {
        this.error = '加载商品列表失败：' + err.message;
        console.error(err);
      } finally {
        this.loading = false;
      }
    },

    async addProduct() {
      const category = this.newCategory.trim();
      if (!category) {
        this.error = '请输入商品类别！';
        return;
      }

      this.error = null;
      try {
        await addProduct(category, this.manageUserId);
        this.newCategory = '';
        this.showMessage('商品添加成功');
        await this.loadProducts();
        this.closeAddModal();
      } catch (err) {
        this.error = '添加商品失败：' + err.message;
        console.error(err);
      }
    },

    closeAddModal() {
      this.showAddModal = false;
      this.newCategory = '';
    },

    applyFilters() {
    },

    resetFilters() {
      this.filters = {
        productId: '',
        startDate: '',
        endDate: '',
        sortBy: 'createTime',
        sortDir: 'DESC'
      };
    },

    openEditModal(product) {
      this.editingProductId = product.id;
      this.editCategory = product.category;
      this.showEditModal = true;
    },

    closeEditModal() {
      this.showEditModal = false;
      this.editingProductId = null;
      this.editCategory = '';
    },

    async saveEdit() {
      const category = this.editCategory.trim();
      if (!category) {
        this.error = '请输入商品名称！';
        return;
      }

      try {
        await updateProduct(this.editingProductId, category, this.manageUserId);
        this.showMessage('商品更新成功');
        this.closeEditModal();
        await this.loadProducts();
      } catch (err) {
        this.error = '更新商品失败：' + err.message;
        console.error(err);
      }
    },

    goToDashboard(productId) {
      this.$router.push({ name: 'dashboard', query: { productId } });
    },

    goToComments(productId) {
      this.$router.push({ name: 'comments', query: { productId } });
    },

    toggleDropdown(id) {
      this.openDropdownId = this.openDropdownId === id ? null : id;
    },

    closeDropdown() {
      this.openDropdownId = null;
    },

    async deleteProduct(id, name) {
      if (!confirm(`确定删除商品「${name}」吗？该商品下的所有评论将一并删除！`)) return;

      this.error = null;
      try {
        await deleteProduct(id, this.manageUserId);
        this.showMessage('商品删除成功');
        await this.loadProducts();
      } catch (err) {
        this.error = '删除商品失败：' + err.message;
        console.error(err);
      }
    }
  }
};
</script>

<style scoped>

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
  margin-bottom: var(--space-4);
}

.product-management {
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  margin: 0 0 var(--space-6) 0;
  font-size: var(--font-size-xl);
  color: var(--primary-color);
}


.card-actions {
  display: flex;
  gap: var(--space-2);
}

.filter-actions {
  display: flex;
  gap: var(--space-2);
}

.select-field {
  min-width: 120px;
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