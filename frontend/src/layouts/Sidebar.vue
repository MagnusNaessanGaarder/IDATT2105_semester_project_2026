<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NavSection from './NavSection.vue'
import SidebarUser from './SidebarUser.vue'

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
      { id: 'notifications', label: 'Varsler', route: 'Notifications' }
    ]
  },
  ...(user.value?.role === 'ADMIN' ? [{
    key: 'admin',
    label: 'ADMIN',
    icon: 'Settings',
    dashboardRoute: 'Users',
    items: [
      { id: 'users', label: 'Brukere', route: 'Users' },
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

watch(currentScreen, (routeName) => {
  const parentSection = getParentSectionKey(routeName)
  if (parentSection) {
    expandedSections.value = [parentSection]
  }
}, { immediate: true })
</script>

<template>
  <aside 
    class="sidebar"
    :class="{ 'sidebar--open': isOpen }"
    role="navigation"
    aria-label="Hovednavigasjon"
  >
    <div class="sidebar-header">
      <h1 class="app-title">IK-Kontroll</h1>
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
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--color-card);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  z-index: 50;
  transform: translateX(-100%);
  transition: transform var(--transition-base) ease;
}

.sidebar--open {
  transform: translateX(0);
}

@media (min-width: 768px) {
  .sidebar {
    position: static;
    transform: translateX(0);
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 16px;
  border-bottom: 1px solid var(--color-border);
}

.app-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.01em;
  color: var(--color-gray-900);
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  cursor: pointer;
}

@media (min-width: 768px) {
  .close-btn {
    display: none;
  }
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-footer {
  position: sticky;
  bottom: 0;
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
  box-shadow: 0 -6px 16px rgba(15, 23, 42, 0.04);
  z-index: 10;
}

@media (prefers-reduced-motion: reduce) {
  .sidebar {
    transition: none;
  }
}
</style>