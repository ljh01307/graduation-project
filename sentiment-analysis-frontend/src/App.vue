<template>
  <div class="app">
    <header class="header" v-if="showHeader">
      <nav class="nav">
        <div class="nav-left">
          <router-link to="/" class="nav-brand">
            <IconLogo class="nav-logo" />
            <span class="nav-brand-title">SentimentAI</span>
          </router-link>
        </div>
        <div class="nav-center">
          <router-link to="/" class="nav-link"><IconHome class="nav-icon" />情感分析</router-link>
          <router-link v-if="isAdmin" to="/users" class="nav-link"><IconUsers class="nav-icon" />用户管理</router-link>
          <router-link to="/products" class="nav-link"><IconPackage class="nav-icon" />商品管理</router-link>
          <router-link to="/comments" class="nav-link"><IconMessage class="nav-icon" />评论管理</router-link>
          <router-link to="/dashboard" class="nav-link"><IconChart class="nav-icon" />数据看板</router-link>
        </div>
        <div class="nav-right">
          <button class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换到浅色主题' : '切换到深色主题'">
            <svg v-if="isDark" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"></circle>
              <line x1="12" y1="1" x2="12" y2="3"></line>
              <line x1="12" y1="21" x2="12" y2="23"></line>
              <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
              <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
              <line x1="1" y1="12" x2="3" y2="12"></line>
              <line x1="21" y1="12" x2="23" y2="12"></line>
              <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
              <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
            </svg>
          </button>
          <div class="user-dropdown" @click.stop @mouseenter="showDropdownWithDelay" @mouseleave="hideDropdownWithDelay">
            <button class="dropdown-toggle" type="button">
              <div class="user-avatar" :class="{ admin: isAdmin }">
                <IconCrown v-if="isAdmin" />
                <IconUser v-else />
              </div>
              <span class="username">{{ username }}</span>
              <span class="dropdown-arrow" :class="{ open: showDropdown }">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </span>
            </button>
            <div class="dropdown-menu" :style="{ display: showDropdown ? 'block' : 'none' }" @mouseenter="showDropdownWithDelay" @mouseleave="hideDropdownWithDelay">
              <button class="dropdown-item" type="button" @click="openUsernameModal"><IconUser /> 修改用户名</button>
              <button class="dropdown-item" type="button" @click="openPasswordModal"><IconKey /> 修改密码</button>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item danger" type="button" @click="logout"><IconLogout /> 退出</button>
            </div>
          </div>
        </div>
      </nav>
    </header>

    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <KeepAlive include="HomeView,ProductView">
            <component :is="Component" :manageUserId="manageUserId" />
          </KeepAlive>
        </transition>
      </router-view>
    </main>

    <div class="modal-overlay" v-if="showUsernameModal || showPasswordModal" @click.self="closeModals">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">{{ showUsernameModal ? '修改用户名' : '修改密码' }}</h3>
          <button class="modal-close" @click="closeModals">&times;</button>
        </div>
        <div class="modal-body">
          <div class="alert alert-error" v-if="modalError">{{ modalError }}</div>
          <div class="alert alert-success" v-if="modalSuccess">{{ modalSuccess }}</div>
          <div v-if="showUsernameModal">
            <div class="form-group">
              <label class="form-label">当前用户名</label>
              <input type="text" class="input-field" :value="username" disabled />
            </div>
            <div class="form-group">
              <label class="form-label">新用户名</label>
              <input type="text" class="input-field" v-model="newUsername" placeholder="请输入新用户名" @keyup.enter="handleUpdateUsername" />
            </div>
          </div>
          <div v-if="showPasswordModal">
            <div class="form-group">
              <label class="form-label">原密码</label>
              <input type="password" class="input-field" v-model="oldPassword" placeholder="请输入原密码" />
            </div>
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input type="password" class="input-field" v-model="newPassword" placeholder="请输入新密码（至少6位）" />
            </div>
            <div class="form-group">
              <label class="form-label">确认新密码</label>
              <input type="password" class="input-field" v-model="confirmPassword" placeholder="请再次输入新密码" @keyup.enter="handleUpdatePassword" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModals">取消</button>
          <button class="btn btn-primary" @click="showUsernameModal ? handleUpdateUsername() : handleUpdatePassword()">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getUserInfo, clearAuth, setUserInfo } from './utils/auth.js';
