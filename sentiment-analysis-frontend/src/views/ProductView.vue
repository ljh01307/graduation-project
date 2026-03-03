<template>
  <div class="product-management">
    <h2 class="page-title">商品管理</h2>

    <div class="card">
      <h3 class="card-title">筛选条件</h3>
      <div class="filter-row">
        <div class="filter-item">
          <label>商品名称</label>
          <input v-model="filters.keyword" type="text" placeholder="搜索商品名称..." @input="applyFilters" class="input-field" />
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
          <button @click="resetFilters" class="btn btn-secondary btn-sm">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">商品列表（{{ filteredProducts.length }} 项）</h3>
        <div class="card-actions">
          <button @click="showAddModal = true" class="btn btn-outline btn-sm">添加商品</button>
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
            <th class="col-actions">操作</th>
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
                <button @click="toggleDropdown(product.id)" class="btn btn-outline btn-sm dropdown-btn">
                  操作 ▼
                </button>
                <div class="dropdown-menu">
                  <button @click="openEditModal(product); closeDropdown()" class="dropdown-item">
                    ✏️ 编辑
                  </button>
                  <button @click="goToComments(product.id); closeDropdown()" class="dropdown-item">
                    💬 评论管理
                  </button>
                  <button @click="goToDashboard(product.id); closeDropdown()" class="dropdown-item">
                    📊 数据看板
                  </button>
                  <div class="dropdown-divider"></div>
                  <button @click="deleteProduct(product.id, product.category); closeDropdown()" class="dropdown-item dropdown-danger">
                    🗑️ 删除
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

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
      <div class="modal">
        <h3 class="modal-title">添加商品</h3>
        <input
          v-model="newCategory"
          type="text"
          class="input-field modal-input"
          placeholder="请输入商品类别"
          @keypress.enter="addProduct"
        />
        <div class="modal-actions">
          <button @click="closeAddModal" class="btn btn-secondary">取消</button>
          <button @click="addProduct" class="btn btn-primary">添加</button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <h3 class="modal-title">编辑商品名称</h3>
        <input
          v-model="editCategory"
          type="text"
          class="input-field modal-input"
          placeholder="请输入新的商品名称"
          @keypress.enter="saveEdit"
        />
        <div class="modal-actions">
          <button @click="closeEditModal" class="btn btn-secondary">取消</button>
          <button @click="saveEdit" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProductView',
  data() {
    return {
      products: [],
      newCategory: '',
      loading: false,
      error: null,
      showAddModal: false,
      showEditModal: false,
      editingProductId: null,
      editCategory: '',
      openDropdownId: null,
      filters: {
        keyword: '',
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

      if (this.filters.keyword) {
        const keyword = this.filters.keyword.toLowerCase();
        result = result.filter(p => 
          p.category.toLowerCase().includes(keyword)
        );
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
  async mounted() {
    await this.loadProducts();
  },
  methods: {
    formatDate(dateStr) {
      if (!dateStr) return '-';
      const date = new Date(dateStr);
      return date.toLocaleString('zh-CN');
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
        const res = await fetch('http://localhost:8080/api/product/list-with-stats');
        if (!res.ok) throw new Error('服务器返回错误');
        const data = await res.json();
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
        const res = await fetch('http://localhost:8080/api/product/add', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ category })
        });

        if (!res.ok) {
          const errData = await res.json().catch(() => ({}));
          throw new Error(errData.message || '添加失败');
        }

        this.newCategory = '';
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
        keyword: '',
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
        const res = await fetch(`http://localhost:8080/api/product/update/${this.editingProductId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ category })
        });

        if (!res.ok) throw new Error('更新失败');

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

    deleteProduct(id, name) {
      if (!confirm(`⚠️ 确定删除商品「${name}」吗？该商品下的所有评论将一并删除！`)) return;

      this.error = null;
      (async () => {
        try {
          const res = await fetch(`http://localhost:8080/api/product/delete/${id}`, {
            method: 'DELETE'
          });

          if (!res.ok) throw new Error('删除失败');

          await this.loadProducts();
        } catch (err) {
          this.error = '删除商品失败：' + err.message;
          console.error(err);
        }
      })();
    }
  }
};
</script>

<style scoped>
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

.card-actions {
  display: flex;
  gap: var(--space-2);
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

.select-field {
  min-width: 120px;
}

.input-field::placeholder {
  color: var(--text-muted);
}

.input-field:focus,
.select-field:focus {
  outline: none;
  border-color: var(--primary-color);
}

.add-form {
  display: flex;
  gap: var(--space-3);
}

.btn {
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--primary-hover);
}

.btn-secondary {
  background-color: var(--input-bg);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--border-color);
}

.btn-outline {
  background-color: transparent;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
}

.btn-outline:hover {
  background-color: var(--primary-color);
  color: white;
}

.btn-danger {
  background-color: var(--danger-color);
  color: white;
}

.btn-danger:hover {
  background-color: var(--danger-hover);
}

.btn-sm {
  padding: var(--space-2) var(--space-3);
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

.data-table tbody tr {
  transition: background-color var(--transition-fast);
}

.data-table tbody tr:hover {
  background-color: var(--input-bg);
}

.rate-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.rate-tag-high {
  background: rgba(76, 175, 80, 0.15);
  color: var(--primary-color);
}

.rate-tag-medium {
  background: rgba(255, 152, 0, 0.15);
  color: #ff9800;
}

.rate-tag-low {
  background: rgba(244, 67, 54, 0.15);
  color: var(--danger-color);
}

.rate-tag-pending {
  background: rgba(158, 158, 158, 0.15);
  color: #9e9e9e;
}

.actions-cell {
  position: relative;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-btn {
  min-width: 70px;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  min-width: 140px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  display: none;
  margin-top: 4px;
}

.dropdown-open .dropdown-menu {
  display: block;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 14px;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  transition: background-color var(--transition-fast);
}

.dropdown-item:hover {
  background-color: var(--input-bg);
}

.dropdown-danger {
  color: var(--danger-color);
}

.dropdown-danger:hover {
  background-color: rgba(244, 67, 54, 0.1);
}

.dropdown-divider {
  height: 1px;
  background-color: var(--divider-color);
  margin: 4px 0;
}

.center-text {
  text-align: center;
  padding: var(--space-10) 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.empty-state {
  text-align: center;
  padding: var(--space-10) 0;
  color: var(--text-muted);
  font-size: var(--font-size-base);
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

.alert-error {
  background-color: rgba(244, 67, 54, 0.12);
  color: #ef5350;
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  margin-top: var(--space-4);
  border-left: 4px solid var(--danger-color);
  font-size: var(--font-size-sm);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
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
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-title {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.modal-input {
  width: 100%;
  margin-bottom: var(--space-4);
  box-sizing: border-box;
}

.modal-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}
</style>
