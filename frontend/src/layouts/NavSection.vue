<script setup lang="ts">
import { computed } from 'vue'

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
  color: string
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

// Check if this section is active (current screen is in this section)
const isActive = computed(() => {
  // Check if current screen is dashboard or any child item
  const dashboardScreenId = props.section.key + '-dashboard'
  const itemIds = props.section.items.map(item => item.id)
  
  return props.currentScreen === dashboardScreenId || 
         itemIds.includes(props.currentScreen)
})

// Navigate to dashboard and expand section
const handleHeaderClick = () => {
  emit('navigateDashboard', props.section.dashboardRoute)
  if (!props.isExpanded) {
    emit('toggle')
  }
}

// Just toggle expansion (without navigation)
const handleToggleClick = (e: Event) => {
  e.stopPropagation()
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
  <div 
    class="nav-section"
    :class="{ 'nav-section--active': isActive }"
  >
    <!-- Section Header -->
    <div 
      class="section-header"
      :class="{ 'section-header--active': isActive }"
    >
      <button 
        class="section-header__content"
        :aria-expanded="isExpanded"
        @click="handleHeaderClick"
      >
        <span class="section-header__icon" v-html="getIcon(section.icon)" />
        <span class="section-header__label">{{ section.label }}</span>
        <span 
          v-if="section.items.some(i => i.badge)"
          class="section-header__badge"
        >
          {{ section.items.reduce((acc, item) => acc + (item.badge || 0), 0) }}
        </span>
      </button>
      
      <!-- Toggle button -->
      <button 
        class="section-header__toggle"
        @click="handleToggleClick"
        :aria-label="isExpanded ? 'Kollaps seksjon' : 'Ekspander seksjon'"
      >
        <svg 
          width="14" 
          height="14" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          stroke-width="2"
          class="chevron-icon"
          :class="{ 'chevron-icon--rotated': !isExpanded }"
        >
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </button>
    </div>
    
    <!-- Collapsible Items -->
    <transition name="collapse">
      <ul 
        v-show="isExpanded"
        class="nav-items"
        :id="`section-${section.key}-items`"
        role="list"
        :aria-label="`${section.label} menyelementer`"
      >
        <li 
          v-for="item in section.items" 
          :key="item.id"
          class="nav-item-wrapper"
        >
          <button
            class="nav-item"
            :class="{ 'nav-item--active': currentScreen === item.id }"
            @click="navigateToScreen(item.route)"
            :aria-current="currentScreen === item.id ? 'page' : undefined"
          >
            <span class="nav-item__label">{{ item.label }}</span>
            <span 
              v-if="item.badge"
              class="nav-item__badge"
            >
              {{ item.badge }}
            </span>
          </button>
        </li>
      </ul>
    </transition>
  </div>
</template>

<style scoped>
.nav-section {
  margin-bottom: 4px;
}

/* Section Header */
.section-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  background: transparent;
  border: none;
  border-left: 3px solid transparent;
  transition: all 0.15s ease;
}

.section-header__content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}

.section-header__content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.section-header__icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-gray-500);
}

.section-header__label {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-gray-500);
}

.section-header__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #DC2626;
  color: #FFFFFF;
  font-size: 11px;
  font-weight: 700;
  border-radius: 9999px;
  margin-left: auto;
}

/* Toggle button */
.section-header__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-gray-500);
  transition: all 0.15s ease;
  border-radius: 4px;
}

.section-header__toggle:hover {
  background-color: var(--color-gray-100);
}

.section-header__toggle:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.chevron-icon {
  transition: transform 0.2s ease;
}

.chevron-icon--rotated {
  transform: rotate(-90deg);
}

/* Active state */
.section-header--active {
  background-color: var(--color-gray-100);
  border-left-color: var(--color-gray-900);
}

.section-header--active .section-header__icon,
.section-header--active .section-header__label {
  color: var(--color-gray-900);
}

.section-header--active .section-header__toggle {
  color: var(--color-gray-900);
}

/* Hover state */
.section-header:hover {
  background-color: var(--color-gray-50);
}

.section-header:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

/* Nav Items */
.nav-items {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.nav-item-wrapper {
  margin: 0;
}

.nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px 10px 44px;
  background: transparent;
  border: none;
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
  text-align: left;
}

.nav-item__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-gray-500);
}

.nav-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background-color: #DC2626;
  color: #FFFFFF;
  font-size: 11px;
  font-weight: 700;
  border-radius: 9999px;
}

/* Active nav item */
.nav-item--active {
  background-color: var(--color-gray-100);
  border-left-color: var(--color-gray-900);
}

.nav-item--active .nav-item__label {
  font-weight: 600;
  color: var(--color-gray-900);
}

/* Hover */
.nav-item:hover {
  background-color: var(--color-gray-50);
}

.nav-item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

/* Collapse transition */
.collapse-enter-active,
.collapse-leave-active {
  transition: max-height 0.2s ease, opacity 0.2s ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  max-height: 500px;
  opacity: 1;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .section-header,
  .nav-item,
  .section-header__toggle,
  .chevron-icon {
    transition: none;
  }
  
  .collapse-enter-active,
  .collapse-leave-active {
    transition: none;
  }
}
</style>