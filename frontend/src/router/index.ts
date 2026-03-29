import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    allowedRoles?: ('ADMIN' | 'MANAGER' | 'EMPLOYEE')[]
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/features/auth/views/LoginView.vue'),
      meta: { requiresAuth: false, title: 'Innlogging' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppShell.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/features/dashboard/views/DashboardView.vue'),
          meta: { title: 'Dashboard' },
        },
        {
          path: 'ikmat',
          name: 'IKMatDashboard',
          component: () => import('@/features/ik-mat/views/IKMatDashboardView.vue'),
          meta: { title: 'IK-Mat Dashboard' },
        },
        {
          path: 'ikmat/sjekklister',
          name: 'Checklists',
          component: () => import('@/features/ik-mat/views/ChecklistsView.vue'),
          meta: { title: 'Sjekklister' },
        },
        {
          path: 'ikmat/temperatur',
          name: 'Temperature',
          component: () => import('@/features/ik-mat/views/TemperatureView.vue'),
          meta: { title: 'Temperaturlogging' },
        },
        {
          path: 'ikmat/avvik',
          name: 'Deviations',
          component: () => import('@/features/ik-mat/views/DeviationsView.vue'),
          meta: { title: 'Avvikshåndtering' },
        },
        {
          path: 'ikmat/haccp',
          name: 'HACCP',
          component: () => import('@/features/ik-mat/views/HACCPView.vue'),
          meta: { title: 'HACCP-plan' },
        },
        {
          path: 'alkohol',
          name: 'AlkoholDashboard',
          component: () => import('@/features/ik-alkohol/views/AlkoholDashboardView.vue'),
          meta: { title: 'IK-Alkohol Dashboard' },
        },
        {
          path: 'alkohol/daglig-kontroll',
          name: 'DailyControl',
          component: () => import('@/features/ik-alkohol/views/DailyControlView.vue'),
          meta: { title: 'Daglig kontroll' },
        },
        {
          path: 'alkohol/sertifiseringer',
          name: 'Certifications',
          component: () => import('@/features/ik-alkohol/views/CertificationsView.vue'),
          meta: { title: 'Sertifiseringer' },
        },
        {
          path: 'alkohol/regelverk',
          name: 'Regulations',
          component: () => import('@/features/ik-alkohol/views/RegulationsView.vue'),
          meta: { title: 'Regelverk' },
        },
        {
          path: 'rapporter',
          name: 'Reports',
          component: () => import('@/features/dashboard/views/ReportsView.vue'),
          meta: { title: 'Rapporter' },
        },
        {
          path: 'dokumenter',
          name: 'Documents',
          component: () => import('@/features/dashboard/views/DocumentsView.vue'),
          meta: { title: 'Dokumenter' },
        },
        {
          path: 'varsler',
          name: 'Notifications',
          component: () => import('@/features/dashboard/views/NotificationsView.vue'),
          meta: { title: 'Varsler' },
        },
        {
          path: 'admin/brukere',
          name: 'Users',
          component: () => import('@/features/admin/views/UsersView.vue'),
          meta: { title: 'Brukere', allowedRoles: ['ADMIN'] },
        },
        {
          path: 'admin/innstillinger',
          name: 'Settings',
          component: () => import('@/features/admin/views/SettingsView.vue'),
          meta: { title: 'Innstillinger', allowedRoles: ['ADMIN', 'MANAGER'] },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/features/dashboard/views/NotFoundView.vue'),
      meta: { title: 'Side ikke funnet' },
    },
  ],
})

router.beforeEach(async (to, from) => {
  const authStore = useAuthStore()
  
  if (to.meta.title) {
    document.title = `${to.meta.title} - IK-Kontroll`
  }
  
  if (to.meta.requiresAuth !== false) {
    if (!authStore.isAuthenticated && !authStore.hasCheckedAuth) {
      await authStore.checkAuth()
    }
    
    if (!authStore.isAuthenticated) {
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
    
    if (to.meta.allowedRoles && to.meta.allowedRoles.length > 0) {
      const userRole = authStore.user?.role
      if (!userRole || !to.meta.allowedRoles.includes(userRole)) {
        return { name: 'Dashboard' }
      }
    }
  }
  
  if (to.name === 'Login' && authStore.isAuthenticated) {
    return { name: 'Dashboard' }
  }
  
  // Return nothing to allow navigation
})

export default router