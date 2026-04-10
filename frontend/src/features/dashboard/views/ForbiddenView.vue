<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goToDashboard = () => {
  router.push({ name: 'Dashboard' })
}
</script>

<template>
  <section class="forbidden-view">
    <h1>Ingen tilgang</h1>
    <p>Du har ikke nødvendige rettigheter for denne siden.</p>
    <button class="forbidden-view__button" type="button" @click="goToDashboard">
      Tilbake til dashboard
    </button>
  </section>
</template>

<style scoped>
.forbidden-view {
  display: grid;
  gap: 0.75rem;
  max-width: 32rem;
  margin: 2rem auto;
  padding: 1.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
}

.forbidden-view h1 {
  margin: 0;
  font-size: var(--font-size-2xl);
}

.forbidden-view p {
  margin: 0;
  color: var(--color-gray-600);
}

.forbidden-view__button {
  min-height: var(--touch-target);
  width: fit-content;
  padding: 0.45rem 0.95rem;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 600;
}
</style>
