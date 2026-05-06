<template>
  <div class="user-management">
    <h2 class="page-title">用户管理</h2>

    <div class="card">
      <h3 class="card-title">筛选条件</h3>
      <div class="filter-row">
        <div class="filter-item">
          <label>用户角色</label>
          <select v-model="filters.role" @change="applyFilters" class="select-field">
            <option value="">全部</option>
            <option value="ADMIN">管理员</option>
            <option value="USER">普通用户</option>
          </select>
        </div>
        <div class="filter-item">
          <label>搜索用户</label>
          <div class="input-wrapper">
            <span class="input-icon"><IconSearch /></span>
            <input v-model="filters.keyword" type="text" placeholder="搜索用户名..." @input="applyFilters" class="input-field with-icon" />
          </div>
        </div>
        <div class="filter-item">
          <label>排序</label>
          <select v-model="filters.sortBy" @change="applyFilters" class="select-field">
            <option value="createTime">注册时间</option>
            <option value="username">用户名</option>
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
        <h3 class="card-title">用户列表（{{ filteredUsers.length }} 项）</h3>
        <div class="card-actions">
          <button @click="showAddModal = true" class="btn btn-outline btn-sm"><IconPlus /> 添加用户</button>
        </div>
      </div>

      <div v-if="loading" class="center-text">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="filteredUsers.length === 0" class="empty-state">
        <p>暂无用户数据</p>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>注册时间</th>
            <th>商品数</th>
            <th class="col-actions">编辑</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td><strong>{{ user.username }}</strong></td>
            <td>
              <span :class="['tag', user.role === 'ADMIN' ? 'tag-admin' : 'tag-user']">
                {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td>{{ formatDate(user.createTime) }}</td>
            <td>{{ user.productCount || 0 }}</td>
            <td class="actions-cell">
              <div class="dropdown" :class="{ 'dropdown-open': openDropdownId === user.id }">
                <button @click="toggleDropdown(user.id)" class="btn btn-outline btn-sm dropdown-btn icon-only" title=":p">
                  <IconEdit />
                </button>
                <div class="dropdown-menu">
                  <button @click="openEditModal(user); closeDropdown()" class="dropdown-item">
                    <IconEdit /> 编辑
                  </button>
                  <button @click="goToProducts(user.id); closeDropdown()" class="dropdown-item">
                    <IconPackage /> 商品管理
                  </button>
                  <button @click="goToComments(user.id); closeDropdown()" class="dropdown-item">
                    <IconMessage /> 评论管理
                  </button>
                  <button @click="goToDashboard(user.id); closeDropdown()" class="dropdown-item">
                    <IconChart /> 数据看板
                  </button>
                  <div class="dropdown-divider"></div>
                  <button @click="deleteUser(user.id, user.username); closeDropdown()" class="dropdown-item danger">
                    <IconTrash /> 删除
                  </button>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="filteredUsers.length > pagination.size" class="pagination">
        <button
          @click="prevPage"
          :disabled="pagination.currentPage === 0"
          class="btn btn-outline btn-sm"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }} 页
        </span>
        <button
          @click="nextPage"
          :disabled="pagination.currentPage >= pagination.totalPages - 1"
          class="btn btn-outline btn-sm"
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

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">添加用户</h3>
          <button @click="closeAddModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
              v-model="addForm.username"
              type="text"
              class="input-field"
              placeholder="请输入用户名"
            />
          </div>
          <div class="form-group">
            <label class="form-label">密码</label>
            <input
              v-model="addForm.password"
              type="password"
              class="input-field"
              placeholder="请输入密码（至少6位）"
            />
          </div>
          <div class="form-group">
            <label class="form-label">角色</label>
            <select v-model="addForm.role" class="select-field">
              <option value="USER">普通用户</option>
              <option value="ADMIN">管理员</option>
            </select>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="closeAddModal" class="btn btn-secondary">取消</button>
          <button @click="addUser" class="btn btn-primary">添加</button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">编辑用户</h3>
          <button @click="closeEditModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
              v-model="editUsername"
              type="text"
              class="input-field"
              placeholder="请输入新的用户名"
              @keypress.enter="saveEdit"
            />
          </div>
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
import { fetchUsers, addUser as apiAddUser, updateUser, deleteUser as apiDeleteUser } from '../utils/api.js';
import IconSearch from '../components/icons/IconSearch.vue';
import IconRefresh from '../components/icons/IconRefresh.vue';
import IconPlus from '../components/icons/IconPlus.vue';
import IconEdit from '../components/icons/IconEdit.vue';
import IconTrash from '../components/icons/IconTrash.vue';
import IconPackage from '../components/icons/IconPackage.vue';
import IconMessage from '../components/icons/IconMessage.vue';
import IconChart from '../components/icons/IconChart.vue';

