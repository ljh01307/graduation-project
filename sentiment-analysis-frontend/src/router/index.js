import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProductView from '../views/ProductView.vue'
import CommentView from '../views/CommentView.vue'
import DashboardView from '../views/DashboardView.vue'
import UserView from '../views/UserView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import { isLoggedIn, getToken, getUserInfo } from '../utils/auth.js'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true }
    },
    {
      path: '/users',
      name: 'users',
      component: UserView,
      meta: { requiresAuth: true, adminOnly: true }
    },
    {
      path: '/products',
      name: 'products',
      component: ProductView,
      meta: { requiresAuth: true }
    },
    {
      path: '/comments',
      name: 'comments',
      component: CommentView,
      meta: { requiresAuth: true }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAuth: true }
    },
    
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guest: true }
    }
  ],
})

router.beforeEach((to, from, next) => {
  const loggedIn = isLoggedIn()
  const user = getUserInfo()
  const isAdmin = user && user.role === 'ADMIN'

  if (to.meta.requiresAuth && !loggedIn) {
    next({ name: 'login' })
  } else if (to.meta.guest && loggedIn) {
    next({ name: 'home' })
  } else if (to.meta.adminOnly && !isAdmin) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export function fetchWithAuth(url, options = {}) {
  const token = getToken()
  const headers = {
    ...options.headers
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  return fetch(url, {
    ...options,
    headers
  })
}

export default router
