<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-brand">
        <IconLogo class="brand-logo" />
        <h1 class="brand-title">SentimentAI</h1>
      </div>
      <div class="auth-header">
        <h2 class="auth-title">用户注册</h2>
        <p class="auth-subtitle">创建新账号，开始使用</p>
      </div>
      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <div class="input-wrapper">
            <span class="input-icon"><IconUser /></span>
            <input
              id="username"
              v-model="form.username"
              type="text"
              class="input-field with-icon"
              placeholder="请输入用户名"
              required
            />
          </div>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <span class="input-icon"><IconLock /></span>
            <input
              id="password"
              v-model="form.password"
              type="password"
              class="input-field with-icon"
              placeholder="请输入密码（至少6位）"
              required
            />
          </div>
        </div>
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <div class="input-wrapper">
            <span class="input-icon"><IconLockCheck /></span>
            <input
              id="confirmPassword"
              v-model="form.confirmPassword"
              type="password"
              class="input-field with-icon"
              placeholder="请再次输入密码"
              required
            />
          </div>
        </div>
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <div class="auth-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="link">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { setToken, setUserInfo } from '../utils/auth.js'
import IconUser from '../components/icons/IconUser.vue'
import IconLock from '../components/icons/IconLock.vue'
import IconLockCheck from '../components/icons/IconLockCheck.vue'
import IconLogo from '../components/icons/IconLogo.vue'

export default {
  name: 'RegisterView',
  components: {
    IconUser,
    IconLock,
    IconLockCheck,
    IconLogo
  },
  data() {
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: ''
      },
      loading: false,
      errorMessage: '',
      successMessage: ''
    }
  },
  methods: {
    async handleRegister() {
      this.errorMessage = ''
      this.successMessage = ''

      if (this.form.password !== this.form.confirmPassword) {
        this.errorMessage = '两次输入的密码不一致'
        return
      }

      if (this.form.password.length < 6) {
        this.errorMessage = '密码长度至少为6位'
        return
      }

      this.loading = true

      try {
        const response = await fetch('/api/auth/register', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            username: this.form.username,
            password: this.form.password
          })
        })

        const result = await response.json()

        if (result.code === 200) {
          this.successMessage = '注册成功，即将跳转...'
          setToken(result.data.token, false)
          setUserInfo({
            username: result.data.username,
            role: result.data.role
          })
          setTimeout(() => {
            window.location.href = '/'
          }, 1500)
        } else {
          this.errorMessage = result.message || '注册失败'
        }
      } catch (error) {
        this.errorMessage = '网络错误，请稍后重试'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: var(--space-6);
  background: var(--bg-color);
}

.auth-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-8);
  width: 100%;
  max-width: 420px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
}

.auth-brand {
  text-align: center;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--border-color);
}

.brand-logo {
  color: var(--primary-color);
  margin-bottom: var(--space-3);
}

.brand-title {
  color: var(--text-primary);
  font-size: var(--font-size-xl);
  font-weight: 700;
  margin: 0 0 var(--space-1) 0;
  letter-spacing: 1px;
}

.brand-tagline {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  margin: 0;
}

.auth-header {
  text-align: center;
  margin-bottom: var(--space-6);
}

.auth-title {
  color: var(--primary-color);
  font-size: var(--font-size-xl);
  margin: 0 0 var(--space-2) 0;
  font-weight: 600;
}

.auth-subtitle {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-group label {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  display: flex;
  align-items: center;
  color: var(--text-muted);
}

.input-field {
  padding: 12px 16px;
  background: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: var(--font-size-base);
  transition: all var(--transition-fast);
  width: 100%;
}

.input-field.with-icon {
  padding-left: 42px;
}

.input-field:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.1);
}

.input-field::placeholder {
  color: var(--text-muted);
}

.error-message {
  color: var(--danger-color);
  font-size: var(--font-size-sm);
  text-align: center;
  padding: var(--space-3);
  background: rgba(244, 67, 54, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(244, 67, 54, 0.2);
}

.success-message {
  color: var(--primary-color);
  font-size: var(--font-size-sm);
  text-align: center;
  padding: var(--space-3);
  background: rgba(76, 175, 80, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.btn-block {
  width: 100%;
  padding: 14px;
  font-size: var(--font-size-base);
  font-weight: 600;
  margin-top: var(--space-2);
}

.auth-footer {
  margin-top: var(--space-6);
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  margin-left: var(--space-1);
  transition: all var(--transition-fast);
}

.link:hover {
  color: var(--primary-hover);
  text-decoration: underline;
}

.link:hover {
  text-decoration: underline;
}
</style>
