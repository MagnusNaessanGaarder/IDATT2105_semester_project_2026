<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('admin@everest-sushi.no')
const password = ref('Test1234!')
const error = ref('')
const isLoading = ref(false)

const handleLogin = async () => {
  error.value = ''
  isLoading.value = true

  try {
    await authStore.login({
      email: email.value,
      password: password.value,
    })

    // redirect to dashboard
    const redirect = route.query.redirect as string
    router.push(redirect || { name: 'Dashboard' })
  } catch (e) {
    error.value = 'Ugyldig e-post eller passord'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div
      class="login-container"
      v-motion
      :initial="{ opacity: 0, y: 10 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 300 } }"
    >
      <form class="login-form" @submit.prevent="handleLogin">
        <p class="login-kicker">Internkontroll</p>
        <h2>Logg inn</h2>
        <p class="login-subtitle">Få oversikt over rutiner, avvik og dokumentasjon.</p>

        <div v-if="error" class="error-message" role="alert">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          {{ error }}
        </div>

        <div class="form-group">
          <label for="email">E-post</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            autocomplete="email"
            placeholder="din@epost.no"
          />
        </div>

        <div class="form-group">
          <label for="password">Passord</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="passord"
          />
        </div>

        <button type="submit" class="login-btn" :disabled="isLoading">
          <span v-if="isLoading">Logger inn...</span>
          <span v-else>Logg inn</span>
        </button>
      </form>

      <footer class="login-footer"></footer>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 10% 12%, var(--color-surface-tint), transparent 34%),
    radial-gradient(circle at 90% 90%, var(--color-surface-tint-strong), transparent 36%),
    linear-gradient(180deg, var(--color-card-muted) 0%, var(--color-background) 100%);
  padding: var(--spacing-lg);
}

.login-container {
  width: 100%;
  max-width: 26rem;
}

/* Form */
.login-form {
  background: linear-gradient(180deg, var(--color-card) 0%, var(--color-card-muted) 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: clamp(1.5rem, 4vw, 2rem);
  box-shadow: var(--shadow-md);
}

.login-kicker {
  margin-bottom: 0.375rem;
  font-size: var(--font-size-xs);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
}

.login-form h2 {
  font-family: var(--font-family-display);
  font-size: var(--font-size-xl);
  font-weight: 700;
  letter-spacing: -0.005em;
  line-height: var(--line-height-heading);
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.login-subtitle {
  margin-bottom: 1.25rem;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-relaxed);
}

/* Error Message */
.error-message {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  margin-bottom: var(--spacing-md);
  background-color: var(--color-danger-bg);
  color: var(--color-danger-fg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-danger-border);
  font-size: var(--font-size-sm);
}

/* Form Group */
.form-group {
  margin-bottom: var(--spacing-md);
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-foreground);
  margin-bottom: 6px;
}

.form-group input {
  width: 100%;
  padding: var(--input-padding);
  font-size: var(--font-size-base);
  color: var(--color-foreground);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background-color var(--transition-fast);
}

.form-group input:focus {
  border-color: var(--color-focus);
  box-shadow: var(--shadow-focus);
  background: var(--color-card);
}

.form-group input::placeholder {
  color: var(--color-gray-400);
}

/* Login Button */
.login-btn {
  width: 100%;
  padding: var(--button-padding-md);
  margin-top: var(--spacing-xs);
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

.login-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 480px) {
  .login-form {
    padding: var(--spacing-lg);
  }
}
</style>