import { updateUsername, updatePassword } from './utils/api.js';
import { initTheme, toggleTheme as toggleThemeUtil, getTheme } from './utils/theme.js';
import IconLogo from './components/icons/IconLogo.vue';
import IconHome from './components/icons/IconHome.vue';
import IconUsers from './components/icons/IconUsers.vue';
import IconPackage from './components/icons/IconPackage.vue';
import IconMessage from './components/icons/IconMessage.vue';
import IconChart from './components/icons/IconChart.vue';
import IconUser from './components/icons/IconUser.vue';
import IconKey from './components/icons/IconKey.vue';
import IconLogout from './components/icons/IconLogout.vue';
import IconCrown from './components/icons/IconCrown.vue';

export default {
  name: 'App',
  components: {
    IconLogo,
    IconHome,
    IconUsers,
    IconPackage,
    IconMessage,
    IconChart,
    IconUser,
    IconKey,
    IconLogout,
    IconCrown
  },
  data() {
    return {
      username: '',
      isAdmin: false,
      manageUserId: null,
      showDropdown: false,
      dropdownHideTimer: null,
      showUsernameModal: false,
      showPasswordModal: false,
      newUsername: '',
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      modalError: '',
      modalSuccess: '',
      isDark: true
    };
  },
  computed: {
    showHeader() {
      const routeName = this.$route.name;
      return routeName !== 'login' && routeName !== 'register';
    }
  },
  watch: {
    '$route.query.manageUserId': {
      handler(newVal) {
        if (newVal) {
          this.manageUserId = parseInt(newVal);
        }
      },
      immediate: true
    }
  },
  mounted() {
    this.loadUserInfo();
    initTheme();
    this.isDark = getTheme() === 'dark';
    document.addEventListener('click', this.handleClickOutside);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside);
  },
  methods: {
    loadUserInfo() {
      const user = getUserInfo();
      if (user) {
        this.username = user.username;
        this.isAdmin = user.role === 'ADMIN';
      }
    },
    onManageUserChange(userId) {
    },
    showDropdownWithDelay() {
      if (this.dropdownHideTimer) {
        clearTimeout(this.dropdownHideTimer);
        this.dropdownHideTimer = null;
      }
      this.showDropdown = true;
    },
    hideDropdownWithDelay() {
      this.dropdownHideTimer = setTimeout(() => {
        this.showDropdown = false;
      }, 150);
    },
    toggleDropdown(event) {
      event.stopPropagation();
      this.showDropdown = !this.showDropdown;
    },
    handleClickOutside(event) {
      if (!event.target.closest('.user-dropdown')) {
        if (this.dropdownHideTimer) {
          clearTimeout(this.dropdownHideTimer);
        }
        this.showDropdown = false;
      }
    },
    openUsernameModal() {
      this.showDropdown = false;
      this.newUsername = this.username;
      this.modalError = '';
      this.modalSuccess = '';
      this.showUsernameModal = true;
    },
    openPasswordModal() {
      this.showDropdown = false;
      this.oldPassword = '';
      this.newPassword = '';
      this.confirmPassword = '';
      this.modalError = '';
      this.modalSuccess = '';
      this.showPasswordModal = true;
    },
    closeModals() {
      this.showUsernameModal = false;
      this.showPasswordModal = false;
    },
    async handleUpdateUsername() {
      if (!this.newUsername || !this.newUsername.trim()) {
        this.modalError = '请输入用户名';
        return;
      }
      if (this.newUsername === this.username) {
        this.modalError = '新用户名与当前相同';
        return;
      }
      try {
        await updateUsername(this.newUsername.trim());
        const user = getUserInfo();
        user.username = this.newUsername.trim();
        setUserInfo(user);
        this.username = this.newUsername.trim();
        this.modalSuccess = '用户名修改成功';
        setTimeout(() => this.closeModals(), 1500);
      } catch (error) {
        this.modalError = error.message || '修改失败';
      }
    },
    async handleUpdatePassword() {
      if (!this.oldPassword) {
        this.modalError = '请输入原密码';
        return;
      }
      if (!this.newPassword) {
        this.modalError = '请输入新密码';
        return;
      }
      if (this.newPassword.length < 6) {
        this.modalError = '新密码至少6位';
        return;
      }
      if (this.newPassword !== this.confirmPassword) {
        this.modalError = '两次输入的新密码不一致';
        return;
      }
      try {
        await updatePassword(this.oldPassword, this.newPassword);
        this.modalSuccess = '密码修改成功';
        setTimeout(() => this.closeModals(), 1500);
      } catch (error) {
        this.modalError = error.message || '修改失败';
      }
    },
    logout() {
      this.showDropdown = false;
      clearAuth();
      this.$router.push('/login');
    },
    toggleTheme() {
      const newTheme = toggleThemeUtil();
      this.isDark = newTheme === 'dark';
    }
  }
}
</script>