export default {
  name: 'UserView',
  components: {
    IconSearch,
    IconRefresh,
    IconPlus,
    IconEdit,
    IconTrash,
    IconPackage,
    IconMessage,
    IconChart
  },
  data() {
    return {
      users: [],
      loading: false,
      error: null,
      message: '',
      messageType: 'success',
      showAddModal: false,
      showEditModal: false,
      editingUserId: null,
      editUsername: '',
      addForm: {
        username: '',
        password: '',
        role: 'USER'
      },
      openDropdownId: null,
      filters: {
        role: '',
        keyword: '',
        sortBy: 'createTime',
        sortDir: 'DESC'
      },
      pagination: {
        currentPage: 0,
        size: 20,
        totalPages: 0
      }
    };
  },
  computed: {
    filteredUsers() {
      let result = [...this.users];

      if (this.filters.role) {
        result = result.filter(u => u.role === this.filters.role);
      }

      if (this.filters.keyword) {
        const keyword = this.filters.keyword.toLowerCase();
        result = result.filter(u =>
          u.username.toLowerCase().includes(keyword)
        );
      }

      result.sort((a, b) => {
        let aVal, bVal;
        if (this.filters.sortBy === 'username') {
          aVal = a.username.toLowerCase();
          bVal = b.username.toLowerCase();
        } else {
          aVal = new Date(a.createTime);
          bVal = new Date(b.createTime);
        }
        if (this.filters.sortDir === 'DESC') {
          return bVal > aVal ? 1 : -1;
        }
        return aVal > bVal ? 1 : -1;
      });

      return result;
    },
    paginatedUsers() {
      const start = this.pagination.currentPage * this.pagination.size;
      const end = start + this.pagination.size;
      this.pagination.totalPages = Math.ceil(this.filteredUsers.length / this.pagination.size);
      return this.filteredUsers.slice(start, end);
    }
  },
  async mounted() {
    await this.loadUsers();
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

    async loadUsers() {
      this.loading = true;
      this.error = null;
      try {
        const data = await fetchUsers();
        this.users = data || [];
      } catch (err) {
        this.error = '加载用户列表失败：' + err.message;
        console.error(err);
      } finally {
        this.loading = false;
      }
    },

    async addUser() {
      if (!this.addForm.username.trim()) {
        this.error = '请输入用户名';
        return;
      }
      if (!this.addForm.password || this.addForm.password.length < 6) {
        this.error = '密码至少6位';
        return;
      }

      this.error = null;
      try {
        await apiAddUser(this.addForm.username.trim(), this.addForm.password, this.addForm.role);
        this.showMessage('用户添加成功');
        await this.loadUsers();
        this.closeAddModal();
      } catch (err) {
        this.error = '添加用户失败：' + err.message;
        console.error(err);
      }
    },

    closeAddModal() {
      this.showAddModal = false;
      this.addForm = { username: '', password: '', role: 'USER' };
    },

    openEditModal(user) {
      this.editingUserId = user.id;
      this.editUsername = user.username;
      this.showEditModal = true;
    },

    async saveEdit() {
      if (!this.editUsername.trim()) {
        this.error = '用户名不能为空';
        return;
      }

      this.error = null;
      try {
        await updateUser(this.editingUserId, this.editUsername.trim());
        this.showMessage('用户更新成功');
        await this.loadUsers();
        this.closeEditModal();
      } catch (err) {
        this.error = '更新用户失败：' + err.message;
        console.error(err);
      }
    },

    closeEditModal() {
      this.showEditModal = false;
      this.editingUserId = null;
      this.editUsername = '';
    },

    async deleteUser(id, username) {
      if (!confirm(`确定要删除用户 "${username}" 吗？此操作不可恢复。`)) {
        return;
      }

      this.error = null;
      try {
        await apiDeleteUser(id);
        this.showMessage('删除用户成功');
        await this.loadUsers();
      } catch (err) {
        this.error = '删除用户失败：' + err.message;
        console.error(err);
      }
    },

    toggleDropdown(id) {
      this.openDropdownId = this.openDropdownId === id ? null : id;
    },

    closeDropdown() {
      this.openDropdownId = null;
    },

    applyFilters() {
      this.pagination.currentPage = 0;
    },

    resetFilters() {
      this.filters = {
        role: '',
        keyword: '',
        sortBy: 'createTime',
        sortDir: 'DESC'
      };
      this.pagination.currentPage = 0;
    },

    prevPage() {
      if (this.pagination.currentPage > 0) {
        this.pagination.currentPage--;
      }
    },

    nextPage() {
      if (this.pagination.currentPage < this.pagination.totalPages - 1) {
        this.pagination.currentPage++;
      }
    },

    goToProducts(userId) {
      this.$router.push({ name: 'products', query: { manageUserId: userId } });
    },

    goToComments(userId) {
      this.$router.push({ name: 'comments', query: { manageUserId: userId } });
    },

    goToDashboard(userId) {
      this.$router.push({ name: 'dashboard', query: { manageUserId: userId } });
    }
  }
};
</script>

<style scoped>
.user-management {
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

.tag-admin {
  background-color: var(--primary-color);
  color: white;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.tag-user {
  background-color: var(--input-bg);
  color: var(--text-secondary);
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 500;
  border: 1px solid var(--border-color);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  display: flex;
  align-items: center;
  color: var(--text-muted);
  z-index: 1;
}

.input-field.with-icon {
  padding-left: 38px;
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
</style>
