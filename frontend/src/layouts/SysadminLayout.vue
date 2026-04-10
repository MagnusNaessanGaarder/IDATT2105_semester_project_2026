<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router    = useRouter()

function logout() {
  authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<template>
  <div class="sysadmin-shell">
    <header class="topbar">
      <div class="topbar__brand">
        <span class="topbar__logo">IK-Kontroll</span>
        <span class="topbar__badge">Systemadmin</span>
      </div>
      <div class="topbar__right">
        <span class="topbar__user">{{ authStore.email }}</span>
        <button class="topbar__logout" type="button" @click="logout">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          Logg ut
        </button>
      </div>
    </header>

    <main class="sysadmin-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.sysadmin-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
}

.topbar {
  height: 3.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
  flex-shrink: 0;
}

.topbar__brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.topbar__logo {
  font-size: var(--font-size-base);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.topbar__badge {
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.15);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.topbar__user {
  font-size: var(--font-size-sm);
  opacity: 0.75;
}

.topbar__logout {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2rem;
  padding: 0.3rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition-fast);
}
.topbar__logout:hover {
  background: rgba(255, 255, 255, 0.1);
}

.sysadmin-main {
  flex: 1;
  padding: clamp(1.5rem, 3vw, 2.5rem) clamp(1rem, 4vw, 3rem);
  max-width: 72rem;
  width: 100%;
  margin: 0 auto;
}
</style>