<style>
/* ========== 全局设计变量========== */

 :root {
  /* 主色调 */
  --primary-color: #4CAF50;
  --primary-hover: #45a049;

  /* 危险/错误色 */
  --danger-color: #f44336;
  --danger-hover: #d32f2f;

  /* 背景色 */
  --bg-color: #121212;
  --card-bg: #1e1e1e;
  --input-bg: #2d2d2d;

  /* 文字颜色 */
  --text-primary: #ffffff;
  --text-secondary: #b0b0b0;
  --text-muted: #777777;

  /* 边框与分割线 */
  --border-color: #333333;
  --divider-color: #333333;

  /* 阴影 */
  --card-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);

  /* 字体 */
  --font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-size-xs: 12px;
  --font-size-sm: 14px;
  --font-size-base: 16px;
  --font-size-md: 18px;
  --font-size-lg: 20px;
  --font-size-xl: 24px;
  --font-size-2xl: 28px;
  --font-size-3xl: 32px;
  --line-height-tight: 1.35;
  --line-height-normal: 1.5;
  --line-height-relaxed: 1.6;

  /* 间距（4 的倍数，统一节奏） */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;

  /* 动效 */
  --transition-fast: 0.15s ease;
  --transition-normal: 0.25s ease;
}

/* ========== 浅色主题变量 ========== */
[data-theme="light"] {
  --bg-color: #f5f7fa;
  --card-bg: #ffffff;
  --input-bg: #f0f2f5;
  --text-primary: #1a1a2e;
  --text-secondary: #4a5568;
  --text-muted: #a0aec0;
  --border-color: #e2e8f0;
  --divider-color: #e2e8f0;
  --card-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* ========== 主题过渡动画 ========== */
body,
body *,
body *::before,
body *::after {
  transition: background-color 0.5s ease,
              color 0.5s ease,
              border-color 0.5s ease,
              box-shadow 0.5s ease;
}

/* 排除不需要过渡的元素，避免性能问题和交互延迟 */
.modal-overlay,
.dropdown-menu,
.spinner,
.spinner::before,
.spinner::after {
  transition: none !important;
}

/* ========== 全局样式 ========== */
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  padding: 0;
  background-color: var(--bg-color);
  color: var(--text-primary);
  font-family: var(--font-family);
  font-size: var(--font-size-base);
  line-height: var(--line-height-normal);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-y: scroll;
}
/* 页面切换淡入淡出动画 */
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-fade-enter-active {
  transition: all var(--transition-normal);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
.page-fade-leave-active {
  transition: all var(--transition-fast);
}


.app {
  min-height: 100vh;
}

.header {
  background-color: var(--card-bg);
  padding: var(--space-3) var(--space-6);
  border-bottom: 1px solid var(--border-color);
  max-width: 1200px;
  width: calc(100% - var(--space-8));
  margin: var(--space-4) auto 0;
  position: sticky;
  top: var(--space-4);
  z-index: 100;
  overflow: visible;
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0;
  width: 100%;
}

.nav-left {
  display: flex;
  align-items: center;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  color: var(--text-primary);
  transition: opacity var(--transition-fast);
}

.nav-brand:hover {
  opacity: 0.8;
}

.nav-logo {
  color: var(--primary-color);
  width: 28px;
  height: 28px;
}

.nav-brand-title {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.nav-center {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background-color: var(--input-bg);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.theme-toggle:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
  transform: rotate(15deg);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.role-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: var(--font-size-xs);
  background-color: var(--input-bg);
  color: var(--text-muted);
}

.role-tag.admin {
  background-color: var(--primary-color);
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: var(--font-size-sm);
}

.nav-link {
  color: var(--text-secondary);
  text-decoration: none;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.nav-icon {
  width: 18px;
  height: 18px;
}

.nav-link:hover {
  color: var(--text-primary);
  background-color: var(--input-bg);
}

.nav-link.router-link-active {
  color: var(--primary-color);
  background-color: rgba(76, 175, 80, 0.1);
}

.main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-6);
}

/* ========== 通用组件样式 ========== */

/* 卡片 */
.card {
  background-color: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--card-shadow);
}

