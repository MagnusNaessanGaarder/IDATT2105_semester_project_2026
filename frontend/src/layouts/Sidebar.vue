<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { motion } from 'motion-v'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NavSection from './NavSection.vue'
import SidebarUser from './SidebarUser.vue'

defineOptions({
  name: 'AppSidebar',
})

const props = defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const expandedSections = ref<string[]>([])
const user = computed(() => authStore.user)
const isDesktop = ref(false)
const prefersReducedMotion = ref(false)

const fallbackBusinessName = 'Internkontroll'

const businessLabel = computed(() => {
  const email = user.value?.email
  if (!email || !email.includes('@')) {
    return fallbackBusinessName
  }

  const domain = email.split('@')[1]
  if (!domain) {
    return fallbackBusinessName
  }

  const rootDomain = domain.split('.')[0]

  if (!rootDomain || ['gmail', 'outlook', 'hotmail', 'yahoo', 'icloud'].includes(rootDomain.toLowerCase())) {
    return fallbackBusinessName
  }

  return rootDomain
      .split('-')
      .map((part: string) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ')
})

const sections = computed(() => [
  {
    key: 'ikmat',
    label: 'IK-MAT',
    icon: 'Salad',
    dashboardRoute: 'IKMatDashboard',
    items: [
      { id: 'checklists', label: 'Sjekklister', route: 'Checklists' },
      { id: 'temperature', label: 'Temperatur', route: 'Temperature' },
      { id: 'deviations', label: 'Avvik', route: 'Deviations' },
      { id: 'haccp', label: 'HACCP-plan', route: 'HACCP' }
    ]
  },
  {
    key: 'alkohol',
    label: 'IK-ALKOHOL',
    icon: 'Wine',
    dashboardRoute: 'AlkoholDashboard',
    items: [
      { id: 'daily-control', label: 'Daglig kontroll', route: 'DailyControl' },
      { id: 'certifications', label: 'Sertifiseringer', route: 'Certifications' },
      { id: 'regulations', label: 'Regelverk', route: 'Regulations' }
    ]
  },
  {
    key: 'felles',
    label: 'FELLES',
    icon: 'FolderOpen',
    dashboardRoute: 'Dashboard',
    items: [
      { id: 'reports', label: 'Rapporter', route: 'Reports' },
      { id: 'documents', label: 'Dokumenter', route: 'Documents' },
      { id: 'notifications', label: 'Varsler', route: 'Notifications' },
      ...(user.value?.role === 'MANAGER' || user.value?.role === 'ADMIN'
          ? [{ id: 'export', label: 'Eksport', route: 'Export' }]
          : [])
    ]
  },
  ...((user.value?.role === 'ADMIN' || user.value?.role === 'MANAGER') ? [{
    key: 'admin',
    label: 'ADMIN',
    icon: 'Settings',
    dashboardRoute: user.value?.role === 'ADMIN' ? 'Users' : 'Settings',
    items: [
      ...(user.value?.role === 'ADMIN'
          ? [{ id: 'users', label: 'Brukere', route: 'Users' }]
          : []),
      { id: 'locations', label: 'Lokasjoner', route: 'Locations' },
      { id: 'settings', label: 'Innstillinger', route: 'Settings' }
    ]
  }] : [])
])

const isSectionExpanded = (key: string) => expandedSections.value.includes(key)

const toggleSection = (key: string) => {
  const index = expandedSections.value.indexOf(key)
  if (index > -1) {
    // Collapse if already expanded
    expandedSections.value.splice(index, 1)
  } else {
    // Collapse all other sections and expand this one
    expandedSections.value = [key]
  }
}

// Get parent section that contains the current screen
const getParentSectionKey = (screenId: string): string | null => {
  for (const section of sections.value) {
    if (section.dashboardRoute === screenId || section.items.some(item => item.route === screenId)) {
      return section.key
    }
  }
  return null
}

const navigateToDashboard = (routeName: string) => {
  if (currentScreen.value !== routeName) {
    router.push({ name: routeName })
  }

  emit('close')
}

const navigateToScreen = (routeName: string, sectionKey: string) => {
  if (currentScreen.value !== routeName) {
    router.push({ name: routeName })
  }

  expandedSections.value = [sectionKey]
  emit('close')
}

const currentScreen = computed(() => {
  const routeName = route.name as string
  return routeName || ''
})

const updateViewport = () => {
  if (typeof window === 'undefined') {
    return
  }

  isDesktop.value = window.matchMedia('(min-width: 48rem)').matches
  prefersReducedMotion.value = window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})

const sidebarAnimation = computed(() => {
  const x = isDesktop.value || props.isOpen ? '0%' : '-100%'

  return {
    x,
    transition: {
      duration: prefersReducedMotion.value ? 0 : 0.22,
    },
  }
})

const sidebarInitial = computed(() => {
  const x = isDesktop.value || props.isOpen ? '0%' : '-100%'
  return { x }
})

watch(currentScreen, (routeName) => {
  const parentSection = getParentSectionKey(routeName)
  if (parentSection) {
    expandedSections.value = [parentSection]
  }
}, { immediate: true })
</script>

<template>
  <motion.aside
      class="sidebar"
      :initial="sidebarInitial"
      :animate="sidebarAnimation"
      role="navigation"
      aria-label="Hovednavigasjon"
  >
    <div class="sidebar-header">
      <div class="brand-block">
        <h1 class="app-title">IK-Kontroll</h1>
        <p class="app-business">{{ businessLabel }}</p>
      </div>
      <button
          v-if="isOpen"
          class="close-btn"
          @click="emit('close')"
          aria-label="Lukk meny"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>

    <nav class="sidebar-nav">
      <NavSection
          v-for="section in sections"
          :key="section.key"
          :section="section"
          :is-expanded="isSectionExpanded(section.key)"
          :current-screen="currentScreen"
          @toggle="toggleSection(section.key)"
          @navigate-dashboard="(route) => navigateToDashboard(route)"
          @navigate-screen="(route) => navigateToScreen(route, section.key)"
      />
    </nav>

    <div class="sidebar-footer">
      <SidebarUser
          v-if="user"
          :user="user"
      />
    </div>
  </motion.aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  left: 0;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--color-card);
  border-right: 0.0625rem solid var(--color-border);
  display: flex;
  flex-direction: column;
  z-index: 50;
}

@media (min-width: 48rem) {
  .sidebar {
    position: sticky;
    top: 0;
    left: 0;
    width: var(--sidebar-width);
    height: 100vh;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(251, 250, 247, 0.98) 100%);
    border-right: 0.0625rem solid var(--color-border);
    display: flex;
    flex-direction: column;
    z-index: 50;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.5), var(--shadow-sm);
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg) var(--spacing-md) var(--spacing-md);
  border-bottom: 0.0625rem solid var(--color-border);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.02) 0%, rgba(15, 23, 42, 0) 100%);
}

.brand-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.app-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.02em;
  color: var(--color-primary);
  margin: 0;
}

.app-business {
  margin: 0;
  font-size: var(--font-size-xs);
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-gray-500);
  font-weight: var(--font-weight-medium);
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  background: transparent;
  border: none;
  cursor: pointer;
}

@media (min-width: 48rem) {
  .close-btn {
    display: none;
  }
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm) 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.sidebar-footer {
  position: sticky;
  bottom: 0;
  background: var(--color-card);
  border-top: 0.0625rem solid var(--color-border);
  box-shadow: 0 -0.375rem 1rem rgba(15, 23, 42, 0.04);
  z-index: 10;
}

</style>