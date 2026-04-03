<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'

const isSidebarOpen = ref(false)

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
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
  background:
    radial-gradient(circle at 8% 8%, rgba(203, 213, 225, 0.25), transparent 36%),
    linear-gradient(180deg, #fbfdff 0%, var(--color-background) 100%);
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

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
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
  padding: var(--spacing-lg);
}

@media (min-width: 768px) {
  .main-content {
    padding: var(--spacing-xl);
  }
}

@media (prefers-reduced-motion: reduce) {
  .main-content {
    animation: none;
  }
}
</style>