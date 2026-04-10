<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const orgName   = authStore.currentOrg?.orgName ?? authStore.currentOrg?.orgNumber
</script>

<template>
  <div class="inactive-page">
    <div class="inactive-card">

      <div class="inactive-icon" aria-hidden="true">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
        </svg>
      </div>

      <p class="inactive-kicker">Internkontroll</p>
      <h1 class="inactive-title">Organisasjonen er inaktiv</h1>

      <p class="inactive-body">
        <strong>{{ orgName }}</strong> har blitt deaktivert og du har ikke lenger tilgang til systemet.
      </p>

      <p class="inactive-body">
        Hvis dette er en feil, ta kontakt med systemadministrator.
      </p>

      <div class="inactive-contact">
        <p class="inactive-contact__label">Kontakt systemadministrator</p>
        <p class="inactive-contact__value">sysadmin@ik-kontroll.no</p>
      </div>

      <button
          class="inactive-logout"
          type="button"
          @click="authStore.logout(); $router.push({ name: 'Login' })"
      >
        Logg ut
      </button>

    </div>
  </div>
</template>

<style scoped>
.inactive-page {
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

.inactive-card {
  width: 100%;
  max-width: 28rem;
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

.inactive-icon {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  background: var(--color-danger-bg);
  border: 1px solid color-mix(in srgb, var(--color-danger) 25%, var(--color-border));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-danger);
  margin-bottom: 1.25rem;
}

.inactive-kicker {
  font-size: var(--font-size-xs);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 0.4rem;
}

.inactive-title {
  font-family: var(--font-family-display);
  font-size: clamp(1.4rem, 2vw, var(--font-size-2xl));
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--color-foreground);
  margin-bottom: 0.9rem;
}

.inactive-body {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  line-height: var(--line-height-relaxed);
  margin-bottom: 0.6rem;
}

.inactive-contact {
  width: 100%;
  margin: 0.75rem 0 1.5rem;
  padding: 0.85rem 1rem;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.inactive-contact__label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  margin-bottom: 0.3rem;
}

.inactive-contact__value {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
}

.inactive-logout {
  width: 100%;
  padding: var(--button-padding-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: background-color var(--transition-fast), transform var(--transition-fast);
}
.inactive-logout:hover {
  opacity: 0.88;
  transform: translateY(-1px);
}
</style>