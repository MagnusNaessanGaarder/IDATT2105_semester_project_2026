<script setup lang="ts">
import { motion } from 'motion-v'
import { computed } from 'vue'
import NavMenuItem from '../components/NavMenuItem.vue'

interface NavItem {
  id: string
  label: string
  route: string
  badge?: number
}

interface Section {
  key: string
  label: string
  icon: string
  dashboardRoute: string
  items: NavItem[]
}

const props = defineProps<{
  section: Section
  isExpanded: boolean
  currentScreen: string
}>()

const emit = defineEmits<{
  toggle: []
  navigateDashboard: [routeName: string]
  navigateScreen: [routeName: string]
}>()

const isMainActive = computed(() => {
  if (props.currentScreen === props.section.dashboardRoute) {
    return true
  }

  return props.section.items.some(item => item.route === props.currentScreen)
})

const isAnyChildActive = computed(() => {
  return props.section.items.some((item) => item.route === props.currentScreen)
})

const sectionBadgeCount = computed(() => {
  return props.section.items.reduce((acc, item) => acc + (item.badge || 0), 0)
})

// Check if a specific item is the active child
const isItemActive = (routeName: string) => {
  return props.currentScreen === routeName
}

const handleHeaderClick = () => {
  if (props.currentScreen !== props.section.dashboardRoute) {
    emit('navigateDashboard', props.section.dashboardRoute)
    return
  }

  emit('toggle')
}

// Navigate to specific screen
const navigateToScreen = (routeName: string) => {
  emit('navigateScreen', routeName)
}

// Simple icon component mapping
const getIcon = (iconName: string) => {
  const icons: Record<string, string> = {
    'Salad': `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a10 10 0 1 0 10 10H12V2z"/><path d="M12 12L2.5 12"/></svg>`,
    'Wine': `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 22h8"/><path d="M7 10h10v4a5 5 0 0 1-10 0v-4z"/><path d="M12 2v8"/></svg>`,
    'FolderOpen': `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 7h11.5a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z"/><path d="M6 7V5a2 2 0 0 1 2-2h4.5"/><path d="M6 7l2-2h3"/></svg>`,
    'Settings': `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`
  }
  return icons[iconName] || ''
}
</script>

<template>
  <motion.div
    class="nav-section"
    :class="{ 'nav-section--active': isMainActive }"
  >
    <NavMenuItem
      variant="main"
      :label="section.label"
      :icon="getIcon(section.icon)"
      :badge="sectionBadgeCount"
      :active="isMainActive"
      :expanded="isExpanded"
      @select="handleHeaderClick"
    />
    
    <transition name="collapse">
      <motion.div
        v-show="isExpanded"
        class="submenu"
        :initial="{ opacity: 0, y: -6 }"
        :animate="{ opacity: 1, y: 0, transition: { duration: 0.22 } }"
      >
        <motion.div
          class="nav-items"
          :id="`section-${section.key}-items`"
          role="group"
          :aria-label="`${section.label} menyelementer`"
        >
          <motion.div
            v-for="item in section.items"
            :key="item.id"
            class="nav-item-wrapper"
            :initial="{ opacity: 0, x: -6 }"
            :animate="{ opacity: 1, x: 0, transition: { duration: 0.18, delay: 0.03 * section.items.findIndex(i => i.id === item.id) } }"
          >
            <NavMenuItem
              variant="sub"
              :label="item.label"
              :badge="item.badge || 0"
              :active="isItemActive(item.route)"
              @select="navigateToScreen(item.route)"
            />
          </motion.div>
        </motion.div>
      </motion.div>
    </transition>
  </motion.div>
</template>

<style scoped>
.nav-section {
  margin-bottom: 0;
  width: 100%;
}

.submenu {
  overflow: hidden;
  border-left: none;
  margin-left: 0;
  margin-top: 0;
  background: none;
}

.nav-items {
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0;
}

.nav-item-wrapper {
  margin: 0;
}

/* Collapse transition */
.collapse-enter-active,
.collapse-leave-active {
  transition: max-height var(--transition-base) ease, opacity var(--transition-base) ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  max-height: 31.25rem;
  opacity: 1;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .collapse-enter-active,
  .collapse-leave-active {
    transition: none;
  }
}
</style>