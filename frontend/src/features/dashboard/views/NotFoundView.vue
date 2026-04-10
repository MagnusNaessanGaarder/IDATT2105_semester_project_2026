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
  background:
    radial-gradient(circle at 18% 16%, var(--color-surface-tint), transparent 30%),
    radial-gradient(circle at 82% 84%, var(--color-surface-tint-strong), transparent 30%),
    linear-gradient(180deg, var(--color-surface) 0%, var(--color-background) 100%);
  padding: var(--content-padding);
}

.not-found-content {
  text-align: center;
  max-width: 400px;
}

.not-found-content h1 {
  font-size: clamp(4.5rem, 10vw, 6rem);
  font-weight: var(--font-weight-bold);
  color: color-mix(in srgb, var(--color-foreground) 12%, transparent);
  line-height: 1;
  margin-bottom: var(--spacing-md);
}

.not-found-content h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: var(--spacing-xs);
}

.not-found-content p {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
  margin-bottom: var(--spacing-2xl);
}

.home-btn {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary-foreground);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.home-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.home-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>