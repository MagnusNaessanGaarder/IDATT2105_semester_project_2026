<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push({ name: 'Dashboard' })
}
</script>

<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <h2>Siden ble ikke funnet</h2>
      <p>Siden du leter etter eksisterer ikke eller har blitt flyttet.</p>
      <button @click="goHome" class="home-btn">
        Tilbake til Dashboard
      </button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-background);
  padding: 24px;
}

.not-found-content {
  text-align: center;
  max-width: 400px;
}

.not-found-content h1 {
  font-size: 96px;
  font-weight: var(--font-weight-bold);
  color: var(--color-gray-200);
  line-height: 1;
  margin-bottom: 16px;
}

.not-found-content h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.not-found-content p {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
  margin-bottom: 32px;
}

.home-btn {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary-foreground);
  background: var(--color-foreground);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.home-btn:hover {
  background: var(--color-gray-800);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.home-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>