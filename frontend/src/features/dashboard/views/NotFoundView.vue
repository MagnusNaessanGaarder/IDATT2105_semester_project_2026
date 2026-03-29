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
  padding: 1.5rem;
}

.not-found-content {
  text-align: center;
  max-width: 25rem;
}

.not-found-content h1 {
  font-size: 6rem;
  font-weight: var(--font-weight-bold);
  color: var(--color-gray-200);
  line-height: 1;
  margin-bottom: 1rem;
}

.not-found-content h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.not-found-content p {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
  margin-bottom: 2rem;
}

.home-btn {
  display: inline-flex;
  align-items: center;
  padding: 0.75rem 1.5rem;
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
  transform: translateY(-0.0625rem);
  box-shadow: var(--shadow-md);
}

.home-btn:focus-visible {
  outline: 0.125rem solid var(--color-focus);
  outline-offset: 0.125rem;
}
</style>