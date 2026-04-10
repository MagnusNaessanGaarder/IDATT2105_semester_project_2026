import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ensureOrganizationSettings, isModuleEnabled } from '@/shared/utils/orgSettings'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresSysadmin?: boolean
    allowedRoles?: ('ADMIN' | 'MANAGER' | 'EMPLOYEE')[]
    moduleKey?: 'food' | 'alcohol'
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
      path: '/register',
      name: 'Register',
      component: () => import('@/features/auth/views/RegisterView.vue'),
      meta: { requiresAuth: false, title: 'Registrering' },
    },
    {
      path: '/sysadmin',
      component: () => import('@/layouts/SysadminLayout.vue'),
      meta: { requiresAuth: true, requiresSysadmin: true },
      children: [
        {
          path: '',
          name: 'SysadminOrgs',
          component: () => import('@/features/sysadmin/views/SysadminOrgsView.vue'),
          meta: { title: 'Organisasjoner – Systemadmin' },
        },
      ],
    },
    {
      path: '/ingen-organisasjon',
      name: 'NoOrganization',
      component: () => import('@/features/auth/views/NoOrganizationView.vue'),
      meta: { requiresAuth: true, title: 'Venter på tilgang' },
    },
    {
      path: '/organisasjon-inaktiv',
      name: 'OrgInactive',
      component: () => import('@/features/auth/views/OrgInactiveView.vue'),
      meta: { requiresAuth: true, title: 'Organisasjon inaktiv' },
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
          meta: { title: 'IK-Mat Dashboard', moduleKey: 'food' },
        },
        {
          path: 'ikmat/sjekklister',
          name: 'Checklists',
          component: () => import('@/features/ik-mat/views/ChecklistsView.vue'),
          meta: { title: 'Sjekklister', moduleKey: 'food' },
        },
        {
          path: 'ikmat/temperatur',
          name: 'Temperature',
          component: () => import('@/features/ik-mat/views/TemperatureView.vue'),
          meta: { title: 'Temperaturlogging', moduleKey: 'food' },
        },
        {
          path: 'ikmat/avvik',
          name: 'Deviations',
          component: () => import('@/features/ik-mat/views/DeviationsView.vue'),
          meta: { title: 'Avvikshåndtering', moduleKey: 'food' },
        },
        {
          path: 'ikmat/haccp',
          name: 'HACCP',
          component: () => import('@/features/ik-mat/views/HACCPView.vue'),
          meta: { title: 'HACCP-plan', moduleKey: 'food' },
        },
        {
          path: 'alkohol',
          name: 'AlkoholDashboard',
          component: () => import('@/features/ik-alkohol/views/AlkoholDashboardView.vue'),
          meta: { title: 'IK-Alkohol Dashboard', moduleKey: 'alcohol' },
        },
        {
          path: 'alkohol/daglig-kontroll',
          name: 'DailyControl',
          component: () => import('@/features/ik-alkohol/views/DailyControlView.vue'),
          meta: { title: 'Daglig kontroll', moduleKey: 'alcohol' },
        },
        {
          path: 'alkohol/sertifiseringer',
          name: 'Certifications',
          component: () => import('@/features/ik-alkohol/views/CertificationsView.vue'),
          meta: { title: 'Sertifiseringer', moduleKey: 'alcohol' },
        },
        {
          path: 'alkohol/regelverk',
          name: 'Regulations',
          component: () => import('@/features/ik-alkohol/views/RegulationsView.vue'),
          meta: { title: 'Regelverk', moduleKey: 'alcohol' },
        },
        {
          path: 'opplaering',
          name: 'EmployeeTraining',
          component: () => import('@/features/dashboard/views/EmployeeTrainingView.vue'),
          meta: { title: 'Opplæringsregister' },
        },
        {
          path: 'rapporter',
          name: 'Reports',
          component: () => import('@/features/dashboard/views/ReportsView.vue'),
          meta: { title: 'Rapporter' },
        },
        {
          path: 'eksport',
          name: 'Export',
          component: () => import('@/features/export/views/ExportView.vue'),
          meta: { title: 'Eksport', allowedRoles: ['MANAGER', 'ADMIN'] },
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
          // Role policy: user administration is restricted to ADMIN.
          meta: { title: 'Brukere', allowedRoles: ['ADMIN'] },
        },
        {
          path: 'admin/opplaering',
          name: 'Training',
          component: () => import('@/features/admin/views/TrainingView.vue'),
          // Role policy: user administration is restricted to ADMIN.
          meta: { title: 'Opplaering', allowedRoles: ['ADMIN'] },
        },
        {
          path: 'admin/lokasjoner',
          name: 'Locations',
          component: () => import('@/features/admin/views/LocationsView.vue'),
          meta: { title: 'Lokasjoner', allowedRoles: ['ADMIN', 'MANAGER'] },
        },
        {
          path: 'admin/innstillinger',
          name: 'Settings',
          component: () => import('@/features/admin/views/SettingsView.vue'),
          // Role policy: ADMIN and MANAGER can inspect/configure organization settings.
          meta: { title: 'Innstillinger', allowedRoles: ['ADMIN', 'MANAGER'] },
        },
        {
          path: 'forbidden',
          name: 'Forbidden',
          component: () => import('@/features/dashboard/views/ForbiddenView.vue'),
          meta: { title: 'Ingen tilgang' },
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

router.beforeEach(async (to) => {
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

    // Sysadmin users only see the sysadmin area
    if (authStore.isSysadmin && !to.meta.requiresSysadmin) {
      return { name: 'SysadminOrgs' }
    }

    // Non-sysadmin users cannot access the sysadmin area
    if (to.meta.requiresSysadmin && !authStore.isSysadmin) {
      return { name: 'Forbidden' }
    }

    // Users with no org see the waiting screen
    if (
        to.name !== 'NoOrganization' &&
        to.name !== 'OrgInactive' &&
        !authStore.isSysadmin &&
        (authStore.organizations?.length ?? 0) === 0
    ) {
      return { name: 'NoOrganization' }
    }

    // Users whose org has been deactivated by sysadmin see the inactive screen
    if (
        to.name !== 'OrgInactive' &&
        to.name !== 'NoOrganization' &&
        !authStore.isSysadmin &&
        authStore.currentOrg?.isActive === false
    ) {
      return { name: 'OrgInactive' }
    }

    if (to.meta.allowedRoles && to.meta.allowedRoles.length > 0) {
      const userRole = authStore.user?.role
      if (!userRole || !to.meta.allowedRoles.includes(userRole as 'ADMIN' | 'MANAGER' | 'EMPLOYEE')) {
        return { name: 'Forbidden' }
      }
    }

    const currentOrgNumber = authStore.currentOrg?.orgNumber
    if (currentOrgNumber) {
      await ensureOrganizationSettings(currentOrgNumber)
      if (to.meta.moduleKey && !isModuleEnabled(to.meta.moduleKey, currentOrgNumber)) {
        return { name: 'Forbidden' }
      }
    }
  }

  if ((to.name === 'Login' || to.name === 'Register') && authStore.isAuthenticated) {
    return authStore.isSysadmin ? { name: 'SysadminOrgs' } : { name: 'Dashboard' }
  }

})

export default router