import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProductView from '../views/ProductView.vue'
import CommentView from '../views/CommentView.vue'
import DashboardView from '../views/DashboardView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/products',
      name: 'products',
      component: ProductView // 商品路由
    },
    {
      path: '/comments',  // 评论路由
      name: 'comments',
      component: CommentView
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView
    },
  ],
})

export default router