.card-title {
  font-size: var(--font-size-lg);
  margin-bottom: var(--space-4);
  color: var(--text-primary);
}

/* 按钮 */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-base);
  font-weight: 500;
  transition: all var(--transition-fast);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
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

.btn-danger {
  background-color: var(--danger-color);
  color: white;
}

.btn-danger:hover {
  background-color: var(--danger-hover);
}

/* 输入框 */
.input-field {
  padding: 10px 14px;
  background-color: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: var(--font-size-base);
  transition: border-color var(--transition-fast);
  width: 100%;
}

.input-field:focus {
  outline: none;
  border-color: var(--primary-color);
}

.input-field::placeholder {
  color: var(--text-muted);
}

/* 选择框 */
.select-field {
  padding: 10px 14px;
  background-color: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: var(--font-size-base);
  cursor: pointer;
  width: 100%;
}

.select-field:focus {
  outline: none;
  border-color: var(--primary-color);
}

/* 页面标题 */
.page-title {
  font-size: var(--font-size-2xl);
  margin-bottom: var(--space-6);
  color: var(--text-primary);
}

/* 警告提示 */
.alert {
  padding: var(--space-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}

.alert-error {
  background-color: rgba(244, 67, 54, 0.1);
  color: var(--danger-color);
  border: 1px solid var(--danger-color);
}

.alert-success {
  background-color: rgba(76, 175, 80, 0.1);
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
}

/* 标签 */
.tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.tag-positive {
  background-color: rgba(76, 175, 80, 0.2);
  color: #4CAF50;
}

.tag-negative {
  background-color: rgba(244, 67, 54, 0.2);
  color: #f44336;
}

.tag-neutral {
  background-color: rgba(158, 158, 158, 0.2);
  color: #9e9e9e;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--bg-color);
}

::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}

/* 用户下拉框 */
.user-dropdown {
  position: relative;
  z-index: 1000;
}

.dropdown-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 16px 6px 6px;
  background-color: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: 50px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.dropdown-toggle:hover {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
  transform: translateY(-1px);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0e0e0 0%, #bdbdbd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.3s ease;
}

.user-avatar.admin {
  background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.user-avatar svg {
  width: 20px;
  height: 20px;
}

.dropdown-arrow {
  display: flex;
  align-items: center;
  color: var(--text-muted);
  transition: all 0.3s ease;
}

.dropdown-arrow svg {
  transition: transform 0.3s ease;
}

.dropdown-arrow.open svg {
  transform: rotate(180deg);
}

.user-dropdown:hover .dropdown-arrow {
  color: var(--primary-color);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  min-width: 160px;
  background-color: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--card-shadow);
  z-index: 1000;
  overflow: visible;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.dropdown-item:hover {
  background-color: var(--input-bg);
}

.dropdown-item.danger {
  color: var(--danger-color);
}

.dropdown-item.danger:hover {
  background-color: rgba(244, 67, 54, 0.1);
}

.dropdown-divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 4px 0;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background-color: var(--card-bg);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 420px;
  box-shadow: var(--card-shadow);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--border-color);
}

.modal-title {
  margin: 0;
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  color: var(--text-primary);
}

.modal-body {
  padding: var(--space-6);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--border-color);
}

.form-group {
  margin-bottom: var(--space-4);
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
</style>
