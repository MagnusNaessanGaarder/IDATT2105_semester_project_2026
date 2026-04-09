<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'

const isSidebarOpen = ref(false)

const openSidebar = () => {
  isSidebarOpen.value = true
}

const closeSidebar = () => {
  isSidebarOpen.value = false
}
</script>

<template>
  <div class="app-shell">
    <a href="#main-content" class="skip-link">
      Hopp til hovedinnhold
    </a>

    <button
      class="sidebar-toggle"
      type="button"
      aria-label="Åpne meny"
      @click="openSidebar"
    >
      ☰
    </button>
    
    <button
      class="menu-toggle"
      type="button"
      aria-label="Aapne meny"
      @click="toggleSidebar"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="3" y1="6" x2="21" y2="6" />
        <line x1="3" y1="12" x2="21" y2="12" />
        <line x1="3" y1="18" x2="21" y2="18" />
      </svg>
    </button>

    <div 
      v-if="isSidebarOpen" 
      class="sidebar-backdrop"
      @click="closeSidebar"
    />
    
    <Sidebar 
      :is-open="isSidebarOpen"
      @close="closeSidebar"
    />
    
    <div class="main-wrapper">
      <main 
        id="main-content" 
        class="main-content"
        tabindex="-1"
        v-motion
        :initial="{ opacity: 0, y: 12 }"
        :enter="{ opacity: 1, y: 0, transition: { duration: 320, easing: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)' } }"
        :key="$route.fullPath"
      >
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  width: 100%;
  position: relative;
  isolation: isolate;
  background: linear-gradient(180deg, var(--color-card-muted) 0%, var(--color-background) 100%);
}

.skip-link {
  position: absolute;
  top: -100px;
  left: 16px;
  z-index: 100;
  padding: 8px 16px;
  background-color: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
}

.skip-link:focus {
  top: 8px;
}

.sidebar-toggle {
  position: fixed;
  top: 12px;
  left: 12px;
  z-index: 60;
  min-height: 44px;
  min-width: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  color: var(--color-foreground);
  font-size: 1.1rem;
  font-weight: 700;
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: var(--color-overlay-soft);
  z-index: 40;
}

.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg) var(--spacing-md);
  content-visibility: auto;
  contain-intrinsic-size: 800px;
}

@media (min-width: 768px) {
  .sidebar-toggle {
    display: none;
  }

  .main-content {
    padding: var(--spacing-xl) clamp(1.25rem, 2.2vw, 2.5rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  .main-content {
    animation: none;
  }
}
</style>
