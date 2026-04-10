<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const email = authStore.email
</script>

<template>
  <div class="no-org-page">
    <div
        class="no-org-container"
        v-motion
        :initial="{ opacity: 0, y: 10 }"
        :enter="{ opacity: 1, y: 0, transition: { duration: 300 } }"
    >
      <div class="no-org-card">
        <div class="no-org-icon" aria-hidden="true">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </div>

        <p class="no-org-kicker">Internkontroll</p>
        <h1 class="no-org-title">Venter på tilgang</h1>
        <p class="no-org-body">
          Kontoen din er opprettet, men du er ikke lagt til i noen organisasjon ennå.
          Be din leder om å legge deg til i teamet.
        </p>

        <div class="no-org-email-box">
          <p class="no-org-email-label">Din e-post</p>
          <p class="no-org-email">{{ email }}</p>
        </div>

        <p class="no-org-hint">
          Send denne e-postadressen til din leder. De vil bruke den for å legge deg til i organisasjonen.
        </p>

        <p class="no-org-hint no-org-hint--sub">
          Allerede blitt lagt til? Logg ut og inn igjen for å oppdatere tilgangen din.
        </p>

        <button class="no-org-logout" @click="authStore.logout(); $router.push({ name: 'Login' })">
          Logg ut
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.no-org-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
      radial-gradient(circle at 8% 14%, var(--color-surface-tint), transparent 32%),
      radial-gradient(circle at 90% 88%, var(--color-surface-tint-strong), transparent 35%),
      linear-gradient(180deg, var(--color-card-muted) 0%, var(--color-background) 100%);
  padding: var(--content-padding);
}

.no-org-container {
  width: 100%;
  max-width: 28rem;
}

.no-org-card {
  background: linear-gradient(180deg, var(--color-card) 0%, var(--color-card-muted) 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: clamp(1.75rem, 4vw, 2.25rem);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.no-org-icon {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  background: var(--color-gray-100);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-gray-500);
  margin-bottom: 1.25rem;
}

.no-org-kicker {
  font-size: var(--font-size-xs);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 0.4rem;
}

.no-org-title {
  font-family: var(--font-family-display);
  font-size: clamp(1.5rem, 2vw, var(--font-size-2xl));
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--color-foreground);
  margin-bottom: 0.75rem;
}

.no-org-body {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  line-height: var(--line-height-relaxed);
  margin-bottom: 1.5rem;
}

.no-org-email-box {
  width: 100%;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.85rem 1rem;
  margin-bottom: 1rem;
}

.no-org-email-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  margin-bottom: 0.3rem;
}

.no-org-email {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
  word-break: break-all;
}

.no-org-hint {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
  line-height: var(--line-height-relaxed);
  margin-bottom: 0.75rem;
}

.no-org-hint--sub {
  font-size: var(--font-size-xs);
  margin-bottom: 1.5rem;
}

.no-org-logout {
  width: 100%;
  padding: var(--button-padding-md);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary-foreground);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: background-color var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}

.no-org-logout:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.no-org-logout:active {
  transform: translateY(0);
}

@media (max-width: 480px) {
  .no-org-card {
    padding: 1.5rem;
  }
}
</style>