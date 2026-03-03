<template>
  <div class="app">
    <header class="header">
      <nav class="nav">
        <router-link to="/" class="nav-link">情感分析</router-link>
        <router-link to="/products" class="nav-link">商品管理</router-link>
        <router-link to="/comments" class="nav-link">评论管理</router-link>
        <router-link to="/dashboard" class="nav-link">数据看板</router-link>

      </nav>
    </header>

    <main class="main">
      <!-- Vue Router 官方推荐写法：通过 slot 拿到 Component，再配合 keep-alive 和 transition -->
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <KeepAlive include="HomeView,ProductView">
            <component :is="Component" />
          </KeepAlive>
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script>
export default {
  name: 'App'
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
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--border-color);
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav {
  display: flex;
  gap: 0;
  /* 固定导航项容器宽度，避免内容变化导致宽度波动 */
  width: 100%;
}
.nav-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: var(--font-size-base);
  padding: var(--space-2) var(--space-6);
  transition: color var(--transition-fast), border-bottom-color var(--transition-fast);
  flex: 1;
  text-align: center;
  border-bottom: 2px solid transparent;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-link:hover,
.nav-link.router-link-active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.main {
  padding: var(--space-6);
  max-width: 1200px;
  width: 100%;
  box-sizing: border-box;
  margin: 0 auto;
  min-height: calc(100vh - 88px);
}
</style>